import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

/// Consecutive touch flings add speed without discarding a slower swipe.
class MomentumScrollController extends ScrollController {
  MomentumScrollController({
    super.initialScrollOffset,
    super.keepScrollOffset,
    super.debugLabel,
  });

  @override
  ScrollPosition createScrollPosition(
    ScrollPhysics physics,
    ScrollContext context,
    ScrollPosition? oldPosition,
  ) {
    if (defaultTargetPlatform != TargetPlatform.android) {
      return super.createScrollPosition(physics, context, oldPosition);
    }
    return _MomentumScrollPosition(
      physics: _NoCarriedMomentumPhysics(parent: physics),
      context: context,
      initialPixels: initialScrollOffset,
      keepScrollOffset: keepScrollOffset,
      oldPosition: oldPosition,
      debugLabel: debugLabel,
    );
  }
}

// Flutter's drag controller otherwise adds momentum only if the next fling
// exceeds half the carried velocity. Handle that addition in the position.
class _NoCarriedMomentumPhysics extends ScrollPhysics {
  const _NoCarriedMomentumPhysics({super.parent});

  @override
  _NoCarriedMomentumPhysics applyTo(ScrollPhysics? ancestor) =>
      _NoCarriedMomentumPhysics(parent: buildParent(ancestor));

  @override
  double carriedMomentum(double existingVelocity) => 0;
}

class _MomentumScrollPosition extends ScrollPositionWithSingleContext {
  _MomentumScrollPosition({
    required super.physics,
    required super.context,
    super.initialPixels,
    super.keepScrollOffset,
    super.oldPosition,
    super.debugLabel,
  });

  final _sinceMotion = Stopwatch();
  double _carriedVelocity = 0;

  @override
  ScrollHoldController hold(VoidCallback holdCancelCallback) {
    _carriedVelocity = activity is BallisticScrollActivity && !outOfRange
        ? activity!.velocity
        : 0;
    _sinceMotion
      ..reset()
      ..start();
    return super.hold(holdCancelCallback);
  }

  @override
  void applyUserOffset(double delta) {
    // A reversal or a deliberate pause stops carrying the previous fling.
    if (_sinceMotion.elapsedMilliseconds > 100 ||
        delta * _carriedVelocity > 0) {
      _carriedVelocity = 0;
    }
    if (delta != 0) _sinceMotion.reset();
    super.applyUserOffset(delta);
  }

  @override
  void goBallistic(double velocity) {
    if (activity is DragScrollActivity &&
        !outOfRange &&
        _sinceMotion.elapsedMilliseconds <= 100 &&
        velocity.abs() >= physics.minFlingVelocity &&
        velocity * _carriedVelocity > 0) {
      velocity = (velocity + _carriedVelocity).clamp(
        -physics.maxFlingVelocity,
        physics.maxFlingVelocity,
      );
    }
    _carriedVelocity = 0;
    super.goBallistic(velocity);
  }
}
