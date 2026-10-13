package com.example.agslplayground

import android.graphics.RuntimeShader
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleListScreen(onOpen: (ShaderSample) -> Unit) {
    val context = LocalContext.current
    var blocked by remember { mutableStateOf(PreviewGuard.blocked(context)) }
    val live = remember { mutableStateListOf<String>() }

    // Switch previews on one at a time. If the app dies, the sample marked "pending" is the
    // culprit: it is blocked on the next launch, so the app can still open.
    LaunchedEffect(blocked) {
        for (sample in ShaderSamples.all) {
            if (sample.id in blocked || sample.id in live) continue
            PreviewGuard.markPending(context, sample.id)
            live += sample.id
            delay(500)
        }
        PreviewGuard.clearPending(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AGSL Playground") },
                actions = {
                    if (blocked.isNotEmpty()) {
                        TextButton(onClick = {
                            PreviewGuard.unblockAll(context)
                            live.clear()
                            blocked = emptySet()
                        }) { Text("Re-enable previews") }
                    }
                },
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 280.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            items(ShaderSamples.all, key = { it.id }) { sample ->
                Card(onClick = { onOpen(sample) }) {
                    Box(
                        Modifier.fillMaxWidth().height(170.dp).background(Color.Black),
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            sample.id in live -> {
                                val shader = remember(sample.id) { compileShader(sample.source).getOrNull() }
                                if (shader != null) {
                                    ShaderView(shader, sample.isEffect, Modifier.fillMaxSize())
                                }
                            }
                            sample.id in blocked -> Text(
                                "Preview crashed the app on this device",
                                color = Color.White,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                    Column(Modifier.padding(12.dp)) {
                        Text(sample.title, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            sample.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaygroundScreen(sample: ShaderSample, onBack: () -> Unit) {
    val context = LocalContext.current
    var blockedHere by remember(sample.id) { mutableStateOf(sample.id in PreviewGuard.blocked(context)) }
    var code by rememberSaveable(sample.id) { mutableStateOf(sample.source) }
    var shader by remember(sample.id) { mutableStateOf<RuntimeShader?>(null) }
    var error by remember(sample.id) { mutableStateOf<String?>(null) }
    var showEditor by rememberSaveable { mutableStateOf(true) }

    // Compile right away the first time, then shortly after you stop typing.
    // On error, keep showing the last good shader. Skipped if this sample crashed the app before.
    LaunchedEffect(sample.id, code, blockedHere) {
        if (blockedHere) return@LaunchedEffect
        if (shader != null) delay(350)
        compileShader(code).fold(
            onSuccess = { shader = it; error = null },
            onFailure = { error = it.message ?: "Compile error" },
        )
    }

    // While the preview is starting, remember which sample it is (see PreviewGuard).
    LaunchedEffect(sample.id, blockedHere) {
        if (blockedHere) return@LaunchedEffect
        PreviewGuard.markPending(context, sample.id)
        delay(2_000)
        PreviewGuard.clearPending(context)
    }
    DisposableEffect(sample.id) { onDispose { PreviewGuard.clearPending(context) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(sample.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { code = sample.source }) { Text("Reset") }
                    TextButton(onClick = { showEditor = !showEditor }) {
                        Text(if (showEditor) "Hide code" else "Show code")
                    }
                },
            )
        },
    ) { padding ->
        BoxWithConstraints(
            Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .fillMaxSize(),
        ) {
            val wide = maxWidth > maxHeight

            val preview: @Composable (Modifier) -> Unit = { m ->
                Box(m.background(Color.Black), contentAlignment = Alignment.Center) {
                    if (blockedHere) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp),
                        ) {
                            Text(
                                "This sample crashed the app on this device.",
                                color = Color.White,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = {
                                PreviewGuard.unblock(context, sample.id)
                                blockedHere = false
                            }) { Text("Run anyway") }
                        }
                    } else {
                        shader?.let { ShaderView(it, sample.isEffect, Modifier.fillMaxSize(), interactive = true) }
                    }
                }
            }
            val editor: @Composable (Modifier) -> Unit = { m ->
                CodeEditor(code = code, onCodeChange = { code = it }, error = error, modifier = m)
            }

            when {
                !showEditor -> preview(Modifier.fillMaxSize())
                // Landscape tablet: code on the left, live preview on the right.
                wide -> Row(Modifier.fillMaxSize()) {
                    editor(Modifier.weight(1f).fillMaxHeight())
                    preview(Modifier.weight(1f).fillMaxHeight())
                }
                // Portrait: preview on top, code below.
                else -> Column(Modifier.fillMaxSize()) {
                    preview(Modifier.weight(1f).fillMaxWidth())
                    editor(Modifier.weight(1f).fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun CodeEditor(code: String, onCodeChange: (String) -> Unit, error: String?, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.background(colors.surfaceContainer)) {
        BasicTextField(
            value = code,
            onValueChange = onCodeChange,
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Ascii,
            ),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
        )
        Text(
            text = error ?: "✓ Compiled",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = if (error != null) colors.onErrorContainer else colors.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .background(if (error != null) colors.errorContainer else colors.surfaceContainerHigh)
                .heightIn(max = 140.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}
