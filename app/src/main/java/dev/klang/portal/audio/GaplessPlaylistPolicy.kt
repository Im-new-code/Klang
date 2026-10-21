package dev.klang.portal.audio

/** Keeps transitions on one player; tests are required before claiming universal gapless output. */
object GaplessPlaylistPolicy {
    const val RECREATE_PLAYER_BETWEEN_TRACKS = false
    const val CLAIM_BIT_PERFECT_OUTPUT = false
}
