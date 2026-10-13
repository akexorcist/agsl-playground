package com.example.agslplayground

import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Opens every sample and checks that its shader compiles in the live editor. */
@RunWith(AndroidJUnit4::class)
class SamplesTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun everySampleOpensAndCompiles() {
        for (sample in ShaderSamples.all) {
            rule.onNode(hasScrollAction()).performScrollToNode(hasText(sample.title))
            rule.onNodeWithText(sample.title).performClick()
            rule.mainClock.advanceTimeBy(1_000)
            rule.waitForIdle()
            rule.onNodeWithText("✓ Compiled").assertExists("Sample did not compile: ${sample.title}")
            rule.onNodeWithContentDescription("Back").performClick()
            rule.waitForIdle()
        }
    }
}
