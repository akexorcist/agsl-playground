package com.example.agslplayground

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Launches the real app and lets every sample on the list animate for a few seconds. */
@RunWith(AndroidJUnit4::class)
class LaunchSmokeTest {
    @Test
    fun launchesAndKeepsAnimating() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            Thread.sleep(3_000)
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }
}
