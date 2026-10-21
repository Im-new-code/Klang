import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:looper_player/core/services/storage/db_service.dart';
import 'package:looper_player/features/settings/presentation/providers/settings_notifier.dart';
import 'package:looper_player/l10n/app_localizations.dart';

/// Whether song subtitles add the album ("Artist • Album"); off by default.
bool watchShowAlbum(WidgetRef ref) =>
    ref.watch(settingsProvider.select((s) => s.showAlbumNextToArtist));

/// Quick Picks add the album only when their own toggle is on as well.
bool watchShowAlbumInQuickPicks(WidgetRef ref) => ref.watch(
  settingsProvider.select(
    (s) => s.showAlbumNextToArtist && s.showAlbumInQuickPicks,
  ),
);

/// The artist, or "Artist • Album" when [showAlbum] is on.
String artistAlbumLabel(
  Song song,
  AppLocalizations l10n, {
  required bool showAlbum,
}) {
  final artist = song.artist?.trim();
  final artistLabel = artist == null || artist.isEmpty
      ? l10n.unknownArtist
      : artist;
  if (!showAlbum) return artistLabel;
  final album = song.album?.trim();
  return '$artistLabel • '
      '${album == null || album.isEmpty ? l10n.unknownAlbum : album}';
}
