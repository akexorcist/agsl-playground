package com.example.agslplayground

import android.app.ActivityManager
import android.app.Application
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

/** Saves the stack trace of an uncaught crash so the next launch can show it. */
object CrashReporter {
    private const val FILE_NAME = "last_crash.txt"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                File(appContext.filesDir, FILE_NAME).writeText(
                    buildString {
                        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                        appendLine("Thread: ${thread.name}")
                        appendLine()
                        append(Log.getStackTraceString(throwable))
                    },
                )
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private val INTERESTING_EXITS = setOf(
        ApplicationExitInfo.REASON_CRASH,
        ApplicationExitInfo.REASON_CRASH_NATIVE,
        ApplicationExitInfo.REASON_ANR,
        ApplicationExitInfo.REASON_SIGNALED,
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE,
        ApplicationExitInfo.REASON_LOW_MEMORY,
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE,
    )

    /**
     * Native crashes (GPU driver, Skia) never reach the Java handler above, but Android
     * remembers how each process ended. Add any new abnormal exit to the saved report.
     */
    fun recordPreviousExits(context: Context) = runCatching {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences("crash_reporter", Context.MODE_PRIVATE)
        val seen = prefs.getLong("last_exit_timestamp", 0L)
        val exits = appContext.getSystemService(ActivityManager::class.java)
            .getHistoricalProcessExitReasons(appContext.packageName, 0, 5)
        val newest = exits.maxOfOrNull { it.timestamp } ?: return@runCatching
        prefs.edit().putLong("last_exit_timestamp", newest).apply()

        val fresh = exits.filter { it.timestamp > seen && it.reason in INTERESTING_EXITS }
        if (fresh.isEmpty()) return@runCatching
        val file = File(appContext.filesDir, FILE_NAME)
        if (!file.exists()) {
            file.writeText("Device: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
        }
        file.appendText("\n== Previous process exits (reported by Android) ==\n" + fresh.joinToString("\n\n") { describe(it) } + "\n")
    }

    private fun describe(info: ApplicationExitInfo): String = buildString {
        appendLine("reason=${reasonName(info.reason)} status=${info.status} importance=${info.importance} process=${info.processName}")
        appendLine("description=${info.description}")
        appendLine("time=${java.util.Date(info.timestamp)} pss=${info.pss}KB rss=${info.rss}KB")
        if (info.reason == ApplicationExitInfo.REASON_CRASH_NATIVE || info.reason == ApplicationExitInfo.REASON_ANR) {
            runCatching {
                info.traceInputStream?.use {
                    appendLine("-- trace (readable text only) --")
                    append(printableStrings(it.readBytes()))
                }
            }
        }
    }

    private fun reasonName(reason: Int) = when (reason) {
        ApplicationExitInfo.REASON_CRASH -> "CRASH (Java)"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "CRASH_NATIVE"
        ApplicationExitInfo.REASON_ANR -> "ANR"
        ApplicationExitInfo.REASON_SIGNALED -> "SIGNALED"
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "INITIALIZATION_FAILURE"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW_MEMORY"
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "EXCESSIVE_RESOURCE_USAGE"
        else -> "reason $reason"
    }

    /** Pulls readable strings (library names, function names, abort message) out of a binary trace. */
    private fun printableStrings(bytes: ByteArray, minLength: Int = 6, limit: Int = 6000): String {
        val out = StringBuilder()
        val run = StringBuilder()
        for (b in bytes) {
            val c = b.toInt() and 0xFF
            if (c in 32..126) {
                run.append(c.toChar())
            } else {
                if (run.length >= minLength) out.appendLine(run)
                run.clear()
            }
            if (out.length > limit) break
        }
        if (run.length >= minLength) out.appendLine(run)
        return out.toString().take(limit)
    }

    fun read(context: Context): String? =
        File(context.filesDir, FILE_NAME).takeIf { it.exists() }?.readText()

    fun clear(context: Context) {
        File(context.filesDir, FILE_NAME).delete()
    }
}

class PlaygroundApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this)
        CrashReporter.recordPreviousExits(this)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrashReportScreen(report: String, onContinue: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    Scaffold(topBar = { TopAppBar(title = { Text("The app crashed last time") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { clipboard.setText(AnnotatedString(report)) }) { Text("Copy") }
                OutlinedButton(onClick = onContinue) { Text("Continue to app") }
            }
            Spacer(Modifier.height(12.dp))
            SelectionContainer(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Text(report, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
    }
}
