import 'package:flutter/material.dart';

/// Pull-to-refresh wrapper used across the library screens. The spinner
/// stays pinned for as long as [onRefresh] is still working and is released
/// when it finishes, so it doubles as the "still processing" signal.
///
/// It never shows for less than [_minimumVisible], so a refresh that returns
/// at once still reads as having happened, and a failure in [onRefresh]
/// releases it like a normal finish.
class AppRefreshIndicator extends StatelessWidget {
  static const _minimumVisible = Duration(milliseconds: 350);

  final Widget child;
  final Future<void> Function() onRefresh;

  const AppRefreshIndicator({
    super.key,
    required this.child,
    required this.onRefresh,
  });

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
      color: Theme.of(context).colorScheme.primary,
      backgroundColor: const Color(0xFF1E1E1E),
      onRefresh: () => Future.wait([
        Future<void>.sync(onRefresh).catchError((_) {}),
        Future<void>.delayed(_minimumVisible),
      ]),
      child: child,
    );
  }
}
