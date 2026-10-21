import 'package:looper_player/core/services/storage/local_json_store.dart';

/// Decides when to show the monthly "Support us on Ko-fi" snackbar.
///
/// Counts the distinct days the app was opened in the current calendar month.
/// Once that reaches [minDays], the prompt is due - at most once per month.
/// The counter starts over each month. Kept in LocalJsonStore rather than
/// AppSettings so it never ends up in settings backups.
class SupportPrompt {
  SupportPrompt._();

  static const int minDays = 10;
  static const String _storeKey = 'support_prompt';

  static String _monthKey(DateTime d) =>
      '${d.year}-${d.month.toString().padLeft(2, '0')}';

  static Future<Map<String, dynamic>> _read() async {
    final raw = await LocalJsonStore.read(_storeKey);
    return raw is Map<String, dynamic> ? raw : <String, dynamic>{};
  }

  /// Records today as a day of use and returns whether the prompt is due.
  static Future<bool> recordUseAndCheck() async {
    final now = DateTime.now();
    final month = _monthKey(now);
    final data = await _read();

    final days = data['month'] == month && data['days'] is List
        ? Set<int>.from(data['days'] as List)
        : <int>{};
    days.add(now.day);
    data['month'] = month;
    data['days'] = days.toList()..sort();
    await LocalJsonStore.write(_storeKey, data);

    return days.length >= minDays && data['shownMonth'] != month;
  }

  /// Call once the snackbar has actually been shown this month.
  static Future<void> markShown() async {
    final data = await _read();
    data['shownMonth'] = _monthKey(DateTime.now());
    await LocalJsonStore.write(_storeKey, data);
  }
}
