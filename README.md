# Klang

**Your library. Your sound.**

Klang is a local-first music player built around the music collection already on your device. It combines a searchable library, artwork-aware playback, synced lyrics and an adaptive liquid-glass interface.

## What Klang includes

- Browse music by songs, albums, artists, genres, folders and playlists.
- Read embedded tags and album artwork, edit supported metadata, and recover missing cover art during library enrichment.
- Use embedded lyrics, local `.lrc` files and optional online lyrics providers.
- View synchronized lyrics in the player and in a dedicated lyrics-only Android home-screen widget.
- Manage playback queues, favourites, listening history, audio effects and equalizer settings.
- Keep music playing alongside other apps by default, while retaining a separate active-call playback guard.
- Use responsive liquid-glass navigation and player controls with adaptive rendering quality, plus dark, light and AMOLED themes.
- Install universal or architecture-specific Android builds for ARM64, ARMv7 and x86_64.

## Android support

The Android application targets API 26 and newer. Continuous integration builds the universal and ABI-specific APKs and checks launch behaviour on Android API 26 and API 37 emulator images.

The app has been tested in CI, but actual-device checks are still important. Older GPUs may use a lighter glass renderer automatically to keep navigation responsive.

## Build from source

Klang uses Flutter, Dart, Kotlin and Gradle. With the Flutter SDK and Android toolchain installed:

    flutter pub get
    dart run build_runner build --delete-conflicting-outputs
    flutter analyze
    flutter test
    flutter build apk --release --flavor github

GitHub Actions also applies the narrowly scoped Gradle compatibility patch needed by the pinned `metadata_god` dependency.

## Project identity and credits

Klang has its own product direction, name, visual design and user experience. Parts of the current application code are derived from the open-source Looper Player project. The project retains its GPL-3.0 licensing obligations and the original authors' copyright notices. This attribution is provided as source and legal credit; it does not identify the Klang product.

Klang also uses the MIT-licensed `liquid_glass_widgets` Flutter package by Sebastian Degenaar for adaptive liquid-glass UI components. Third-party package licences and notices are available through the in-app Open Source Licences page.

