package dev.klang.portal.audio

/** Development flags only; production requires a verified Google Play Billing entitlement. */
object PremiumFeatures {
    const val MILK_VISUALIZER = false
    const val CUSTOM_MILK_IMPORT = false
    const val SPATIAL_SOUNDSTAGES = false
    const val PREMIUM_SCENES = false

    fun isAvailable(feature: Feature, hasVerifiedEntitlement: Boolean): Boolean {
        if (!hasVerifiedEntitlement) return false
        return when (feature) {
            Feature.MILK_VISUALIZER -> MILK_VISUALIZER
            Feature.CUSTOM_MILK_IMPORT -> CUSTOM_MILK_IMPORT
            Feature.SPATIAL_SOUNDSTAGES -> SPATIAL_SOUNDSTAGES
            Feature.PREMIUM_SCENES -> PREMIUM_SCENES
        }
    }

    enum class Feature { MILK_VISUALIZER, CUSTOM_MILK_IMPORT, SPATIAL_SOUNDSTAGES, PREMIUM_SCENES }
}
