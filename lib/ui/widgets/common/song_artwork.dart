import 'package:looper_player/features/library/domain/models/models.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:looper_player/features/playback/presentation/providers/playback/playback_notifier.dart';
import 'package:looper_player/ui/widgets/common/optimized_image.dart';
import 'package:looper_player/ui/widgets/common/song_playback_overlay.dart';

/// Artwork that keeps identifying the loaded song when playback is paused.
class SongArtwork extends ConsumerWidget {
  const SongArtwork({
    super.key,
    required this.song,
    required this.size,
    this.borderRadius = BorderRadius.zero,
    this.placeholder,
    this.cacheWidth,
    this.cacheHeight,
    this.indicatorSize = 24,
  });

  final Song song;
  final double size;
  final BorderRadius borderRadius;
  final Widget? placeholder;
  final int? cacheWidth;
  final int? cacheHeight;
  final double indicatorSize;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final isCurrent = ref.watch(
      playbackProvider.select((s) => s.currentSong?.path == song.path),
    );
    final isPlaying =
        isCurrent && ref.watch(playbackProvider.select((s) => s.isPlaying));
    return ClipRRect(
      borderRadius: borderRadius,
      child: SizedBox.square(
        dimension: size,
        child: Stack(
          fit: StackFit.expand,
          children: [
            OptimizedImage(
              imagePath: song.artPath,
              width: size,
              height: size,
              placeholder: placeholder,
              cacheWidth: cacheWidth,
              cacheHeight: cacheHeight,
            ),
            if (isCurrent)
              SongPlaybackOverlay(
                isPlaying: isPlaying,
                indicatorSize: indicatorSize,
              ),
          ],
        ),
      ),
    );
  }
}
