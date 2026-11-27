# Klang

Klang is a local-first Android music player focused on a fast music library, clear album artwork, smooth playback controls and synchronized lyrics.

This repository contains the Klang application and its Android build workflows. The Android project supports API 26 and newer and produces universal and ABI-specific APKs.

## Current application capabilities

- Browse locally stored music and organize it by songs, albums, artists and genres.
- Read embedded audio tags and embedded album artwork.
- Load local LRC lyrics and use supported online lyrics providers.
- Edit supported song and album metadata.
- Create playlists, manage the playback queue and restore playback state.
- Use the built-in equalizer and playback controls.
- Add Android home-screen playback widgets.
- Choose accent colors, typography and artwork-aware visual styling.
- Scan device media and user-selected folders, including supported removable storage locations.

Klang is being reworked with a cool-toned liquid-glass interface and its own visual identity. Some planned items, including a dedicated lyrics-only widget and further audio-mixing controls, are still under development and are not claimed as complete by this README.

## Build

The project uses Flutter, Dart, Kotlin and Gradle. GitHub Actions builds release APKs for ARM64, ARMv7, x86_64 and universal Android distributions, and runs static analysis, tests and Android emulator smoke tests.

For local development, run these commands in order:

    flutter pub get
    dart run build_runner build --delete-conflicting-outputs
    flutter analyze
    flutter test
    flutter build apk --release --flavor github

## Upstream and license

Klang is based on the open-source Looper Player project by its original authors and contributors. The project retains the upstream GPL-3.0 licensing obligations; upstream work and its authors must continue to receive appropriate credit. Klang-specific branding, product direction and subsequent changes are maintained in this repository.
