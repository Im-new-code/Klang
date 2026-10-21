part of 'android_search_tab.dart';

class _SearchHeader extends StatelessWidget {
  final TextEditingController controller;
  final FocusNode focusNode;
  final ValueChanged<String> onChanged;
  final ValueChanged<String> onSubmitted;
  final VoidCallback onClear;
  final VoidCallback onBack;

  const _SearchHeader({
    super.key,
    required this.controller,
    required this.focusNode,
    required this.onChanged,
    required this.onSubmitted,
    required this.onClear,
    required this.onBack,
  });

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    return Padding(
      padding: const EdgeInsets.fromLTRB(4, 10, 16, 6),
      child: Row(
        children: [
          IconButton(
            icon: const Icon(LucideIcons.arrowLeft, size: 22),
            color: Colors.white,
            tooltip: l10n.back,
            onPressed: onBack,
          ),
          Expanded(
            child: Container(
              height: 48,
              decoration: BoxDecoration(
                color: Colors.white.withValues(alpha: 0.07),
                borderRadius: BorderRadius.circular(24),
                border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
              ),
              child: TextField(
                controller: controller,
                focusNode: focusNode,
                textInputAction: TextInputAction.search,
                onChanged: onChanged,
                onSubmitted: onSubmitted,
                style: AppFonts.jostStyle(color: Colors.white, fontSize: 15),
                cursorColor: Theme.of(context).colorScheme.primary,
                decoration: InputDecoration(
                  hintText: l10n.searchLibraryHint,
                  hintStyle: AppFonts.jostStyle(
                    color: Colors.white.withValues(alpha: 0.45),
                    fontSize: 15,
                  ),
                  prefixIcon: Icon(
                    LucideIcons.search,
                    size: 18,
                    color: Colors.white.withValues(alpha: 0.6),
                  ),
                  suffixIcon: ValueListenableBuilder<TextEditingValue>(
                    valueListenable: controller,
                    builder: (context, value, _) => value.text.isEmpty
                        ? const SizedBox.shrink()
                        : IconButton(
                            icon: const Icon(LucideIcons.x, size: 18),
                            color: Colors.white70,
                            tooltip: l10n.clear,
                            onPressed: onClear,
                          ),
                  ),
                  border: InputBorder.none,
                  contentPadding: const EdgeInsets.symmetric(vertical: 13),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// Replaces the search field while songs are selected.
class _SelectionBar extends StatelessWidget {
  final int count;
  final VoidCallback onCancel;
  final VoidCallback onSelectAll;
  final VoidCallback onAddToPlaylist;

  const _SelectionBar({
    super.key,
    required this.count,
    required this.onCancel,
    required this.onSelectAll,
    required this.onAddToPlaylist,
  });

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final accent = Theme.of(context).colorScheme.primary;
    return Padding(
      padding: const EdgeInsets.fromLTRB(4, 10, 8, 6),
      child: SizedBox(
        height: 48,
        child: Row(
          children: [
            IconButton(
              icon: const Icon(LucideIcons.x, size: 22),
              color: Colors.white,
              tooltip: l10n.cancel,
              onPressed: onCancel,
            ),
            Expanded(
              child: Text(
                l10n.selectedCount(count),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: AppFonts.jostStyle(
                  color: Colors.white,
                  fontSize: 17,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
            TextButton(onPressed: onSelectAll, child: Text(l10n.selectAll)),
            IconButton(
              icon: const Icon(LucideIcons.listPlus, size: 22),
              color: accent,
              tooltip: l10n.addToPlaylists,
              onPressed: onAddToPlaylist,
            ),
          ],
        ),
      ),
    );
  }
}

/// The row of result-kind filters under the search field.
class _KindChips extends StatelessWidget {
  final Map<_SearchKind, int> counts;
  final _SearchKind selected;
  final ValueChanged<_SearchKind> onSelected;

  const _KindChips({
    required this.counts,
    required this.selected,
    required this.onSelected,
  });

  String _label(AppLocalizations l10n, _SearchKind kind) => switch (kind) {
    _SearchKind.all => l10n.searchFilterAll,
    _SearchKind.songs => l10n.songs,
    _SearchKind.artists => l10n.artists,
    _SearchKind.albums => l10n.albums,
    _SearchKind.playlists => l10n.playlists,
    _SearchKind.genres => l10n.genres,
  };

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final colorScheme = Theme.of(context).colorScheme;
    final kinds = counts.keys.toList();
    return SizedBox(
      height: 46,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.fromLTRB(16, 4, 16, 6),
        itemCount: kinds.length,
        separatorBuilder: (_, _) => const SizedBox(width: 8),
        itemBuilder: (context, index) {
          final kind = kinds[index];
          final isSelected = kind == selected;
          final foreground = isSelected ? colorScheme.onPrimary : Colors.white;
          return Material(
            color: isSelected
                ? colorScheme.primary
                : Colors.white.withValues(alpha: 0.07),
            borderRadius: BorderRadius.circular(18),
            child: InkWell(
              borderRadius: BorderRadius.circular(18),
              onTap: isSelected ? null : () => onSelected(kind),
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 14),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      _label(l10n, kind),
                      style: AppFonts.jostStyle(
                        color: foreground,
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    if (kind != _SearchKind.all) ...[
                      const SizedBox(width: 6),
                      Text(
                        '${counts[kind]}',
                        style: AppFonts.jostStyle(
                          color: foreground.withValues(alpha: 0.65),
                          fontSize: 13,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
            ),
          );
        },
      ),
    );
  }
}
