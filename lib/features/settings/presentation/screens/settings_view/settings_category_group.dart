part of 'settings_view.dart';

extension _SettingsCategoryGroup on _SettingsViewState {
  Widget _buildCategoryGroup({
    required BuildContext context,
    required String id,
    required String title,
    required String subtitle,
    required IconData icon,
    required ColorScheme colorScheme,
    required bool useBlur,
    required AppSettings settings,
  }) {
    final accent = Color(settings.accentColor);

    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: GlassContainer.card(
        surfaceStyle: GlassSurfaceStyle.solid,
        color: colorScheme.surfaceContainer,
        shape: const LiquidRoundedSuperellipse(borderRadius: 23),
        padding: EdgeInsets.zero,
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: () {
            AppHaptics.light();
            if (id == 'support_us') {
              _showSupportUsDialog(context, colorScheme, useBlur);
              return;
            }
            // Keep settings navigation in the shared history so system back
            // always returns to this list in the expected order.
            ref
                .read(appNavigationProvider.notifier)
                .showSettingsCategory(id: id, title: title);
          },
          borderRadius: BorderRadius.circular(23),
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 15, 18, 15),
            child: Row(
              children: [
                Container(
                  width: 46,
                  height: 46,
                  decoration: BoxDecoration(
                    color: accent.withValues(alpha: 0.11),
                    borderRadius: BorderRadius.circular(15),
                    border: Border.all(
                      color: accent.withValues(alpha: 0.16),
                    ),
                  ),
                  alignment: Alignment.center,
                  child: Icon(icon, color: accent, size: 21),
                ),
                const SizedBox(width: 15),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        title,
                        style: AppFonts.jostStyle(
                          color: colorScheme.onSurface,
                          fontSize: 15,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        subtitle,
                        style: AppFonts.jostStyle(
                          color: colorScheme.onSurfaceVariant.withValues(
                            alpha: 0.82,
                          ),
                          fontSize: 12,
                          height: 1.3,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 10),
                Icon(
                  LucideIcons.chevronRight,
                  color: colorScheme.onSurfaceVariant.withValues(alpha: 0.6),
                  size: 18,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
