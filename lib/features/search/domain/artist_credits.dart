import 'package:looper_player/features/library/domain/models/models.dart';

// Separators between credited artists in a tag: "A; B", "A / B", "A & B",
// "A, B", "A feat. B", "A ft B", "A featuring B", "A and B", "A with B",
// "A x B". The word forms need whitespace around them so names that merely
// contain the letters (Brandy, Daft Punk, Feather) stay whole.
final _creditSeparator = RegExp(
  r'\s*[;/\\&,]\s*|\s+(?:feat\.?|ft\.?|featuring|and|with|x)\s+',
  caseSensitive: false,
);

// Guests named in a title: "Song (feat. A & B)", "Song [ft. A]",
// "Song (with A)", or an unbracketed trailing "Song feat. A".
final _titleGuests = RegExp(
  r'[(\[]\s*(?:feat\.?|ft\.?|featuring|with)\s+([^)\]]+)[)\]]'
  r'|\s(?:feat\.?|ft\.?|featuring)\s+(.+)$',
  caseSensitive: false,
);

List<String> _splitCredits(String raw) => raw
    .split(_creditSeparator)
    .map((name) => name.trim())
    .where((name) => name.isNotEmpty)
    .toList();

/// Every artist [song] credits: the artist tag split into its individual
/// names, plus any guests named in the title.
List<String> creditedArtists(Song song) {
  final artist = song.artist?.trim() ?? '';
  return [
    if (artist.isNotEmpty) ...[artist, ..._splitCredits(artist)],
    for (final match in _titleGuests.allMatches(song.title))
      ..._splitCredits(match.group(1) ?? match.group(2) ?? ''),
  ];
}

/// Whether [artistName] is credited on [song], as the main artist, a
/// co-artist or a featured guest. Matches whole names only, so "Drake" does
/// not claim a song by "Drakeo the Ruler".
bool songCreditsArtist(Song song, String artistName) {
  final wanted = artistName.trim().toLowerCase();
  if (wanted.isEmpty) return false;
  return creditedArtists(song).any((name) => name.toLowerCase() == wanted);
}
