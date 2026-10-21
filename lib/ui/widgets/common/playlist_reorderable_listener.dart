import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';

/// A deliberate hold starts reordering; ordinary taps and scrolling stay free.
class PlaylistReorderableListener extends ReorderableDragStartListener {
  const PlaylistReorderableListener({
    super.key,
    required super.child,
    required super.index,
    super.enabled,
  });

  @override
  MultiDragGestureRecognizer createRecognizer() =>
      DelayedMultiDragGestureRecognizer(
        delay: const Duration(milliseconds: 800),
        debugOwner: this,
      );
}
