# AGSL Playground

A small Android app for learning AGSL (Android Graphics Shading Language) with a
live code editor. Requires Android 13+ (API 33), where `RuntimeShader` was added.

## What's inside

- **8 samples**, from a first gradient to noise, distance fields, touch ripples,
  and effects applied to real Compose UI through `RenderEffect`.
- **Live editor**: change the code and the preview recompiles ~0.3 s after you stop
  typing. Compiler errors appear in the bar under the code; the last working
  shader keeps running. **Reset** restores the original sample.
- **Tablet layout**: in landscape, code and preview sit side by side.

Every shader can use these uniforms, set by the app each frame:

| Uniform       | Type     | Meaning                                  |
|---------------|----------|------------------------------------------|
| `iResolution` | `float2` | Size of the drawing area in pixels       |
| `iTime`       | `float`  | Seconds since the preview started        |
| `iTouch`      | `float2` | Last touch position in pixels            |
| `phase`       | `float`  | Looping 0.0 → 1.0, restarts every 2 s    |
| `colorA` / `colorB` | `half4` | Purple `#683DB8` / blue `#4052F2` (declare with `layout(color)`) |

Effect samples also declare `uniform shader content;` — the UI underneath —
and read it with `content.eval(coord)`.

## Where to look in the code

- `ShaderSamples.kt` — the AGSL sources (commented for learning).
- `ShaderViews.kt` — how AGSL is used from Kotlin:
  - `RuntimeShader(source)` compiles; it throws with the compiler log on error.
  - `setFloatUniform(...)` passes values in.
  - `ShaderCanvas`: draws the shader with `ShaderBrush` on a `Canvas`.
  - `ShaderEffectBox`: `RenderEffect.createRuntimeShaderEffect(shader, "content")`
    on a `graphicsLayer`.

## Building the APK (no computer needed)

1. Create a new GitHub repository and upload these files (keep the folder structure,
   including `.github/` and `debug.keystore`).
2. GitHub Actions builds the app automatically on every push (Actions tab).
3. When it finishes, open the repo's latest release on your tablet and
   tap `agsl-playground.apk` — or add the repo URL in Obtainium for automatic updates.
4. Allow your browser / My Files to install unknown apps when Android asks.

The included `debug.keystore` signs every build the same way, so new builds
install over the old one. It's a public debug key — fine for learning, never for
a Play Store app.

## Building with Android Studio

Open the folder in Android Studio, connect the tablet with USB debugging on,
and press Run.

## AGSL tips

- `main` receives pixel coordinates (not 0..1 like GLSL's `gl_FragCoord`-based UVs);
  divide by `iResolution` yourself.
- Return `half4` (RGBA, premultiplied alpha).
- Loops need constant bounds: `for (int i = 0; i < 5; i++)`.
- Use float literals (`1.0`, not `1`) in float math.
- Porting from Shadertoy: `vec2/vec3/vec4` → `float2/float3/float4`,
  `fragColor = ...` → `return ...`, `texture(iChannel0, uv)` → `content.eval(px)`.
