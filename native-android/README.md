# Klang Retro Native Preview

This isolated Kotlin implementation does not delete or replace the existing Flutter project.

The first preview focuses on a native foundation: MediaStore music discovery, song/album/artist views, search, favourites, Media3 background playback, a mini-player, a now-playing screen, and dark/light/AMOLED themes. Minimum Android API is 27 (Android 8.1).

This is not release-complete. Tag editing and the associated Android write-consent path, embedded album artwork, richer sorting and queue controls, and device validation remain on the migration checklist.

Build with JDK 17, Gradle 8.13, and Android SDK platform 37 by running gradle assembleDebug from this folder.

The debug APK uses the temporary application id dev.klangretro.player.nativepreview so it can coexist with the Flutter build. The production application-id and signing plan will be reviewed before any replacement release.
