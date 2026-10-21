import 'dart:async';

import 'package:flutter/material.dart';
import 'package:looper_player/ui/widgets/common/momentum_scroll_controller.dart';
import 'package:looper_player/core/utils/l10n.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

/// Which way the floating jump button points.
enum _Jump { end, top }

/// Scrolling down offers a jump to the end, scrolling up a jump to the top.
class CollectionScrollToEnd extends StatefulWidget {
  const CollectionScrollToEnd({
    super.key,
    required this.builder,
    this.enabled = true,
    this.bottomPadding = 16,
  });

  final Widget Function(ScrollController controller) builder;
  final bool enabled;
  final double bottomPadding;

  @override
  State<CollectionScrollToEnd> createState() => _CollectionScrollToEndState();
}

class _CollectionScrollToEndState extends State<CollectionScrollToEnd> {
  final _controller = MomentumScrollController();
  final _visible = ValueNotifier<_Jump?>(null);
  Timer? _hideTimer;
  // The button's own jump scrolls too; don't let it bring itself back.
  bool _jumping = false;

  @override
  void dispose() {
    _hideTimer?.cancel();
    _visible.dispose();
    _controller.dispose();
    super.dispose();
  }

  bool _onScroll(ScrollNotification notification) {
    if (!widget.enabled || _jumping || notification.depth != 0) return false;
    if (notification is ScrollStartNotification) {
      _hideTimer?.cancel();
    }
    if (notification is ScrollUpdateNotification) {
      final metrics = notification.metrics;
      final delta = notification.scrollDelta ?? 0;
      if (delta > 0) {
        _visible.value =
            metrics.pixels > 0 &&
                metrics.maxScrollExtent - metrics.pixels >
                    metrics.viewportDimension
            ? _Jump.end
            : null;
      } else if (delta < 0) {
        _visible.value = metrics.pixels > metrics.viewportDimension
            ? _Jump.top
            : null;
      }
      _hideTimer?.cancel();
    } else if (notification is ScrollEndNotification &&
        _visible.value != null) {
      _hideTimer = Timer(
        const Duration(milliseconds: 1400),
        () => _visible.value = null,
      );
    }
    return false;
  }

  Future<void> _jump(_Jump target) async {
    _hideTimer?.cancel();
    _visible.value = null;
    final position = _controller.position;
    _jumping = true;
    final to = target == _Jump.end ? position.maxScrollExtent : 0.0;
    // Animating the whole way would build and decode artwork for every row
    // in between. Jump to within a screen and a half, then glide the rest.
    final runUp = position.viewportDimension * 1.5;
    if ((to - position.pixels).abs() > runUp) {
      _controller.jumpTo(target == _Jump.end ? to - runUp : to + runUp);
    }
    _jumping = true;
    try {
      await _controller.animateTo(
        to,
        duration: const Duration(milliseconds: 450),
        curve: Curves.easeOutCubic,
      );
    } finally {
      _jumping = false;
    }
  }

  @override
  Widget build(BuildContext context) =>
      NotificationListener<ScrollNotification>(
        onNotification: _onScroll,
        child: Stack(
          children: [
            Positioned.fill(child: widget.builder(_controller)),
            PositionedDirectional(
              end: 16,
              bottom: widget.bottomPadding,
              child: ValueListenableBuilder<_Jump?>(
                valueListenable: _visible,
                builder: (context, jump, _) => jump != null && widget.enabled
                    ? IconButton.filled(
                        tooltip: jump == _Jump.end
                            ? context.l10n.scrollToEnd
                            : context.l10n.scrollToTop,
                        style: IconButton.styleFrom(
                          backgroundColor: const Color(0xFF181818),
                          foregroundColor: Colors.white.withValues(alpha: 0.5),
                          side: BorderSide(
                            color: Colors.white.withValues(alpha: 0.08),
                          ),
                        ),
                        onPressed: () => _jump(jump),
                        icon: Icon(
                          jump == _Jump.end
                              ? LucideIcons.chevronsDown
                              : LucideIcons.chevronsUp,
                          size: 18,
                        ),
                      )
                    : const SizedBox.shrink(),
              ),
            ),
          ],
        ),
      );
}
