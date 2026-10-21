# Klang

Klang is a lightweight, offline-first Android music player built with Kotlin, Jetpack Compose Material 3, and AndroidX Media3.

## Current features
- Android MediaStore library scanning, with a manual rescan action and refresh when the app returns after a longer pause
- Songs, albums, artists, search across song metadata and folders, liked songs, and title/artist/duration sorting
- Cached album artwork rendered in song rows, album/artist lists, the mini-player, and the now-playing screen
- Background playback through a MediaSessionService, system media controls, seek/progress display, shuffle, repeat, and next/previous
- Theme choices: System, Light, Dark, and AMOLED
- Accent choices: Purple, Blue, Green, and Orange
- Compact song-list layout option
- In-app media volume slider and custom overlay for hardware volume keys while Klang is foregrounded
- Song metadata editor where Android grants write access
- API 27 minimum to include Android 8.1 devices

## Build
Use Android Studio with JDK 17 and Android SDK 35, or run the Android CI workflow. Debug builds use the `.debug` application ID suffix. GitHub Actions uploads a debug APK artifact after a successful build.

## Known limitations
- Playlist creation still stores playlist names only; adding/removing individual songs from playlists remains future work.
- Audio effects/equalizer are not implemented; Klang keeps playback lightweight and uses Android's standard audio path.
- Metadata editing changes MediaStore fields where permitted; this is not a general embedded ID3/tag editor.
- Notification artwork and hardware volume-key interception still require validation on physical devices and OEM Android builds, especially Android 8.1.
