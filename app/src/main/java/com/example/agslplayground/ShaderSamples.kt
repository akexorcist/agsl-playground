package com.example.agslplayground

/**
 * A sample AGSL program.
 *
 * Every sample can use three uniforms that the app updates every frame:
 *   iResolution (float2) – size of the drawing area in pixels
 *   iTime       (float)  – seconds since the preview started
 *   iTouch      (float2) – last touch position in pixels (center by default)
 *   phase       (float)  – looping 0.0 → 1.0 every 2 seconds
 *   colorA/colorB (half4, `layout(color)`) – purple #683DB8 and blue #4052F2
 *
 * "Effect" samples are applied to normal Compose UI through RenderEffect and
 * must declare `uniform shader content;` – the UI underneath, as a child shader.
 */
data class ShaderSample(
    val id: String,
    val title: String,
    val description: String,
    val source: String,
    val isEffect: Boolean = false,
    /** Effect samples only: use the droid image as the UI under the effect instead of the demo card. */
    val useImage: Boolean = false,
)

object ShaderSamples {

    val all: List<ShaderSample> = listOf(
        ShaderSample(
            id = "gradient",
            title = "1. Hello gradient",
            description = "The basics: main(), fragCoord, uniforms and returning a color.",
            source = """
                // Uniforms are inputs set from Kotlin with setFloatUniform().
                uniform float2 iResolution; // canvas size in pixels
                uniform float  iTime;       // seconds since start
                uniform float2 iTouch;      // touch position in pixels

                // main() runs once for EVERY pixel. fragCoord is that pixel's
                // position in pixels (0,0 = top-left). Return its color (RGBA, 0..1).
                half4 main(float2 fragCoord) {
                    // Normalize to 0..1 so the result doesn't depend on screen size.
                    float2 uv = fragCoord / iResolution;

                    // Red follows x, green follows y, blue pulses with time.
                    float blue = 0.5 + 0.5 * sin(iTime);
                    return half4(uv.x, uv.y, blue, 1.0);
                }
            """.trimIndent(),
        ),
        ShaderSample(
            id = "plasma",
            title = "2. Plasma",
            description = "Layering sine waves and a cosine palette for an animated pattern.",
            source = """
                uniform float2 iResolution;
                uniform float  iTime;
                uniform float2 iTouch;

                half4 main(float2 fragCoord) {
                    // Centered coordinates, y from -0.5 to 0.5 (keeps aspect ratio).
                    float2 uv = (fragCoord - 0.5 * iResolution) / iResolution.y;
                    float t = iTime;

                    float v = sin(uv.x * 10.0 + t);
                    v += sin((uv.y * 10.0 + t) * 0.5);
                    v += sin((uv.x * 10.0 + uv.y * 10.0 + t) * 0.5);
                    float2 c = uv * 10.0 + float2(sin(t / 3.0), cos(t / 2.0)) * 5.0;
                    v += sin(sqrt(c.x * c.x + c.y * c.y + 1.0) + t);
                    v *= 0.5;

                    // Cosine palette: shift R, G, B phases to get rainbow colors.
                    float3 col = 0.5 + 0.5 * cos(3.14159 * v + float3(0.0, 2.0, 4.0));
                    return half4(half3(col), 1.0);
                }
            """.trimIndent(),
        ),
        ShaderSample(
            id = "sdf",
            title = "3. Distance field circle",
            description = "Signed distance functions (SDF). Drag on the preview to move the circle.",
            source = """
                uniform float2 iResolution;
                uniform float  iTime;
                uniform float2 iTouch;

                // Distance from p to a circle of radius r: < 0 inside, > 0 outside.
                float sdCircle(float2 p, float r) {
                    return length(p) - r;
                }

                half4 main(float2 fragCoord) {
                    float2 p = (fragCoord - iTouch) / iResolution.y;
                    float d = sdCircle(p, 0.15 + 0.03 * sin(iTime * 3.0));

                    // Blue inside, orange outside.
                    float3 col = mix(float3(0.65, 0.85, 1.0), float3(0.9, 0.6, 0.3), step(0.0, d));
                    col *= 1.0 - exp(-6.0 * abs(d));       // darken near the edge
                    col *= 0.8 + 0.2 * cos(150.0 * d);     // contour rings
                    col = mix(col, float3(1.0), 1.0 - smoothstep(0.0, 0.01, abs(d))); // white outline
                    return half4(half3(col), 1.0);
                }
            """.trimIndent(),
        ),
        ShaderSample(
            id = "ripple",
            title = "4. Touch ripple",
            description = "Waves spreading from your finger, fading with distance.",
            source = """
                uniform float2 iResolution;
                uniform float  iTime;
                uniform float2 iTouch;

                half4 main(float2 fragCoord) {
                    float2 uv = fragCoord / iResolution;
                    float d = distance(fragCoord, iTouch) / iResolution.y;

                    // sin() makes the rings, exp() fades them out with distance.
                    float wave = sin(d * 40.0 - iTime * 6.0) * exp(-d * 4.0);

                    float3 base = mix(float3(0.05, 0.1, 0.3), float3(0.1, 0.6, 0.8), uv.y);
                    float3 col = base + wave * 0.25;
                    return half4(half3(col), 1.0);
                }
            """.trimIndent(),
        ),
        ShaderSample(
            id = "clouds",
            title = "5. Noise clouds",
            description = "Value noise + fractal Brownian motion (FBM), with a fixed-count loop.",
            source = """
                uniform float2 iResolution;
                uniform float  iTime;
                uniform float2 iTouch;

                // Pseudo-random number from a 2D position.
                float hash(float2 p) {
                    return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453);
                }

                // Smoothly interpolated random values on a grid.
                float noise(float2 p) {
                    float2 i = floor(p);
                    float2 f = fract(p);
                    float2 u = f * f * (3.0 - 2.0 * f);
                    return mix(mix(hash(i), hash(i + float2(1.0, 0.0)), u.x),
                               mix(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), u.x), u.y);
                }

                // Add noise at several scales. AGSL loops need constant bounds.
                float fbm(float2 p) {
                    float v = 0.0;
                    float a = 0.5;
                    for (int i = 0; i < 5; i++) {
                        v += a * noise(p);
                        p *= 2.0;
                        a *= 0.5;
                    }
                    return v;
                }

                half4 main(float2 fragCoord) {
                    float2 uv = fragCoord / iResolution.y;
                    float n = fbm(uv * 4.0 + float2(iTime * 0.3, iTime * 0.1));
                    float3 sky = mix(float3(0.2, 0.45, 0.85), float3(0.55, 0.75, 1.0), fragCoord.y / iResolution.y);
                    float3 col = mix(sky, float3(1.0), smoothstep(0.45, 0.8, n));
                    return half4(half3(col), 1.0);
                }
            """.trimIndent(),
        ),
        ShaderSample(
            id = "color_loop",
            title = "6. Color loop (phase)",
            description = "Looping blend between two colors, driven by a 0.0 → 1.0 phase.",
            source = """
                uniform float phase;                   // 0.0 -> 1.0, then restarts
                layout(color) uniform half4 colorA;    // set from Kotlin: purple #683DB8
                layout(color) uniform half4 colorB;    // set from Kotlin: blue   #4052F2

                const float TWO_PI = 6.2831853;        // one full circle, in radians

                half4 main(float2 fragCoord) {
                    // phase 0.0 -> colorA, 0.5 -> colorB, 1.0 -> colorA again
                    float blend = 0.5 - 0.5 * cos(phase * TWO_PI);

                    return half4(mix(colorA.rgb, colorB.rgb, half(blend)), 1.0);
                }
            """.trimIndent(),
        ),
        ShaderSample(
            id = "wave_effect",
            title = "7. Wavy UI (RenderEffect)",
            description = "Distort real Compose UI: the UI comes in as `uniform shader content`.",
            isEffect = true,
            source = """
                // The Compose UI below this effect, available as a child shader.
                uniform shader content;
                uniform float2 iResolution;
                uniform float  iTime;
                uniform float2 iTouch;

                half4 main(float2 fragCoord) {
                    // Instead of computing a color, we sample the UI at a shifted position.
                    float2 p = fragCoord;
                    p.x += sin(fragCoord.y / 30.0 + iTime * 3.0) * 12.0;
                    return content.eval(p);
                }
            """.trimIndent(),
        ),
        ShaderSample(
            id = "chromatic",
            title = "8. Chromatic aberration",
            description = "Sample R, G and B at different offsets. Drag to move the center.",
            isEffect = true,
            source = """
                uniform shader content;
                uniform float2 iResolution;
                uniform float  iTime;
                uniform float2 iTouch;

                half4 main(float2 fragCoord) {
                    float2 dir = (fragCoord - iTouch) / iResolution.y;
                    float2 offset = dir * (20.0 + 10.0 * sin(iTime * 2.0));

                    half4 center = content.eval(fragCoord);
                    half r = content.eval(fragCoord + offset).r;
                    half b = content.eval(fragCoord - offset).b;
                    return half4(r, center.g, b, center.a);
                }
            """.trimIndent(),
        ),
        ShaderSample(
            id = "grayscale",
            title = "9. Grayscale (RenderEffect)",
            description = "Simplest UI effect: no uniforms except the UI itself, as `uniform shader content`.",
            isEffect = true,
            useImage = true,
            source = """
                uniform shader content;

                half4 main(float2 fragCoord) {
                    half4 color = content.eval(fragCoord);
                    half gray = (color.r + color.g + color.b) / 3.0;

                    return half4(half3(gray), color.a);
                }
            """.trimIndent(),
        ),
        ShaderSample(
            id = "grayscale_luminance",
            title = "10. Grayscale (luminance weights)",
            description = "Same effect, but with the Rec. 709 weights: green counts most, blue least.",
            isEffect = true,
            useImage = true,
            source = """
                uniform shader content;

                half4 main(float2 fragCoord) {
                    half4 color = content.eval(fragCoord);
                    half gray = dot(color.rgb, half3(0.2126, 0.7152, 0.0722));  // Rec. 709 luminance

                    return half4(half3(gray), color.a);
                }
            """.trimIndent(),
        ),
        ShaderSample(
            id = "pixelate",
            title = "11. Pixelate (RenderEffect)",
            description = "Sample the UI once per block: every pixel in a block gets the block's center color.",
            isEffect = true,
            useImage = true,
            source = """
                uniform shader content;

                const float BLOCK_SIZE = 24.0;   // size of one block, in pixels

                half4 main(float2 fragCoord) {
                    float2 blockCenter = (floor(fragCoord / BLOCK_SIZE) + 0.5) * BLOCK_SIZE;

                    return content.eval(blockCenter);
                }
            """.trimIndent(),
        ),
    )

    fun byId(id: String): ShaderSample? = all.firstOrNull { it.id == id }
}
