package com.example.agslplayground

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Compiles AGSL source. On failure the exception message is the compiler log. */
fun compileShader(source: String): Result<RuntimeShader> = runCatching { RuntimeShader(source) }

/**
 * Sets the standard uniforms. setFloatUniform() throws if the shader doesn't
 * declare (or doesn't use) a uniform, so each one is set independently.
 */
private fun RuntimeShader.setStandardUniforms(width: Float, height: Float, time: Float, touch: Offset) {
    runCatching { setFloatUniform("iResolution", width, height) }
    runCatching { setFloatUniform("iTime", time) }
    runCatching { setFloatUniform("iTouch", touch.x, touch.y) }
    // Looping phase 0.0 -> 1.0 every 2 seconds (same as a 2000 ms linear tween).
    runCatching { setFloatUniform("phase", (time % 2f) / 2f) }
    // Colors for the color-loop sample; declared in AGSL as `layout(color) uniform half4`.
    runCatching { setColorUniform("colorA", 0xFF683DB8.toInt()) }
    runCatching { setColorUniform("colorB", 0xFF4052F2.toInt()) }
}

/** Seconds since this composable first appeared, updated every frame. */
@Composable
private fun rememberShaderTime(): State<Float> {
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withInfiniteAnimationFrameNanos { it }
        while (true) {
            withInfiniteAnimationFrameNanos { time.floatValue = (it - start) / 1_000_000_000f }
        }
    }
    return time
}

private fun Modifier.trackTouch(enabled: Boolean, onTouch: (Offset) -> Unit): Modifier =
    if (!enabled) this else pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                event.changes.forEach { change ->
                    if (change.pressed) {
                        onTouch(change.position)
                        change.consume()
                    }
                }
            }
        }
    }

/** Draws a full-area AGSL shader (like a Shadertoy canvas). */
@Composable
fun ShaderCanvas(shader: RuntimeShader, modifier: Modifier = Modifier, interactive: Boolean = false) {
    val time by rememberShaderTime()
    var touch by remember { mutableStateOf(Offset.Unspecified) }
    val brush = remember(shader) { ShaderBrush(shader) }

    Canvas(modifier.trackTouch(interactive) { touch = it }) {
        shader.setStandardUniforms(
            width = size.width,
            height = size.height,
            time = time,
            touch = if (touch.isSpecified) touch else center,
        )
        drawRect(brush)
    }
}

/** Applies an AGSL shader as a RenderEffect to regular Compose UI. */
@Composable
fun ShaderEffectBox(shader: RuntimeShader, modifier: Modifier = Modifier, interactive: Boolean = false) {
    val time by rememberShaderTime()
    var touch by remember { mutableStateOf(Offset.Unspecified) }

    Box(
        modifier
            .clipToBounds()
            .trackTouch(interactive) { touch = it }
            .graphicsLayer {
                val w = size.width
                val h = size.height
                shader.setStandardUniforms(
                    width = w,
                    height = h,
                    time = time,
                    touch = if (touch.isSpecified) touch else Offset(w / 2f, h / 2f),
                )
                // "content" must match the name of the `uniform shader` in AGSL.
                renderEffect = runCatching {
                    RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
                }.getOrNull()
            },
    ) {
        DemoContent()
    }
}

/** Ordinary Compose UI used as the input for effect shaders. */
@Composable
private fun DemoContent() {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(listOf(Color(0xFF1E3C72), Color(0xFF2A5298), Color(0xFFE96443))),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Hello AGSL", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(Color(0xFFFFC107), Color(0xFF4CAF50), Color(0xFF03A9F4), Color(0xFFE91E63)).forEach {
                    Box(Modifier.size(36.dp).background(it, CircleShape))
                }
            }
        }
    }
}

/** Renders a sample the right way depending on whether it's an effect. */
@Composable
fun ShaderView(shader: RuntimeShader, isEffect: Boolean, modifier: Modifier = Modifier, interactive: Boolean = false) {
    if (isEffect) {
        ShaderEffectBox(shader, modifier, interactive)
    } else {
        ShaderCanvas(shader, modifier, interactive)
    }
}
