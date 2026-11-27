import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:liquid_glass_widgets/liquid_glass_widgets.dart';
import 'package:looper_player/core/providers/navigation_provider.dart';
import 'package:looper_player/core/providers/providers.dart';
import 'package:looper_player/core/services/update/apk_installer.dart';
import 'package:looper_player/core/theme/app_fonts.dart';
import 'package:looper_player/features/settings/presentation/widgets/tiles/about_settings_tiles.dart';
import 'package:looper_player/l10n/app_localizations.dart';

/// Klang's own product page. Legal source attribution is kept separate from
/// the app's identity and feature description.
class KlangAboutScreen extends ConsumerWidget {
  const KlangAboutScreen({super.key});

  static const Color _background = Color(0xFF0A101B);
  static const Color _accent = Color(0xFF8EDCFF);
  static const Color _card = Color(0xFF122032);

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context)!;
    final version = ref.watch(appVersionProvider).value ?? '';

    return Scaffold(
      backgroundColor: _background,
      body: Stack(
        children: [
          const Positioned.fill(
            child: IgnorePointer(
              child: DecoratedBox(
                decoration: BoxDecoration(
                  gradient: RadialGradient(
                    center: Alignment(-0.85, -1),
                    radius: 1.35,
                    colors: [_card, _background],
                    stops: [0, 0.86],
                  ),
                ),
              ),
            ),
          ),
          SafeArea(
            child: Column(
              children: [
                Padding(
                  padding: const EdgeInsets.fromLTRB(12, 8, 20, 8),
                  child: Row(
                    children: [
                      IconButton(
                        tooltip: MaterialLocalizations.of(context).backButtonTooltip,
                        onPressed: () =>
                            ref.read(appNavigationProvider.notifier).goBack(),
                        icon: const Icon(Icons.arrow_back_rounded),
                        color: Colors.white,
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          l10n.about,
                          style: AppFonts.jostStyle(
                            color: Colors.white,
                            fontSize: 21,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                      ),
                      Text(
                        version.isEmpty ? 'KLANG' : 'v$version',
                        style: AppFonts.jostStyle(
                          color: _accent.withValues(alpha: 0.78),
                          fontSize: 11,
                          fontWeight: FontWeight.w700,
                          letterSpacing: 1.1,
                        ),
                      ),
                    ],
                  ),
                ),
                Expanded(
                  child: ListView(
                    physics: const BouncingScrollPhysics(),
                    padding: const EdgeInsets.fromLTRB(18, 12, 18, 36),
                    children: [
                      GlassContainer(
                        useOwnLayer: true,
                        quality: GlassQuality.standard,
                        shape: const LiquidRoundedSuperellipse(borderRadius: 28),
                        color: const Color(0x248EDCFF),
                        padding: const EdgeInsets.fromLTRB(24, 26, 24, 24),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Container(
                              width: 58,
                              height: 58,
                              decoration: BoxDecoration(
                                color: const Color(0x338EDCFF),
                                borderRadius: BorderRadius.circular(19),
                                border: Border.all(color: const Color(0x778EDCFF)),
                              ),
                              child: const Icon(
                                Icons.graphic_eq_rounded,
                                color: _accent,
                                size: 32,
                              ),
                            ),
                            const SizedBox(height: 22),
                            Text(
                              'Klang',
                              style: AppFonts.jostStyle(
                                color: Colors.white,
                                fontSize: 42,
                                fontWeight: FontWeight.w800,
                                height: 1,
                                letterSpacing: -1.2,
                              ),
                            ),
                            const SizedBox(height: 8),
                            Text(
                              'Your library. Your sound.',
                              style: AppFonts.jostStyle(
                                color: _accent,
                                fontSize: 17,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                            const SizedBox(height: 12),
                            Text(
                              'A local-first music player built around the collection you already own.',
                              style: AppFonts.jostStyle(
                                color: Colors.white.withValues(alpha: 0.76),
                                fontSize: 14,
                                height: 1.5,
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 30),
                      const _AboutSectionLabel('MADE FOR YOUR MUSIC'),
                      const SizedBox(height: 12),
                      Text(
                        l10n.corePurposeDesc,
                        style: AppFonts.jostStyle(
                          color: Colors.white.withValues(alpha: 0.72),
                          fontSize: 14,
                          height: 1.55,
                        ),
                      ),
                      const SizedBox(height: 22),
                      GlassContainer.card(
                        surfaceStyle: GlassSurfaceStyle.solid,
                        color: _card,
                        shape: const LiquidRoundedSuperellipse(borderRadius: 24),
                        padding: const EdgeInsets.fromLTRB(18, 6, 18, 6),
                        child: const Column(
                          children: [
                            _AboutFeature(
                              icon: Icons.library_music_rounded,
                              title: 'Your collection, organised',
                              description:
                                  'Browse tracks, albums, artists, playlists and folders stored on your device.',
                            ),
                            _AboutDivider(),
                            _AboutFeature(
                              icon: Icons.lyrics_rounded,
                              title: 'Lyrics that keep time',
                              description:
                                  'Read embedded lyrics, use local LRC files, or look up synced lyrics when online services are enabled.',
                            ),
                            _AboutDivider(),
                            _AboutFeature(
                              icon: Icons.tune_rounded,
                              title: 'Playback with room to tune',
                              description:
                                  'Shape your sound with the equalizer, queue controls and smooth playback transitions.',
                            ),
                            _AboutDivider(),
                            _AboutFeature(
                              icon: Icons.blur_on_rounded,
                              title: 'An interface that adapts',
                              description:
                                  'Liquid-glass navigation adjusts its rendering quality to the device instead of forcing the heaviest effects everywhere.',
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 30),
                      const _AboutSectionLabel('THE KLANG PROJECT'),
                      const SizedBox(height: 12),
                      GlassContainer.card(
                        surfaceStyle: GlassSurfaceStyle.solid,
                        color: _card,
                        shape: const LiquidRoundedSuperellipse(borderRadius: 24),
                        padding: const EdgeInsets.symmetric(vertical: 4),
                        child: Column(
                          children: [
                            const GitHubStarTile(),
                            const _AboutDivider(),
                            if (ApkInstaller.supported) ...[
                              const CheckForUpdatesTile(),
                              const _AboutDivider(),
                            ],
                            const OpenSourceLicensesTile(),
                          ],
                        ),
                      ),
                      const SizedBox(height: 26),
                      const _AboutSectionLabel('CREDITS & LEGAL'),
                      const SizedBox(height: 10),
                      Text(
                        'Klang has its own product direction and interface. Some application code is derived from the GPL-3.0 Looper Player project. The original authors and copyright notices remain credited under the applicable licence.',
                        style: AppFonts.jostStyle(
                          color: Colors.white.withValues(alpha: 0.5),
                          fontSize: 12,
                          height: 1.55,
                        ),
                      ),
                      const SizedBox(height: 20),
                      Center(
                        child: Text(
                          'MADE FOR THE MUSIC YOU KEEP',
                          style: AppFonts.jostStyle(
                            color: Colors.white.withValues(alpha: 0.28),
                            fontSize: 10,
                            fontWeight: FontWeight.w700,
                            letterSpacing: 1.3,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _AboutSectionLabel extends StatelessWidget {
  const _AboutSectionLabel(this.text);
  final String text;

  @override
  Widget build(BuildContext context) => Text(
        text,
        style: AppFonts.jostStyle(
          color: const Color(0xFF8EDCFF),
          fontSize: 11,
          fontWeight: FontWeight.w800,
          letterSpacing: 1.35,
        ),
      );
}

class _AboutFeature extends StatelessWidget {
  const _AboutFeature({
    required this.icon,
    required this.title,
    required this.description,
  });

  final IconData icon;
  final String title;
  final String description;

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 15),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              width: 42,
              height: 42,
              decoration: BoxDecoration(
                color: const Color(0x228EDCFF),
                borderRadius: BorderRadius.circular(14),
              ),
              child: Icon(icon, color: const Color(0xFF8EDCFF), size: 21),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: AppFonts.jostStyle(
                      color: Colors.white,
                      fontSize: 15,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    description,
                    style: AppFonts.jostStyle(
                      color: Colors.white.withValues(alpha: 0.57),
                      fontSize: 12,
                      height: 1.45,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      );
}

class _AboutDivider extends StatelessWidget {
  const _AboutDivider();

  @override
  Widget build(BuildContext context) => Divider(
        height: 1,
        thickness: 1,
        color: Colors.white.withValues(alpha: 0.07),
      );
}
