part of 'premium_music_bar.dart';

extension _MiniPlayerContent on _PremiumMusicBarState {
  Widget _buildMiniPlayerContent(
    Song song,
    bool isPlaying,
    bool useBlur,
    Color? cardBgColor,
    double cardBlurAmount,
    dynamic settings, {
    bool compact = false,
  }) {
    Widget buildHero({required String tag, required Widget child}) {
      if (settings.enableSlideGesture) return child;
      return Hero(tag: tag, child: child);
    }

    final double artSize = compact ? 40.0 : 48.0;
    final double titleFontSize = compact ? 15.0 : 17.0;
    final double artistFontSize = compact ? 11.0 : 12.5;
    final bool glassEnabled = useBlur && !settings.disableBlur;
    final double tintAlpha =
        (0.055 + cardBlurAmount / 100).clamp(0.055, 0.12);

    final Widget artwork = buildHero(
      tag: 'album_art',
      child: ClipRRect(
        borderRadius: BorderRadius.circular(15),
        child: SizedBox(
          width: artSize,
          height: artSize,
          child: Stack(
            fit: StackFit.expand,
            children: [
              OptimizedImage(
                imagePath: song.artPath != null &&
                        !song.artPath!.startsWith('http')
                    ? song.artPath
                    : null,
                imageUrl: song.artPath != null &&
                        song.artPath!.startsWith('http')
                    ? song.artPath
                    : null,
                fit: BoxFit.cover,
              ),
              Positioned.fill(
                child: SongPlaybackOverlay(isPlaying: isPlaying),
              ),
            ],
          ),
        ),
      ),
    );

    final Widget trackInfo = Expanded(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          buildHero(
            tag: 'song_title',
            child: ScrollingText(
              text: song.title,
              style: AppFonts.jostStyle(
                color: Colors.white,
                fontWeight: FontWeight.w700,
                fontSize: titleFontSize.ts,
                letterSpacing: 0.1,
              ),
            ),
          ),
          const SizedBox(height: 2),
          buildHero(
            tag: 'song_artist',
            child: ScrollingText(
              text: artistAlbumLabel(
                song,
                context.l10n,
                showAlbum: settings.showAlbumNextToArtist as bool,
              ),
              style: AppFonts.jostStyle(
                color: Colors.white.withValues(alpha: 0.62),
                fontSize: artistFontSize.ts,
                letterSpacing: 0.15,
              ),
            ),
          ),
        ],
      ),
    );

    final Widget playButton = buildHero(
      tag: 'player_play_pause_btn',
      child: Container(
        width: compact ? 40 : 44,
        height: compact ? 40 : 44,
        decoration: BoxDecoration(
          color: const Color(0x338EDCFF),
          shape: BoxShape.circle,
          border: Border.all(
            color: const Color(0x668EDCFF),
            width: 1,
          ),
        ),
        alignment: Alignment.center,
        child: AnimatedPlayPauseIcon(
          isPlaying: isPlaying,
          color: const Color(0xFFF4F8FF),
          size: (compact
                  ? AppIcons.expandedPlayerPlayPauseIcon * 0.78
                  : AppIcons.expandedPlayerPlayPauseIcon * 0.86)
              .s,
        ),
      ),
    );

    final Widget playerContent = Row(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        artwork,
        const SizedBox(width: 12),
        trackInfo,
        const SizedBox(width: 12),
        Padding(
          padding: const EdgeInsets.only(right: 1),
          child: playButton,
        ),
      ],
    );

    final Widget surface;
    if (settings.enableSlideGesture) {
      // Keep the mini-player transparent while it morphs into the full player.
      surface = playerContent;
    } else if (glassEnabled) {
      surface = GlassContainer(
        useOwnLayer: true,
        quality: GlassQuality.standard,
        shape: const LiquidRoundedSuperellipse(borderRadius: 34),
        color: cardBgColor ??
            Colors.white.withValues(alpha: tintAlpha),
        padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 7),
        child: playerContent,
      );
    } else {
      surface = Container(
        decoration: BoxDecoration(
          color: cardBgColor ??
              Theme.of(context).colorScheme.surfaceContainer,
          borderRadius: BorderRadius.circular(34),
          border: Border.all(
            color: Colors.white.withValues(alpha: 0.12),
          ),
        ),
        padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 7),
        child: playerContent,
      );
    }

    return Padding(
      padding: EdgeInsets.symmetric(
        vertical: settings.enableSlideGesture ? 1.2 : 0.0,
      ),
      child: surface,
    );
  }
}
