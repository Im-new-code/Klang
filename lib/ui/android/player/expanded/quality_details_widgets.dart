part of 'quality_details_sheet.dart';

/// Tier headline: the badge icon in a tinted circle (popping in once),
/// then the tier, its details and the song. Leaves room on the right for
/// the dialog's close button.
class _Header extends StatelessWidget {
  final Animation<double> intro;
  final Color tint;
  final Widget icon;
  final String title;
  final String subtitle;
  final String song;

  const _Header({
    required this.intro,
    required this.tint,
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.song,
  });

  @override
  Widget build(BuildContext context) {
    final pop = CurvedAnimation(
      parent: intro,
      curve: const Interval(0, 0.7, curve: Curves.easeOutBack),
    );
    return Padding(
      padding: const EdgeInsets.only(right: 32),
      child: Row(
        children: [
          ScaleTransition(
            scale: Tween(begin: 0.7, end: 1.0).animate(pop),
            child: Container(
              width: 48,
              height: 48,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: tint.withValues(alpha: 0.12),
                border: Border.all(color: tint.withValues(alpha: 0.35)),
              ),
              child: icon,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: AppFonts.jostStyle(
                    color: tint,
                    fontSize: 19,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                Text(
                  subtitle,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: AppFonts.jostStyle(
                    color: Colors.white70,
                    fontSize: 12.5,
                    fontWeight: FontWeight.w600,
                    letterSpacing: 0.3,
                  ),
                ),
                Text(
                  song,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: AppFonts.jostStyle(
                    color: Colors.white38,
                    fontSize: 11.5,
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

class _Stage {
  final IconData icon;
  final String label;
  final String value;
  final String? detail;

  const _Stage(this.icon, this.label, this.value, this.detail);
}

/// Source -> decoder -> output -> device, with arrows that light up in
/// turn like a signal flowing through.
class _SignalPath extends StatelessWidget {
  final Animation<double> pulse;
  final Color tint;
  final List<_Stage> stages;

  const _SignalPath({
    required this.pulse,
    required this.tint,
    required this.stages,
  });

  @override
  Widget build(BuildContext context) {
    final children = <Widget>[];
    for (int i = 0; i < stages.length; i++) {
      if (i > 0) {
        children.add(
          AnimatedBuilder(
            animation: pulse,
            builder: (context, _) {
              // A bright spot travels left to right once per cycle.
              final double d = (pulse.value * (stages.length) - i).abs();
              final double glow = (1 - d.clamp(0.0, 1.0));
              return Icon(
                LucideIcons.chevronRight,
                size: 14,
                color: Color.lerp(Colors.white24, tint, glow),
              );
            },
          ),
        );
      }
      children.add(
        Expanded(
          child: _StageNode(stage: stages[i], tint: tint),
        ),
      );
    }
    return Row(children: children);
  }
}

class _StageNode extends StatelessWidget {
  final _Stage stage;
  final Color tint;

  const _StageNode({required this.stage, required this.tint});

  @override
  Widget build(BuildContext context) {
    TextStyle small(Color c, {FontWeight w = FontWeight.w500}) =>
        AppFonts.jostStyle(color: c, fontSize: 10.5, fontWeight: w);
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 4),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.04),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: Colors.white10, width: 0.6),
      ),
      child: Column(
        children: [
          Icon(stage.icon, size: 16, color: tint),
          const SizedBox(height: 4),
          Text(
            stage.label.toUpperCase(),
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: small(Colors.white38, w: FontWeight.w700),
          ),
          const SizedBox(height: 2),
          Text(
            stage.value,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: small(Colors.white, w: FontWeight.w600),
          ),
          Text(
            stage.detail ?? ' ',
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: small(Colors.white54),
          ),
        ],
      ),
    );
  }
}

class _Verdict extends StatelessWidget {
  final bool ok;
  final String text;

  const _Verdict({required this.ok, required this.text});

  @override
  Widget build(BuildContext context) {
    final Color c = ok ? const Color(0xFF7BD88F) : const Color(0xFFF5B657);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 7),
      decoration: BoxDecoration(
        color: c.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: c.withValues(alpha: 0.3), width: 0.6),
      ),
      child: Row(
        children: [
          Icon(
            ok ? LucideIcons.circleCheck : LucideIcons.arrowRightLeft,
            size: 16,
            color: c,
          ),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              text,
              style: AppFonts.jostStyle(
                color: c,
                fontSize: 12.5,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _SectionTitle extends StatelessWidget {
  final String text;

  const _SectionTitle(this.text);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(4, 0, 4, 8),
      child: Text(
        text.toUpperCase(),
        style: AppFonts.jostStyle(
          color: Colors.white54,
          fontSize: 12,
          fontWeight: FontWeight.bold,
          letterSpacing: 1.0,
        ),
      ),
    );
  }
}

class _Section extends StatelessWidget {
  final IconData icon;
  final String title;
  final String? note;
  final List<(String, String)> rows;

  const _Section({
    required this.icon,
    required this.title,
    required this.rows,
    this.note,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(top: 10),
      padding: const EdgeInsets.fromLTRB(14, 10, 14, 6),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.04),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.white10, width: 0.6),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              Icon(icon, size: 15, color: Colors.white70),
              const SizedBox(width: 8),
              Text(
                title,
                style: AppFonts.jostStyle(
                  color: Colors.white,
                  fontSize: 14,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ),
          const SizedBox(height: 4),
          for (final (label, value) in rows)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 4),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    label,
                    style: AppFonts.jostStyle(
                      color: Colors.white54,
                      fontSize: 12.5,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Text(
                      value,
                      textAlign: TextAlign.end,
                      style: AppFonts.jostStyle(
                        color: Colors.white,
                        fontSize: 12.5,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          if (note != null)
            Padding(
              padding: const EdgeInsets.only(top: 4, bottom: 6),
              child: Text(
                note!,
                style: AppFonts.jostStyle(color: Colors.white38, fontSize: 11),
              ),
            ),
        ],
      ),
    );
  }
}

/// The centered dialog card; the card is the hero the badge flies into.
class QualityDialogFrame extends StatelessWidget {
  final Widget child;

  const QualityDialogFrame({super.key, required this.child});

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Center(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 24),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 480),
            child: Hero(
              tag: qualityBadgeHeroTag,
              flightShuttleBuilder: _morphShuttle,
              child: _DialogCard(
                content: Stack(
                  children: [
                    child,
                    Positioned(
                      top: 6,
                      right: 6,
                      child: IconButton(
                        tooltip: MaterialLocalizations.of(
                          context,
                        ).closeButtonTooltip,
                        icon: const Icon(
                          LucideIcons.x,
                          size: 18,
                          color: Colors.white54,
                        ),
                        onPressed: () => Navigator.of(context).maybePop(),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// The card's surface, drawn like [AppBottomSheetContainer] (frosted blur
/// or the solid sheet color). [content] is kept separate so the morph can
/// draw it over its own animated surface.
class _DialogCard extends ConsumerWidget {
  final Widget content;

  const _DialogCard({required this.content});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final surface = ref.watch(settingsProvider.select(_surface));
    const radius = BorderRadius.all(Radius.circular(_cardRadius));
    Widget body = ColoredBox(
      color: surface.color,
      child: Material(type: MaterialType.transparency, child: content),
    );
    // Skipped entirely when off: BackdropFilter costs a saveLayer every
    // frame even at sigma 0 (same reasoning as AppBottomSheetContainer).
    if (surface.blur) {
      body = BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 10, sigmaY: 10),
        child: body,
      );
    }
    return DecoratedBox(
      position: DecorationPosition.foreground,
      decoration: BoxDecoration(
        borderRadius: radius,
        border: Border.all(color: Colors.white10, width: 0.6),
      ),
      child: ClipRRect(borderRadius: radius, child: body),
    );
  }
}
