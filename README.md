# Klang · Project Portal

Klang is an offline-first Android music player built around a watercolor fantasy world. Local music is the primary experience; the scene, motion, volume artifact, lyrics, and visualizers make the library feel like a place rather than a list.

## Current implementation milestone

This repository starts from an empty Android repository. The first increment establishes a native Android foundation that targets Android 8.1 (API 27) and newer, with a MediaStore library scanner, Media3 playback/session plumbing, sensor-driven scene motion, a custom fantasy-scene renderer, a potion-style volume control, a water-reflection shader, and architecture notes for the native projectM visualizer and spatial DSP.

**Scope honesty:** this first increment is a foundation, not the finished premium product. A genuine 3D world-space UI, production-quality painted environment assets, projectM NDK integration and 20 licensed/curated `.milk` presets, HRTF convolution, and billing must be delivered and tested as separate milestones. The initial renderer uses a low-cost stylized shader scene, not a full ray-traced or hand-painted 3D environment. No Studio Ghibli artwork is copied; the art brief is treated as a high-level watercolor fantasy direction.

## Build

Open the repository in Android Studio with JDK 17 and Android SDK Platform 35 installed, or run `gradle assembleDebug` with Gradle 8.9. The GitHub Actions workflow builds a debug APK. Minimum supported Android version is API 27 to match the original target device.

## First milestone modules

- `app/src/main/java/dev/klang/portal/audio/`: MediaStore scanning and Media3 playback/session service.
- `app/src/main/java/dev/klang/portal/world/`: sensor smoothing, procedural fantasy viewport, and potion volume interaction.
- `app/src/main/assets/shaders/`: GLSL scene and puddle reflection shaders.
- `docs/ARCHITECTURE.md`: directory blueprint, boundaries, and staged rollout.
- `docs/SHADERS_AND_WORLD_UI.md`: reflection shader and world-space UI evolution plan.
- `docs/AUDIO_PIPELINE.md`: scanner, metadata, interruptions, gapless playback, lyrics, and spatial DSP plan.
- `app/src/main/cpp/`: reserved NDK/CMake integration point for projectM; projectM is not vendored yet.

## Principles

1. Offline playback must never depend on a network request.
2. Request only scoped media permissions; do not request broad all-files access.
3. Scene rendering is decorative and must not block or interrupt audio.
4. Keep GPU assets small, use GLES 2.0-compatible fallbacks, and release GL resources on context loss.
5. Premium gates are configuration and entitlement checks, never a substitute for Google Play Billing verification.
6. Fail visibly but safely when optional native libraries, imported presets, sensors, or lyric services are unavailable.

## License

Project-specific source licensing will be selected before public release. Third-party libraries retain their own licenses. Do not add projectM or third-party `.milk` presets without verifying their license and attribution requirements.
