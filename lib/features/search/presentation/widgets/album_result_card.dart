part of '../screens/search_view.dart';

class _AlbumResultCard extends ConsumerWidget {
  final Album album;
  const _AlbumResultCard({required this.album});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context)!;
    return InkWell(
      onTap: () async {
        await ref
            .read(appNavigationProvider.notifier)
            .showAlbum(album, unknownArtist: l10n.unknownArtist);
      },
      child: Container(
        width: 120,
        margin: const EdgeInsets.only(right: 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            SizedBox(
              width: 120,
              height: 120,
              child: ClipRRect(
                borderRadius: BorderRadius.circular(12),
                child: OptimizedImage(
                  imagePath: album.artPath,
                  fit: BoxFit.cover,
                  placeholder: Container(
                    color: Colors.grey.withValues(
                      alpha: ref.watch(settingsProvider).enableDynamicTheming
                          ? 0.8
                          : 0.1,
                    ),
                    child: const Center(child: Icon(LucideIcons.music)),
                  ),
                ),
              ),
            ),
            const SizedBox(height: 8),
            Text(
              album.name,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: AppFonts.jostStyle(
                fontWeight: FontWeight.normal,
                fontSize: 13,
              ),
            ),
            Text(
              album.artist ?? l10n.unknownArtist,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: AppFonts.jostStyle(color: Colors.grey, fontSize: 11),
            ),
          ],
        ),
      ),
    );
  }
}
