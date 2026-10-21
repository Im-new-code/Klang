import 'package:flutter/material.dart';
import 'package:looper_player/ui/widgets/common/song_playback_overlay.dart';

class PlayingOverlay extends StatelessWidget {
  const PlayingOverlay({
    super.key,
    required this.child,
    required this.isCurrent,
    required this.isPlaying,
    this.borderRadius = 8,
  });

  final Widget child;
  final bool isCurrent;
  final bool isPlaying;
  final double borderRadius;

  @override
  Widget build(BuildContext context) => Stack(
    children: [
      child,
      if (isCurrent)
        Positioned.fill(
          child: ClipRRect(
            borderRadius: BorderRadius.circular(borderRadius),
            child: SongPlaybackOverlay(isPlaying: isPlaying),
          ),
        ),
    ],
  );
}
