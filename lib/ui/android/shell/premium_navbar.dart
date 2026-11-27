import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:liquid_glass_widgets/liquid_glass_widgets.dart';
import 'package:looper_player/core/theme/app_fonts.dart';
import 'package:looper_player/core/services/haptics/app_haptics.dart';
import 'package:looper_player/features/settings/presentation/providers/settings_notifier.dart';
import 'package:looper_player/l10n/app_localizations.dart';
import 'package:looper_player/core/utils/responsive.dart';

class PremiumNavbar extends ConsumerWidget {
  final int currentIndex;
  final ValueChanged<int> onTap;

  const PremiumNavbar({
    super.key,
    required this.currentIndex,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final accentColor = Color(
      ref.watch(settingsProvider.select((settings) => settings.accentColor)),
    );
    final l10n = AppLocalizations.of(context)!;
    final compact = Responsive.isShort(MediaQuery.sizeOf(context));

    return GlassTabBar.bottom(
      tabs: [
        GlassTab(
          icon: const Icon(Icons.home_outlined),
          activeIcon: const Icon(Icons.home_rounded),
          label: l10n.home,
        ),
        GlassTab(
          icon: const Icon(Icons.graphic_eq_rounded),
          activeIcon: const Icon(Icons.equalizer_rounded),
          label: l10n.songs,
        ),
        GlassTab(
          icon: const Icon(Icons.library_music_outlined),
          activeIcon: const Icon(Icons.library_music_rounded),
          label: l10n.library,
        ),
      ],
      selectedIndex: currentIndex.clamp(0, 2),
      onTabSelected: (index) {
        AppHaptics.tick();
        onTap(index);
      },
      barHeight: compact ? 56 : 64,
      verticalPadding: compact ? 5 : 13,
      horizontalPadding: 16,
      spacing: 6,
      barBorderRadius: 36,
      iconSize: compact ? 20 : 22,
      iconLabelSpacing: 3,
      labelFontSize: compact ? 10 : 11,
      selectedIconColor: accentColor,
      selectedLabelColor: accentColor,
      unselectedIconColor: Colors.white.withValues(alpha: 0.65),
      unselectedLabelColor: Colors.white.withValues(alpha: 0.65),
      selectedLabelStyle: AppFonts.jostStyle(
        color: accentColor,
        fontSize: compact ? 10 : 11,
        fontWeight: FontWeight.w700,
      ),
      unselectedLabelStyle: AppFonts.jostStyle(
        color: Colors.white.withValues(alpha: 0.68),
        fontSize: compact ? 10 : 11,
        fontWeight: FontWeight.w500,
      ),
      indicatorColor: accentColor.withValues(alpha: 0.18),
      maskingQuality: MaskingQuality.off,
    );
  }
}
