import 'dart:ui' as ui;

import 'package:flutter/foundation.dart';
import 'package:flutter/painting.dart';

/// Decodes [image] just big enough to *cover* a [width] x [height] pixel box:
/// both sides at least the box, aspect kept, never upscaled.
///
/// Flutter's own cacheWidth / [ResizeImage] limit one side (or fit inside
/// the box), so a wide picture in a square tile came out shorter than the
/// tile and was then stretched by BoxFit.cover - visibly soft.
class CoverResizeImage extends ImageProvider<CoverResizeImageKey> {
  const CoverResizeImage(
    this.image, {
    required this.width,
    required this.height,
  });

  final ImageProvider<Object> image;
  final int width;
  final int height;

  @override
  Future<CoverResizeImageKey> obtainKey(
    ImageConfiguration configuration,
  ) async {
    final inner = await image.obtainKey(configuration);
    return CoverResizeImageKey(inner, width, height);
  }

  @override
  ImageStreamCompleter loadImage(
    CoverResizeImageKey key,
    ImageDecoderCallback decode,
  ) {
    Future<ui.Codec> decodeCover(
      ui.ImmutableBuffer buffer, {
      ui.TargetImageSizeCallback? getTargetSize,
    }) {
      return decode(
        buffer,
        getTargetSize: (intrinsicWidth, intrinsicHeight) {
          final scale = coverScale(
            intrinsicWidth,
            intrinsicHeight,
            key.width,
            key.height,
          );
          if (scale >= 1) return ui.TargetImageSize();
          return ui.TargetImageSize(
            width: (intrinsicWidth * scale).ceil(),
            height: (intrinsicHeight * scale).ceil(),
          );
        },
      );
    }

    return image.loadImage(key.inner, decodeCover);
  }

  /// How much to shrink an [imageWidth] x [imageHeight] picture so it still
  /// covers [boxWidth] x [boxHeight]; 1 or more means keep full size.
  @visibleForTesting
  static double coverScale(
    int imageWidth,
    int imageHeight,
    int boxWidth,
    int boxHeight,
  ) {
    if (imageWidth <= 0 || imageHeight <= 0) return 1;
    final scaleX = boxWidth / imageWidth;
    final scaleY = boxHeight / imageHeight;
    return scaleX > scaleY ? scaleX : scaleY;
  }
}

@immutable
class CoverResizeImageKey {
  const CoverResizeImageKey(this.inner, this.width, this.height);

  final Object inner;
  final int width;
  final int height;

  @override
  bool operator ==(Object other) =>
      other is CoverResizeImageKey &&
      other.inner == inner &&
      other.width == width &&
      other.height == height;

  @override
  int get hashCode => Object.hash(inner, width, height);
}
