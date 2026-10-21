# GLSL reflection pass and world-space UI plan

## Water reflection shader

The complete fragment shader lives at app/src/main/assets/shaders/water_reflection.frag. It samples a pre-rendered scene texture, mirrors vertical UV coordinates around a configurable waterline, applies two low-cost sine distortions, adds a cool water tint, and fades the reflection toward the puddle boundary.

Integration steps:

1. Render the environment to a texture-backed framebuffer.
2. Draw the puddle quad after the environment pass, binding that color texture as uSceneTexture.
3. Set uWaterLine in the same UV coordinate space as the scene texture and tune uStrength conservatively.
4. Enable alpha blending for the puddle pass.
5. Re-create FBOs and textures after GL context loss; delete previous handles on the GL thread.

The active portal_world.frag uses a procedural water surface and ripple highlights to avoid framebuffer allocation in the foundation build. The reflection shader is ready to wire, but is not yet a live FBO pass.

## Camera parallax

MainActivity reads the rotation-vector sensor when available, converts it to pitch/roll, low-pass filters both axes, clamps motion and sends normalized offsets to PortalWorldRenderer. Without the sensor, the scene remains usable with a static view. Sensor registration is limited to the resumed Activity.

## UI tree in the first increment

    Activity (MainActivity)
    └── FrameLayout
        ├── FantasyWorldView (GLES 2 backdrop)
        │   └── PortalWorldRenderer
        │       ├── procedural sky and soft clouds
        │       ├── layered hills and distant island
        │       ├── time-dependent day/night palette
        │       └── procedural pool and ripple highlights
        └── WorldHudView (painted projected planes)
            ├── local archive / track plaque
            ├── album-art vinyl artifact
            ├── previous / play-pause / next controls
            └── brass potion dial with haptic ticks

The HUD is currently drawn into a Canvas overlay styled as a perspective plane. This keeps the prototype dependency-light, but is not true mesh-based world-space UI yet. For the production scene, promote HUD elements to 3D plane nodes (or a Godot SubViewport-on-quad) and map touch rays to node-local coordinates. Preserve keyboard/switch-access and TalkBack semantics during that port.

## Texture and frame hygiene

- The current renderer creates no GL textures and uses a single full-screen quad.
- Shader compile/link failures leave a dark background and write a log entry.
- Re-create program state in onSurfaceCreated after context loss; never retain old GL handles as valid.
- Future bitmaps uploaded to GL must use bounded texture sizes, avoid duplicate uploads, delete old handles and dispose on the GL thread.
- Render on demand. Do not tie continuous high-rate rendering to audio playback unless the user explicitly enables the premium visualizer.
