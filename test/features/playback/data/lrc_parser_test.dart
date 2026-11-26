import 'package:flutter_test/flutter_test.dart';
import 'package:looper_player/features/playback/data/lrc_parser.dart';

void main() {
  group('LrcParser', () {
    test('parses line timestamps and distributes plain-text word timing', () {
      final lines = LrcParser.parse(
        '[00:01.00]first line\n[00:03.50]second line',
      );

      expect(lines, hasLength(2));
      expect(lines[0].timestamp, const Duration(seconds: 1));
      expect(lines[0].text, 'first line');
      expect(lines[0].words, hasLength(2));
      expect(lines[0].words[0].timestamp, const Duration(seconds: 1));
      expect(lines[0].words[1].timestamp, const Duration(milliseconds: 2250));
      expect(lines[1].timestamp, const Duration(milliseconds: 3500));
    });

    test('parses enhanced LRC word timestamps', () {
      final lines = LrcParser.parse(
        '[00:01.00]<00:01.00>Hello <00:01.50>world',
      );

      expect(lines, hasLength(1));
      expect(lines.single.text, 'Hello world');
      expect(lines.single.words, hasLength(2));
      expect(lines.single.words[0].timestamp, const Duration(seconds: 1));
      expect(
        lines.single.words[1].timestamp,
        const Duration(milliseconds: 1500),
      );
      expect(
        lines.single.words[0].duration,
        const Duration(milliseconds: 500),
      );
      expect(
        lines.single.words[1].duration,
        const Duration(milliseconds: 4500),
      );
    });

    test('strips the internal lyrics source marker', () {
      expect(
        LrcParser.stripSourceTag('[source:embedded]\nActual lyric'),
        'Actual lyric',
      );
      expect(LrcParser.stripSourceTag(null), isNull);
    });

    test('returns no lines for plain text without timestamps', () {
      expect(LrcParser.parse('Plain lyrics only'), isEmpty);
    });
  });
}
