part of 'android_search_tab.dart';

/// [text] with every occurrence of [query] picked out in the accent colour.
class _Highlighted extends StatelessWidget {
  final String text;
  final String query;
  final TextStyle style;

  const _Highlighted({
    required this.text,
    required this.query,
    required this.style,
  });

  @override
  Widget build(BuildContext context) {
    final lowerText = text.toLowerCase();
    final lowerQuery = query.toLowerCase();
    final spans = <TextSpan>[];
    // Lower-casing can change a string's length in a few scripts; highlighting
    // by index is only safe when it did not.
    if (lowerQuery.isNotEmpty && lowerText.length == text.length) {
      final highlight = style.copyWith(
        color: Theme.of(context).colorScheme.primary,
        fontWeight: FontWeight.w700,
      );
      var start = 0;
      int match;
      while ((match = lowerText.indexOf(lowerQuery, start)) != -1) {
        if (match > start) {
          spans.add(TextSpan(text: text.substring(start, match)));
        }
        start = match + lowerQuery.length;
        spans.add(
          TextSpan(text: text.substring(match, start), style: highlight),
        );
      }
      if (spans.isNotEmpty && start < text.length) {
        spans.add(TextSpan(text: text.substring(start)));
      }
    }
    return Text.rich(
      spans.isEmpty ? TextSpan(text: text) : TextSpan(children: spans),
      style: style,
      maxLines: 1,
      overflow: TextOverflow.ellipsis,
    );
  }
}

class _SectionHeader extends StatelessWidget {
  final String title;
  final String? actionLabel;
  final VoidCallback? onAction;
  // A count line ("12 songs") rather than a section title.
  final bool quiet;
  final EdgeInsets padding;

  const _SectionHeader({
    required this.title,
    this.actionLabel,
    this.onAction,
    this.quiet = false,
    this.padding = const EdgeInsets.fromLTRB(16, 8, 8, 0),
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: padding,
      child: ConstrainedBox(
        constraints: const BoxConstraints(minHeight: 44),
        child: Row(
          children: [
            Expanded(
              child: Text(
                title,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: quiet
                    ? AppFonts.jostStyle(
                        color: Colors.white.withValues(alpha: 0.55),
                        fontSize: 14,
                      )
                    : AppFonts.jostStyle(
                        color: Colors.white,
                        fontSize: 18,
                        fontWeight: FontWeight.w600,
                      ),
              ),
            ),
            if (actionLabel != null)
              TextButton(onPressed: onAction, child: Text(actionLabel!)),
          ],
        ),
      ),
    );
  }
}

/// A square or round thumbnail: the artwork at [path], or [icon] on a tinted
/// tile when there is none.
class _Thumb extends StatelessWidget {
  final String? path;
  final IconData icon;
  final bool round;
  final bool accent;
  final double size;

  const _Thumb({
    this.path,
    required this.icon,
    this.round = false,
    this.accent = false,
    this.size = 52,
  });

  @override
  Widget build(BuildContext context) {
    final accentColor = Theme.of(context).colorScheme.primary;
    final fallback = Container(
      width: size,
      height: size,
      color: accent
          ? accentColor.withValues(alpha: 0.14)
          : Colors.white.withValues(alpha: 0.07),
      alignment: Alignment.center,
      child: Icon(
        icon,
        size: size * 0.42,
        color: accent ? accentColor : Colors.white.withValues(alpha: 0.5),
      ),
    );
    final image = path == null
        ? fallback
        : OptimizedImage(
            imagePath: path,
            width: size,
            height: size,
            placeholder: fallback,
          );
    return SizedBox.square(
      dimension: size,
      child: round
          ? ClipOval(child: image)
          : ClipRRect(
              borderRadius: BorderRadius.circular(size * 0.2),
              child: image,
            ),
    );
  }
}

/// One album / artist / playlist / genre match as a full-width row.
class _ResultRow extends StatelessWidget {
  final Widget thumb;
  final String title;
  final String subtitle;
  final String query;
  final VoidCallback onTap;

  const _ResultRow({
    required this.thumb,
    required this.title,
    required this.subtitle,
    required this.query,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: () {
        AppHaptics.light();
        onTap();
      },
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 8, 12, 8),
        child: Row(
          children: [
            thumb,
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _Highlighted(
                    text: title,
                    query: query,
                    style: AppFonts.jostStyle(
                      color: Colors.white,
                      fontSize: 16,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    subtitle,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: AppFonts.jostStyle(
                      color: Colors.white.withValues(alpha: 0.5),
                      fontSize: 13,
                    ),
                  ),
                ],
              ),
            ),
            Icon(
              LucideIcons.chevronRight,
              size: 18,
              color: Colors.white.withValues(alpha: 0.3),
            ),
          ],
        ),
      ),
    );
  }
}

class _ArtistBubble extends StatelessWidget {
  final Artist artist;
  final VoidCallback onTap;

  const _ArtistBubble({required this.artist, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      borderRadius: BorderRadius.circular(16),
      onTap: () {
        AppHaptics.light();
        onTap();
      },
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 4),
        child: SizedBox(
          width: 84,
          child: Column(
            children: [
              _Thumb(
                path: artist.artPath,
                icon: LucideIcons.user,
                round: true,
                size: 80,
              ),
              const SizedBox(height: 8),
              Text(
                artist.name,
                textAlign: TextAlign.center,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: AppFonts.jostStyle(
                  color: Colors.white,
                  fontSize: 12,
                  height: 1.2,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _AlbumCard extends StatelessWidget {
  final Album album;
  final VoidCallback onTap;

  const _AlbumCard({required this.album, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    return InkWell(
      borderRadius: BorderRadius.circular(16),
      onTap: () {
        AppHaptics.light();
        onTap();
      },
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 4),
        child: SizedBox(
          width: 124,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _Thumb(path: album.artPath, icon: LucideIcons.disc3, size: 124),
              const SizedBox(height: 8),
              Text(
                album.name,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: AppFonts.jostStyle(
                  color: Colors.white,
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                ),
              ),
              Text(
                album.artist ?? l10n.unknownArtist,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: AppFonts.jostStyle(
                  color: Colors.white.withValues(alpha: 0.5),
                  fontSize: 12,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// The best-matching song, as a card that plays it on tap. Shows the lyric
/// line that matched when the title itself did not.
class _TopResultCard extends ConsumerWidget {
  final Song song;
  final String query;
  final VoidCallback onPlay;

  const _TopResultCard({
    required this.song,
    required this.query,
    required this.onPlay,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final colorScheme = Theme.of(context).colorScheme;
    final isCurrent = ref.watch(
      playbackProvider.select((s) => s.currentSong?.path == song.path),
    );
    final lyrics = song.lyrics;
    final snippet =
        lyrics == null || song.title.toLowerCase().contains(query.toLowerCase())
        ? null
        : lyricSnippet(lyrics, query);

    return Material(
      color: isCurrent
          ? colorScheme.primary.withValues(alpha: 0.18)
          : Colors.white.withValues(alpha: 0.05),
      borderRadius: BorderRadius.circular(20),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () {
          AppHaptics.light();
          onPlay();
          ref.read(lyricsSearchQueryProvider.notifier).clear();
          ref.read(playbackProvider.notifier).play(song);
        },
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  _Thumb(path: song.artPath, icon: LucideIcons.music, size: 76),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        _Highlighted(
                          text: song.title,
                          query: query,
                          style: AppFonts.jostStyle(
                            color: Colors.white,
                            fontSize: 19,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        const SizedBox(height: 4),
                        _Highlighted(
                          text: song.artist ?? l10n.unknownArtist,
                          query: query,
                          style: AppFonts.jostStyle(
                            color: Colors.white.withValues(alpha: 0.6),
                            fontSize: 14,
                          ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          song.album ?? l10n.unknownAlbum,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: AppFonts.jostStyle(
                            color: Colors.white.withValues(alpha: 0.4),
                            fontSize: 12,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 12),
                  Container(
                    width: 44,
                    height: 44,
                    decoration: BoxDecoration(
                      color: colorScheme.primary,
                      shape: BoxShape.circle,
                    ),
                    child: Icon(
                      Icons.play_arrow_rounded,
                      color: colorScheme.onPrimary,
                      size: 26,
                    ),
                  ),
                ],
              ),
              if (snippet != null) ...[
                const SizedBox(height: 12),
                Row(
                  children: [
                    Icon(
                      LucideIcons.quote,
                      size: 12,
                      color: colorScheme.primary,
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: _Highlighted(
                        text: snippet,
                        query: query,
                        style: AppFonts.jostStyle(
                          color: Colors.white.withValues(alpha: 0.75),
                          fontSize: 13,
                          fontStyle: FontStyle.italic,
                        ),
                      ),
                    ),
                  ],
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
