package com.i2hammad.admanagekit.admob

import com.i2hammad.admanagekit.R

/**
 * Enum representing all available native ad templates.
 * Each template has a corresponding layout resource for the ad view and shimmer preview.
 *
 * @property layoutResId The layout resource ID for the actual ad template
 * @property shimmerResId The layout resource ID for the shimmer/loading placeholder
 * @property displayName Human-readable name for the template
 */
enum class NativeAdTemplate(
    val layoutResId: Int,
    val shimmerResId: Int,
    val displayName: String
) {
    // Modern & Material Design Templates
    CARD_MODERN(
        layoutResId = R.layout.layout_native_card_modern,
        shimmerResId = R.layout.layout_shimmer_card_modern,
        displayName = "Card Modern"
    ),

    MATERIAL3(
        layoutResId = R.layout.layout_native_material3,
        shimmerResId = R.layout.layout_shimmer_material3,
        displayName = "Material 3"
    ),

    MINIMAL(
        layoutResId = R.layout.layout_native_minimal,
        shimmerResId = R.layout.layout_shimmer_minimal,
        displayName = "Minimal"
    ),

    COMPACT_HORIZONTAL(
        layoutResId = R.layout.layout_native_compact_horizontal,
        shimmerResId = R.layout.layout_shimmer_compact_horizontal,
        displayName = "Compact Horizontal"
    ),

    STORY_STYLE(
        layoutResId = R.layout.layout_native_story_style,
        shimmerResId = R.layout.layout_shimmer_story_style,
        displayName = "Story Style"
    ),

    FULL_WIDTH_BANNER(
        layoutResId = R.layout.layout_native_full_width_banner,
        shimmerResId = R.layout.layout_shimmer_full_width_banner,
        displayName = "Full Width Banner"
    ),

    GRID_CARD(
        layoutResId = R.layout.layout_native_grid_card,
        shimmerResId = R.layout.layout_shimmer_grid_card,
        displayName = "Grid Card"
    ),

    LIST_ITEM(
        layoutResId = R.layout.layout_native_list_item,
        shimmerResId = R.layout.layout_shimmer_list_item,
        displayName = "List Item"
    ),

    FEATURED(
        layoutResId = R.layout.layout_native_featured,
        shimmerResId = R.layout.layout_shimmer_featured,
        displayName = "Featured"
    ),

    OVERLAY_DARK(
        layoutResId = R.layout.layout_native_overlay_dark,
        shimmerResId = R.layout.layout_shimmer_overlay_dark,
        displayName = "Overlay Dark"
    ),

    MAGAZINE(
        layoutResId = R.layout.layout_native_magazine,
        shimmerResId = R.layout.layout_shimmer_magazine,
        displayName = "Magazine"
    ),

    // Video Templates
    VIDEO_SMALL(
        layoutResId = R.layout.layout_native_video_small,
        shimmerResId = R.layout.layout_shimmer_video_small,
        displayName = "Video Small"
    ),

    VIDEO_MEDIUM(
        layoutResId = R.layout.layout_native_video_medium,
        shimmerResId = R.layout.layout_shimmer_video_medium,
        displayName = "Video Medium"
    ),

    VIDEO_LARGE(
        layoutResId = R.layout.layout_native_video_large,
        shimmerResId = R.layout.layout_shimmer_video_large,
        displayName = "Video Large"
    ),

    VIDEO_SQUARE(
        layoutResId = R.layout.layout_native_video_square,
        shimmerResId = R.layout.layout_shimmer_video_square,
        displayName = "Video Square"
    ),

    VIDEO_VERTICAL(
        layoutResId = R.layout.layout_native_video_vertical,
        shimmerResId = R.layout.layout_shimmer_video_vertical,
        displayName = "Video Vertical"
    ),

    VIDEO_FULLSCREEN(
        layoutResId = R.layout.layout_native_video_fullscreen,
        shimmerResId = R.layout.layout_shimmer_video_fullscreen,
        displayName = "Video Fullscreen"
    );

    companion object {
        /**
         * Get template by name string (case-insensitive)
         * @param name Template name (e.g., "CARD_MODERN", "card_modern", "Card Modern")
         * @return The matching template or CARD_MODERN as default
         */
        fun fromString(name: String): NativeAdTemplate {
            val normalizedName = name.uppercase().replace(" ", "_").replace("-", "_")
            return entries.find { it.name == normalizedName || it.displayName.uppercase().replace(" ", "_") == normalizedName }
                ?: CARD_MODERN
        }

        /**
         * Get template by ordinal index
         * @param index The template index (0-10)
         * @return The matching template or CARD_MODERN if index is out of bounds
         */
        fun fromIndex(index: Int): NativeAdTemplate {
            return entries.getOrNull(index) ?: CARD_MODERN
        }

        /**
         * Get all video templates
         */
        fun videoTemplates(): List<NativeAdTemplate> {
            return entries.filter { it.name.startsWith("VIDEO_") }
        }

        /**
         * Get all non-video templates
         */
        fun standardTemplates(): List<NativeAdTemplate> {
            return entries.filter { !it.name.startsWith("VIDEO_") }
        }
    }
}
