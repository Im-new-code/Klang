import 'package:flutter/material.dart';
import 'package:looper_player/core/theme/app_assets.dart';
import 'package:looper_player/core/utils/motion.dart';

class SongPlaybackOverlay extends StatelessWidget {
  const SongPlaybackOverlay({
    super.key,
    required this.isPlaying,
    this.indicatorSize = 24,
  });

  final bool isPlaying;
  final double indicatorSize;

  @override
  Widget build(BuildContext context) => ColoredBox(
    color: Colors.black.withValues(alpha: 0.4),
    child: Center(
      child: SongPlaybackIndicator(isPlaying: isPlaying, size: indicatorSize),
    ),
  );
}

class SongPlaybackIndicator extends StatelessWidget {
  const SongPlaybackIndicator({
    super.key,
    required this.isPlaying,
    this.size = 24,
  });

  final bool isPlaying;
  final double size;

  @override
  Widget build(BuildContext context) =>
      // GIF decoding keeps running offstage and while paused, so only mount it
      // when visible and playing.
      isPlaying && TickerMode.of(context) && !Motion.reducedOf(context)
      ? Image.asset(
          AppAssets.playingGif,
          width: size,
          height: size,
          color: Colors.white,
        )
      : FittedBox(
          fit: BoxFit.scaleDown,
          child: SizedBox(
            width: size,
            height: size,
            // Match the GIF's four columns and baseline, flattened while paused.
            child: Padding(
              padding: EdgeInsets.only(top: size * 5.2 / 24),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                spacing: size * 0.8 / 24,
                children: [
                  for (var i = 0; i < 4; i++)
                    Container(
                      width: size * 2.2 / 24,
                      height: size * 2.2 / 24,
                      decoration: BoxDecoration(
                        color: Colors.white.withValues(alpha: 0.5),
                        borderRadius: BorderRadius.circular(size * 1.1 / 24),
                      ),
                    ),
                ],
              ),
            ),
          ),
        );
}
