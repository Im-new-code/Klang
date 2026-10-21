# Klang / Project Portal: directory blueprint

## Repository layout

    Klang/
    ├── .github/workflows/android.yml
    ├── app/
    │   ├── build.gradle.kts
    │   └── src/main/
    │       ├── AndroidManifest.xml
    │       ├── assets/shaders/
    │       │   ├── fullscreen.vert
    │       │   ├── portal_world.frag
    │       │   └── water_reflection.frag
    │       ├── cpp/
    │       │   ├── CMakeLists.txt
    │       │   └── portal_native.cpp
    │       ├── java/dev/klang/portal/
    │       │   ├── MainActivity.kt
    │       │   ├── audio/
    │       │   │   ├── GaplessPlaylistPolicy.kt
    │       │   │   ├── LocalAudioRepository.kt
    │       │   │   ├── LrcLyricsRepository.kt
    │       │   │   ├── NativeVisualizerBridge.kt
    │       │   │   ├── PortalPlaybackService.kt
    │       │   │   └── PremiumFeatures.kt
    │       │   ├── model/Track.kt
    │       │   └── world/
    │       │       ├── FantasyWorldView.kt
    │       │       ├── PortalWorldRenderer.kt
    │       │       └── WorldHudView.kt
    │       └── res/values/{strings,styles}.xml
    ├── docs/
    │   ├── ARCHITECTURE.md
    │   ├── AUDIO_PIPELINE.md
    │   └── SHADERS_AND_WORLD_UI.md
    ├── assets/presets/README.md
    ├── settings.gradle.kts
    ├── build.gradle.kts
    └── gradle.properties

## Module boundaries

- **Audio repository:** a background MediaStore query returns content URIs and catalog metadata. No media file bytes are uploaded.
- **Playback service:** Media3/ExoPlayer owns the player and MediaSession, so playback can survive Activity recreation. Audio focus is delegated to Android; the noisy-route hook pauses on common headset/audio-route disconnection.
- **World rendering:** GLES 2.0, render-on-demand, one fullscreen quad, and a small uniform set. The scene renderer does not decode album art or own audio state.
- **Projected HUD:** Canvas-drawn perspective control planes keep the first build dependency-light. Text and controls are not yet native 3D meshes. The future world-space version should use scene meshes or a Godot SubViewport-on-quad and ray-to-plane input mapping.
- **Lyrics:** optional explicit network request. It must never block local playback or library scanning.
- **Native visualizer:** app/src/main/cpp is the reservation point. projectM source is deliberately not vendored in this increment. Pin the upstream revision, license, ABI support and rendering backend before enabling it.
- **Licensing:** premium defaults are locked. A future BillingClient integration must validate purchase tokens and expose a verified entitlement to feature gates.

## Rendering and memory budgets

- GLES 2.0 is the baseline; no depth buffer is needed for the procedural backdrop.
- Artwork is decoded off the UI thread and downsampled before display.
- The renderer currently creates no heap textures or framebuffers. When reflection rendering is wired, create framebuffer/texture resources in onSurfaceCreated, delete them on context replacement, and check GL_MAX_TEXTURE_SIZE.
- Android 8.1 is the minimum supported version. Permissions split between READ_EXTERNAL_STORAGE on Android 8.1 through 12L and READ_MEDIA_AUDIO on Android 13+.
- The view requests frames on sensor movement and on a one-minute clock tick rather than running an unconditional 60 FPS loop.

## Delivery sequence

1. **Foundation:** Android shell, MediaStore scanner, playback service/session, stylized shader world, sensor smoothing, potion control, reflection shader and CI build.
2. **Audio hardening:** lock-screen controls, focus/noisy-route tests, queue restore, codec matrix, metadata fallback tests and rescan UX.
3. **True world-space UI:** scene meshes or SubViewport projection, ray-plane touch mapping, focus animation and accessibility semantics.
4. **Artwork and atmosphere:** original watercolor/gouache environment assets, day/night transition easing, shadows and parallax tuning against older Adreno GPUs.
5. **Visualizer:** pinned projectM source, JNI bridge and GLES context interoperability; preset import sandboxing and 20 curated presets with audited redistribution rights.
6. **Lyrics:** explicit query UX, LRC parser/time index, synchronized parchment rendering, caching and timeout/error states.
7. **Spatial sound:** profile A/B tests for CPU/battery cost and subtle selectable room responses; never spatialize by default or silently alter lossless output.
8. **Premium:** Play Billing purchase/restore, entitlement validation, recovery paths and store-policy review.
9. **Release qualification:** physical-device tests, GPU/heap profiling, audio interruptions, rotation/process death, accessibility, privacy and licensing review.
