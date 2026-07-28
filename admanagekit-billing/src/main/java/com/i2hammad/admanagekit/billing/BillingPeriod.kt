package com.i2hammad.admanagekit.billing

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Parsed view over an ISO-8601 duration string as used by Google Play billing
 * periods (`"P7D"`, `"P1W"`, `"P1M"`, `"P3M"`, `"P6M"`, `"P1Y"`).
 *
 * Play returns raw ISO-8601 strings from
 * [com.android.billingclient.api.ProductDetails.PricingPhase.getBillingPeriod],
 * which are not directly presentable to users and are awkward to compare. This
 * class turns them into:
 *  - discrete components ([years], [months], [weeks], [days]);
 *  - a single dominant [unit] + [count] pair, which is what you want for
 *    localized plurals (`getQuantityString(R.plurals.days, count, count)`);
 *  - normalized [totalMonths] / [totalDays], for price-per-month comparisons.
 *
 * Obtain instances via [parse]. Comparisons and normalization use the average
 * Gregorian month (30.436875 days) and year (365.2425 days), so `P1Y` normalizes
 * to exactly 12.0 months and `P12M` does too.
 *
 * ```kotlin
 * val period = BillingPeriod.parse("P1Y")           // years=1
 * period.unit                                        // Unit.YEAR
 * period.count                                       // 1
 * period.totalMonths                                 // 12.0
 * period.format()                                    // "1 year"
 * ```
 *
 * @since 4.4.0
 */
data class BillingPeriod(
    /** Years component of the duration (the `nY` term), or 0. */
    val years: Int,
    /** Months component of the duration (the `nM` date term), or 0. */
    val months: Int,
    /** Weeks component of the duration (the `nW` term), or 0. */
    val weeks: Int,
    /** Days component of the duration (the `nD` term), or 0. */
    val days: Int,
    /** The original ISO-8601 string this was parsed from. */
    val iso: String,
) {

    /**
     * A single calendar unit, used together with [count] to render a localized
     * period label without re-deriving it from the raw components.
     */
    enum class Unit { DAY, WEEK, MONTH, YEAR }

    /**
     * The dominant unit of this period — the largest non-zero component.
     * A period of `"P1M15D"` reports [Unit.MONTH]; `"P0D"` reports [Unit.DAY].
     */
    val unit: Unit
        get() = when {
            years > 0 -> Unit.YEAR
            months > 0 -> Unit.MONTH
            weeks > 0 -> Unit.WEEK
            else -> Unit.DAY
        }

    /**
     * The magnitude of the dominant [unit]. Pair these two for localized output:
     * `resources.getQuantityString(pluralFor(unit), count, count)`.
     *
     * Note this drops smaller components — `"P1M15D"` reports `count == 1` with
     * [Unit.MONTH]. Use [format] or the raw components when that matters.
     */
    val count: Int
        get() = when (unit) {
            Unit.YEAR -> years
            Unit.MONTH -> months
            Unit.WEEK -> weeks
            Unit.DAY -> days
        }

    /**
     * The whole duration expressed in months, using the average Gregorian month.
     * `P1Y` → `12.0`, `P1M` → `1.0`, `P1W` → `≈0.23`, `P7D` → `≈0.23`.
     *
     * This is the denominator for per-month price normalization; see
     * [AppPurchase.getPricePerMonthMicros].
     */
    val totalMonths: Double
        get() = years * MONTHS_PER_YEAR + months + (weeks * DAYS_PER_WEEK + days) / DAYS_PER_MONTH

    /**
     * The whole duration expressed in weeks, using the average Gregorian month.
     */
    val totalWeeks: Double
        get() = totalDaysExact / DAYS_PER_WEEK

    /**
     * The whole duration expressed in days, rounded to the nearest whole day.
     * `P1Y` → `365`, `P1M` → `30`, `P1W` → `7`.
     */
    val totalDays: Int
        get() = totalDaysExact.roundToInt()

    /** Unrounded day count — kept internal so callers get a stable rounded value. */
    private val totalDaysExact: Double
        get() = years * DAYS_PER_YEAR + months * DAYS_PER_MONTH + weeks * DAYS_PER_WEEK + days

    /** `true` when every component is zero (e.g. `"P0D"`). */
    val isZero: Boolean
        get() = years == 0 && months == 0 && weeks == 0 && days == 0

    /**
     * Renders an English label such as `"7 days"`, `"1 month"`, `"3 months"`.
     *
     * This is a convenience for prototypes and English-only apps. For localized
     * output, use [unit] and [count] with your own plurals resources — a library
     * cannot resolve the host app's locale rules correctly.
     *
     * @param abbreviated when `true`, renders short forms (`"7d"`, `"1mo"`, `"1y"`).
     */
    @JvmOverloads
    fun format(abbreviated: Boolean = false): String {
        val n = count
        if (abbreviated) {
            val suffix = when (unit) {
                Unit.DAY -> "d"
                Unit.WEEK -> "w"
                Unit.MONTH -> "mo"
                Unit.YEAR -> "y"
            }
            return "$n$suffix"
        }
        val name = when (unit) {
            Unit.DAY -> "day"
            Unit.WEEK -> "week"
            Unit.MONTH -> "month"
            Unit.YEAR -> "year"
        }
        return if (n == 1) "$n $name" else "$n ${name}s"
    }

    override fun toString(): String = iso

    companion object {
        private const val DAYS_PER_WEEK = 7.0
        private const val DAYS_PER_MONTH = 30.436875
        private const val DAYS_PER_YEAR = 365.2425
        private const val MONTHS_PER_YEAR = 12

        /**
         * Parses an ISO-8601 duration string into a [BillingPeriod].
         *
         * Accepts the date-portion forms Play emits (`PnYnMnWnD`, any subset, in
         * that order). The `M` (months) designator must be uppercase — lowercase
         * `m` means minutes in ISO-8601 — but `P`, `Y`, `W` and `D` are accepted
         * in either case. Time components (`PT1H`) are not used by Play billing
         * periods and are rejected.
         *
         * @param iso the raw period string, e.g. `"P1M"`. May be `null`.
         * @return the parsed period, or `null` if [iso] is null, blank, or not a
         *         well-formed date-only ISO-8601 duration.
         */
        @JvmStatic
        fun parse(iso: String?): BillingPeriod? {
            if (iso.isNullOrBlank()) return null
            val text = iso.trim()
            val match = PATTERN.matchEntire(text) ?: return null
            val (y, mo, w, d) = match.destructured
            // "P" alone matches the pattern but carries no duration.
            if (y.isEmpty() && mo.isEmpty() && w.isEmpty() && d.isEmpty()) return null
            return BillingPeriod(
                years = y.toIntOrNull() ?: 0,
                months = mo.toIntOrNull() ?: 0,
                weeks = w.toIntOrNull() ?: 0,
                days = d.toIntOrNull() ?: 0,
                iso = text,
            )
        }

        /**
         * Total months for a raw ISO-8601 period, or `0.0` when unparseable.
         * Convenience for Java callers that only need the normalization factor.
         */
        @JvmStatic
        fun totalMonthsOf(iso: String?): Double = parse(iso)?.totalMonths ?: 0.0

        /**
         * Total days for a raw ISO-8601 period, or `0` when unparseable.
         */
        @JvmStatic
        fun totalDaysOf(iso: String?): Int = parse(iso)?.totalDays ?: 0

        /**
         * Formats a raw ISO-8601 period as an English label, returning an empty
         * string when unparseable. See [format] for localization caveats.
         */
        @JvmStatic
        @JvmOverloads
        fun formatOf(iso: String?, abbreviated: Boolean = false): String =
            parse(iso)?.format(abbreviated) ?: ""

        /**
         * Percentage saved by paying [comparedMicros] per [comparedPeriod] instead
         * of [baseMicros] per [basePeriod], normalized to a common cadence.
         *
         * Typical use is "yearly vs monthly": pass the monthly plan as the base.
         * Returns `0` when either side is unusable (missing price, unparseable
         * period, zero-length period) or when the compared plan is not cheaper.
         *
         * @return whole-percent saving in `0..100`.
         */
        @JvmStatic
        fun savingsPercent(
            baseMicros: Long,
            basePeriod: String?,
            comparedMicros: Long,
            comparedPeriod: String?,
        ): Int {
            if (baseMicros <= 0L || comparedMicros <= 0L) return 0
            val baseMonths = parse(basePeriod)?.totalMonths ?: return 0
            val comparedMonths = parse(comparedPeriod)?.totalMonths ?: return 0
            if (baseMonths <= 0.0 || comparedMonths <= 0.0) return 0
            val basePerMonth = baseMicros / baseMonths
            val comparedPerMonth = comparedMicros / comparedMonths
            if (basePerMonth <= 0.0 || comparedPerMonth >= basePerMonth) return 0
            val saving = (basePerMonth - comparedPerMonth) / basePerMonth * 100.0
            return saving.roundToInt().coerceIn(0, 100)
        }

        /**
         * Price in micros normalized to one average month.
         *
         * @return per-month micros, or `0` when the period is unparseable or zero.
         */
        @JvmStatic
        fun pricePerMonthMicros(priceMicros: Long, period: String?): Long {
            val months = parse(period)?.totalMonths ?: return 0L
            if (months <= 0.0 || priceMicros <= 0L) return 0L
            return (priceMicros / months).roundToLong()
        }

        /**
         * Price in micros normalized to one week.
         *
         * @return per-week micros, or `0` when the period is unparseable or zero.
         */
        @JvmStatic
        fun pricePerWeekMicros(priceMicros: Long, period: String?): Long {
            val weeks = parse(period)?.totalWeeks ?: return 0L
            if (weeks <= 0.0 || priceMicros <= 0L) return 0L
            return (priceMicros / weeks).roundToLong()
        }

        /**
         * `true` when two ISO-8601 periods describe the same duration even if
         * written differently (`"P1Y"` vs `"P12M"`, `"P1W"` vs `"P7D"`).
         */
        @JvmStatic
        fun isSameDuration(first: String?, second: String?): Boolean {
            val a = parse(first) ?: return false
            val b = parse(second) ?: return false
            return abs(a.totalMonths - b.totalMonths) < EPSILON
        }

        private const val EPSILON = 1e-9

        // Date-only ISO-8601 duration. Groups: years, months, weeks, days.
        private val PATTERN =
            Regex("""^[Pp](?:(\d+)[Yy])?(?:(\d+)M)?(?:(\d+)[Ww])?(?:(\d+)[Dd])?$""")
    }
}
