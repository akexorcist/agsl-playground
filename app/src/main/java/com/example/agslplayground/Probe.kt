@file:Suppress("unused")

package com.example.agslplayground

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis as probeAnimationCoreMillis
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos as probeAnimationCoreNanos
import androidx.compose.ui.platform.withInfiniteAnimationFrameMillis as probeUiMillis

suspend fun probe() {
    probeAnimationCoreNanos { }
    probeAnimationCoreMillis { }
    probeUiMillis { }
}
