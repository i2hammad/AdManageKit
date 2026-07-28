package com.i2hammad.admanagekit.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [BillingPeriod] — ISO-8601 parsing, normalization and the pricing
 * comparisons that paywalls depend on.
 */
class BillingPeriodTest {

    // ==================== parse ====================

    @Test
    fun `parses the period forms Play emits`() {
        assertEquals(7, BillingPeriod.parse("P7D")!!.days)
        assertEquals(1, BillingPeriod.parse("P1W")!!.weeks)
        assertEquals(1, BillingPeriod.parse("P1M")!!.months)
        assertEquals(3, BillingPeriod.parse("P3M")!!.months)
        assertEquals(6, BillingPeriod.parse("P6M")!!.months)
        assertEquals(1, BillingPeriod.parse("P1Y")!!.years)
    }

    @Test
    fun `parses compound periods`() {
        val period = BillingPeriod.parse("P1Y2M3W4D")!!
        assertEquals(1, period.years)
        assertEquals(2, period.months)
        assertEquals(3, period.weeks)
        assertEquals(4, period.days)
    }

    @Test
    fun `retains the original string`() {
        assertEquals("P1M", BillingPeriod.parse("P1M")!!.iso)
        assertEquals("P1M", BillingPeriod.parse("  P1M  ")!!.iso)
    }

    @Test
    fun `returns null for unusable input`() {
        assertNull(BillingPeriod.parse(null))
        assertNull(BillingPeriod.parse(""))
        assertNull(BillingPeriod.parse("   "))
        assertNull(BillingPeriod.parse("1M"))
        assertNull(BillingPeriod.parse("P"))
        assertNull(BillingPeriod.parse("monthly"))
        assertNull(BillingPeriod.parse("PT1H"))
        // Components out of ISO-8601 order are not valid.
        assertNull(BillingPeriod.parse("P1D1M"))
    }

    @Test
    fun `accepts lowercase designators except months`() {
        assertEquals(7, BillingPeriod.parse("p7d")!!.days)
        assertEquals(1, BillingPeriod.parse("p1y")!!.years)
        // Lowercase m means minutes in ISO-8601, so it must not parse as months.
        assertNull(BillingPeriod.parse("P1m"))
    }

    // ==================== unit / count ====================

    @Test
    fun `reports the largest non-zero component as the dominant unit`() {
        assertEquals(BillingPeriod.Unit.YEAR, BillingPeriod.parse("P1Y")!!.unit)
        assertEquals(BillingPeriod.Unit.MONTH, BillingPeriod.parse("P3M")!!.unit)
        assertEquals(BillingPeriod.Unit.WEEK, BillingPeriod.parse("P2W")!!.unit)
        assertEquals(BillingPeriod.Unit.DAY, BillingPeriod.parse("P7D")!!.unit)
        // Largest component wins over smaller ones.
        assertEquals(BillingPeriod.Unit.MONTH, BillingPeriod.parse("P1M15D")!!.unit)
    }

    @Test
    fun `count is the magnitude of the dominant unit`() {
        assertEquals(3, BillingPeriod.parse("P3M")!!.count)
        assertEquals(1, BillingPeriod.parse("P1M15D")!!.count)
        assertEquals(14, BillingPeriod.parse("P14D")!!.count)
    }

    // ==================== normalization ====================

    @Test
    fun `a year normalizes to exactly twelve months`() {
        assertEquals(12.0, BillingPeriod.parse("P1Y")!!.totalMonths, 1e-9)
        assertEquals(12.0, BillingPeriod.parse("P12M")!!.totalMonths, 1e-9)
    }

    @Test
    fun `month and week normalize to expected day counts`() {
        assertEquals(30, BillingPeriod.parse("P1M")!!.totalDays)
        assertEquals(365, BillingPeriod.parse("P1Y")!!.totalDays)
        assertEquals(7, BillingPeriod.parse("P1W")!!.totalDays)
        assertEquals(7, BillingPeriod.parse("P7D")!!.totalDays)
    }

    @Test
    fun `isZero is true only when every component is zero`() {
        assertTrue(BillingPeriod.parse("P0D")!!.isZero)
        assertFalse(BillingPeriod.parse("P1D")!!.isZero)
    }

    @Test
    fun `isSameDuration equates equivalent spellings`() {
        assertTrue(BillingPeriod.isSameDuration("P1Y", "P12M"))
        assertTrue(BillingPeriod.isSameDuration("P1W", "P7D"))
        assertFalse(BillingPeriod.isSameDuration("P1M", "P1Y"))
        assertFalse(BillingPeriod.isSameDuration("P1M", null))
        assertFalse(BillingPeriod.isSameDuration(null, null))
    }

    // ==================== formatting ====================

    @Test
    fun `formats singular and plural English labels`() {
        assertEquals("1 month", BillingPeriod.parse("P1M")!!.format())
        assertEquals("3 months", BillingPeriod.parse("P3M")!!.format())
        assertEquals("1 year", BillingPeriod.parse("P1Y")!!.format())
        assertEquals("7 days", BillingPeriod.parse("P7D")!!.format())
        assertEquals("2 weeks", BillingPeriod.parse("P2W")!!.format())
    }

    @Test
    fun `formats abbreviated labels`() {
        assertEquals("1mo", BillingPeriod.parse("P1M")!!.format(abbreviated = true))
        assertEquals("1y", BillingPeriod.parse("P1Y")!!.format(abbreviated = true))
        assertEquals("7d", BillingPeriod.parse("P7D")!!.format(abbreviated = true))
        assertEquals("2w", BillingPeriod.parse("P2W")!!.format(abbreviated = true))
    }

    @Test
    fun `formatOf degrades to empty string on bad input`() {
        assertEquals("", BillingPeriod.formatOf(null))
        assertEquals("", BillingPeriod.formatOf("nonsense"))
        assertEquals("1 month", BillingPeriod.formatOf("P1M"))
    }

    // ==================== price normalization ====================

    @Test
    fun `pricePerMonthMicros divides a yearly price by twelve`() {
        // $59.99/year -> ~$5.00/month
        assertEquals(4_999_167L, BillingPeriod.pricePerMonthMicros(59_990_000L, "P1Y"))
    }

    @Test
    fun `pricePerMonthMicros is identity for a monthly price`() {
        assertEquals(9_990_000L, BillingPeriod.pricePerMonthMicros(9_990_000L, "P1M"))
    }

    @Test
    fun `pricePerMonthMicros returns zero for unusable input`() {
        assertEquals(0L, BillingPeriod.pricePerMonthMicros(9_990_000L, null))
        assertEquals(0L, BillingPeriod.pricePerMonthMicros(9_990_000L, "bogus"))
        assertEquals(0L, BillingPeriod.pricePerMonthMicros(0L, "P1M"))
        assertEquals(0L, BillingPeriod.pricePerMonthMicros(-1L, "P1M"))
        assertEquals(0L, BillingPeriod.pricePerMonthMicros(9_990_000L, "P0D"))
    }

    @Test
    fun `pricePerWeekMicros normalizes a monthly price`() {
        // $10/month over 30.436875/7 = ~4.348 weeks
        assertEquals(2_299_842L, BillingPeriod.pricePerWeekMicros(10_000_000L, "P1M"))
    }

    // ==================== savings ====================

    @Test
    fun `savingsPercent computes the annual-versus-monthly badge`() {
        // $9.99/month vs $59.99/year -> ~50% saving
        val saving = BillingPeriod.savingsPercent(
            baseMicros = 9_990_000L,
            basePeriod = "P1M",
            comparedMicros = 59_990_000L,
            comparedPeriod = "P1Y",
        )
        assertEquals(50, saving)
    }

    @Test
    fun `savingsPercent is zero when the compared plan is not cheaper`() {
        // Same per-month cost.
        assertEquals(
            0,
            BillingPeriod.savingsPercent(10_000_000L, "P1M", 120_000_000L, "P1Y"),
        )
        // Compared plan is more expensive per month.
        assertEquals(
            0,
            BillingPeriod.savingsPercent(10_000_000L, "P1M", 240_000_000L, "P1Y"),
        )
    }

    @Test
    fun `savingsPercent is zero for unusable input`() {
        assertEquals(0, BillingPeriod.savingsPercent(0L, "P1M", 59_990_000L, "P1Y"))
        assertEquals(0, BillingPeriod.savingsPercent(9_990_000L, null, 59_990_000L, "P1Y"))
        assertEquals(0, BillingPeriod.savingsPercent(9_990_000L, "P1M", 59_990_000L, null))
        assertEquals(0, BillingPeriod.savingsPercent(9_990_000L, "P1M", 0L, "P1Y"))
        assertEquals(0, BillingPeriod.savingsPercent(9_990_000L, "P0D", 59_990_000L, "P1Y"))
    }

    @Test
    fun `savingsPercent never leaves the zero to one hundred range`() {
        // A near-free yearly plan still caps at 100.
        val saving = BillingPeriod.savingsPercent(100_000_000L, "P1M", 1L, "P1Y")
        assertTrue(saving in 0..100)
    }

    @Test
    fun `totalMonthsOf and totalDaysOf degrade to zero`() {
        assertEquals(12.0, BillingPeriod.totalMonthsOf("P1Y"), 1e-9)
        assertEquals(0.0, BillingPeriod.totalMonthsOf("nope"), 1e-9)
        assertEquals(30, BillingPeriod.totalDaysOf("P1M"))
        assertEquals(0, BillingPeriod.totalDaysOf(null))
    }

    @Test
    fun `toString returns the original iso string`() {
        assertNotNull(BillingPeriod.parse("P1M"))
        assertEquals("P1M", BillingPeriod.parse("P1M").toString())
    }
}
