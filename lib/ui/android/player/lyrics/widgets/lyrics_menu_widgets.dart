part of 'android_lyrics_menu_sheet.dart';

/// Rounded card grouping related menu rows, with hairline dividers between
/// them.
class _MenuSection extends StatelessWidget {
  final List<Widget> children;

  const _MenuSection({required this.children});

  @override
  Widget build(BuildContext context) {
    return Container(
      clipBehavior: Clip.antiAlias,
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.05),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: Colors.white.withValues(alpha: 0.06)),
      ),
      child: Material(
        type: MaterialType.transparency,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            for (var i = 0; i < children.length; i++) ...[
              if (i > 0)
                Divider(color: Colors.white.withValues(alpha: 0.06), height: 1),
              children[i],
            ],
          ],
        ),
      ),
    );
  }
}

class _SectionLabel extends StatelessWidget {
  final String text;

  const _SectionLabel(this.text);

  @override
  Widget build(BuildContext context) {
    return Text(
      text,
      style: AppFonts.jostStyle(
        color: Colors.white70,
        fontSize: 13,
        fontWeight: FontWeight.w600,
      ),
    );
  }
}

/// Leading icon badge shared by the menu's switch and action rows.
class _MenuIcon extends StatelessWidget {
  final IconData icon;
  final Color color;

  const _MenuIcon(this.icon, {this.color = Colors.white70});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(8),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(10),
      ),
      child: Icon(icon, color: color, size: 18),
    );
  }
}

class _MenuActionTile extends StatelessWidget {
  final IconData icon;
  final Color iconColor;
  final String title;
  final String? subtitle;
  final VoidCallback onTap;

  const _MenuActionTile({
    required this.icon,
    required this.title,
    required this.onTap,
    this.subtitle,
    this.iconColor = Colors.white70,
  });

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 16),
      dense: true,
      visualDensity: VisualDensity.compact,
      leading: _MenuIcon(icon, color: iconColor),
      title: Text(
        title,
        style: AppFonts.jostStyle(
          color: Colors.white,
          fontWeight: FontWeight.w500,
          fontSize: 15,
        ),
      ),
      subtitle: subtitle == null
          ? null
          : Text(
              subtitle!,
              style: AppFonts.jostStyle(color: Colors.white54, fontSize: 12),
            ),
      trailing: const Icon(
        LucideIcons.chevronRight,
        color: Colors.white38,
        size: 18,
      ),
      onTap: onTap,
    );
  }
}

class _MenuSwitchTile extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;
  final bool value;
  final Color accent;
  final ValueChanged<bool> onChanged;

  const _MenuSwitchTile({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.value,
    required this.accent,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return SwitchListTile(
      contentPadding: const EdgeInsets.only(left: 16, right: 10),
      dense: true,
      visualDensity: VisualDensity.compact,
      activeColor: accent,
      secondary: _MenuIcon(icon),
      title: Text(
        title,
        style: AppFonts.jostStyle(
          color: Colors.white,
          fontWeight: FontWeight.w500,
          fontSize: 15,
        ),
      ),
      subtitle: Text(
        subtitle,
        style: AppFonts.jostStyle(color: Colors.white54, fontSize: 12),
      ),
      value: value,
      onChanged: (val) {
        AppHaptics.light();
        onChanged(val);
      },
    );
  }
}

/// "Word-synced" / "Line-synced" / "Not synced" pill next to the lyrics
/// source.
class _SyncBadge extends StatelessWidget {
  final bool wordSynced;
  final bool synced;
  final Color accent;

  const _SyncBadge({
    required this.wordSynced,
    required this.synced,
    required this.accent,
  });

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final color = wordSynced ? accent : Colors.white54;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: color.withValues(alpha: 0.3)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            wordSynced
                ? LucideIcons.sparkles
                : synced
                ? LucideIcons.alignLeft
                : LucideIcons.timerOff,
            size: 13,
            color: color,
          ),
          const SizedBox(width: 6),
          Text(
            wordSynced
                ? l10n.lyricsWordSynced
                : synced
                ? l10n.lyricsLineSynced
                : l10n.lyricsNotSynced,
            style: AppFonts.jostStyle(
              color: color,
              fontSize: 12,
              fontWeight: FontWeight.w600,
            ),
          ),
        ],
      ),
    );
  }
}

/// Choice chip used by both the highlight-style and provider pickers.
class _MenuChip extends StatelessWidget {
  final String label;
  final bool selected;
  final Color accent;
  final VoidCallback onSelected;

  const _MenuChip({
    required this.label,
    required this.selected,
    required this.accent,
    required this.onSelected,
  });

  @override
  Widget build(BuildContext context) {
    return ChoiceChip(
      label: Text(
        label,
        style: AppFonts.jostStyle(
          color: selected ? Colors.white : Colors.white70,
          fontWeight: selected ? FontWeight.bold : FontWeight.normal,
          fontSize: 13,
        ),
      ),
      selected: selected,
      showCheckmark: false,
      visualDensity: VisualDensity.compact,
      materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
      selectedColor: accent,
      backgroundColor: Colors.white.withValues(alpha: 0.06),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(
          color: selected ? accent : Colors.white.withValues(alpha: 0.1),
        ),
      ),
      onSelected: (_) => onSelected(),
    );
  }
}

/// Lyrics timing offset: nudges this song's lyric timing later/earlier in
/// 0.1 s steps. Every song starts at the app default; a change is saved for
/// the song it was made on only.
class _LyricsOffsetControl extends StatelessWidget {
  static const int _step = 100;

  final int offsetMs;
  final Color accent;
  final ValueChanged<int> onChanged;

  const _LyricsOffsetControl({
    required this.offsetMs,
    required this.accent,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    // Positive = lyrics earlier; the default nudges them a little early.
    const int defaultMs = AppSettings.defaultLyricsOffsetMs;
    final bool isDefault = offsetMs == defaultMs;
    final seconds = (offsetMs / 1000).toStringAsFixed(1);
    final label = offsetMs > 0 ? '+$seconds s' : '$seconds s';

    Widget stepButton(IconData icon, String tooltip, int delta) => IconButton(
      tooltip: tooltip,
      visualDensity: VisualDensity.compact,
      icon: Icon(icon, size: 18, color: Colors.white70),
      onPressed: () {
        AppHaptics.selection();
        onChanged(offsetMs + delta);
      },
    );

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          l10n.lyricsTiming,
          style: AppFonts.jostStyle(
            color: Colors.white70,
            fontSize: 13,
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: 2),
        Text(
          l10n.lyricsTimingDesc,
          style: AppFonts.jostStyle(color: Colors.white38, fontSize: 12),
        ),
        Row(
          children: [
            stepButton(LucideIcons.minus, l10n.lyricsTimingLater, -_step),
            Container(
              constraints: const BoxConstraints(minWidth: 72),
              alignment: Alignment.center,
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
              decoration: BoxDecoration(
                color: (isDefault ? Colors.white : accent).withValues(
                  alpha: 0.1,
                ),
                borderRadius: BorderRadius.circular(14),
              ),
              child: Text(
                label,
                style: AppFonts.jostStyle(
                  color: isDefault ? Colors.white70 : accent,
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
            stepButton(LucideIcons.plus, l10n.lyricsTimingEarlier, _step),
            const Spacer(),
            if (!isDefault)
              TextButton(
                onPressed: () {
                  AppHaptics.selection();
                  onChanged(defaultMs);
                },
                child: Text(
                  l10n.lyricsTimingReset,
                  style: AppFonts.jostStyle(color: accent, fontSize: 13),
                ),
              ),
          ],
        ),
      ],
    );
  }
}
