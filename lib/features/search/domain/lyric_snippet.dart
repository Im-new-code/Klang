/// The lyric line around the first occurrence of [query] in [lyrics], cut to
/// a short excerpt with "..." where it was trimmed. Null when there is no
/// match.
String? lyricSnippet(String lyrics, String query) {
  final lowerLyrics = lyrics.toLowerCase();
  final lowerQuery = query.toLowerCase();
  final index = lowerLyrics.indexOf(lowerQuery);
  if (index == -1) return null;

  int start = index;
  bool truncatedStart = false;
  while (start > 0 && lyrics[start - 1] != '\n') {
    start--;
    if (index - start > 40) {
      truncatedStart = true;
      break;
    }
  }

  int end = index + query.length;
  bool truncatedEnd = false;
  while (end < lyrics.length && lyrics[end] != '\n') {
    end++;
    if (end - index > 60) {
      truncatedEnd = true;
      break;
    }
  }

  String snippet = lyrics.substring(start, end).trim();
  if (truncatedStart) snippet = '...$snippet';
  if (truncatedEnd) snippet = '$snippet...';
  return snippet;
}
