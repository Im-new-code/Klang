# Klang

Klang is a lightweight, offline-first Android music player built with Kotlin, Jetpack Compose Material 3, and AndroidX Media3.

## Goals
- Small dependency surface, no account, analytics, or cloud library
- Android API 27+ compatibility
- Material 3 design, responsive layouts, subtle transitions
- Songs, albums, artists, search, favorites, and local playlist names
- Background playback, media session/notification, audio focus, headset-disconnect pause
- Song metadata editing where Android storage permissions allow writing
- Shuffle, repeat, sorting, and settings

## Build
Use Android Studio with JDK 17 and Android SDK 35, or GitHub Actions. Debug builds use the .debug application ID suffix.

## Known limitations
- Playlist creation currently saves playlist names only; adding/removing individual songs is a follow-up.
- Metadata editing uses MediaStore write approval on Android 11+; Android 10 and older need device validation.
- Equalizer settings are explanatory placeholders; no DSP dependency is bundled.
- Album art currently uses lightweight generated tiles instead of decoding artwork.
- Verify playback and permissions on real devices, especially Android 8.1 / API 27.
