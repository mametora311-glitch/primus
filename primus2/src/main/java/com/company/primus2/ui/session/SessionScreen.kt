package com.company.primus2.ui.session

import android.Manifest
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.company.primus2.billing.LocalPlan
import com.company.primus2.billing.isPaid
import com.company.primus2.memory.db.entities.MessageEntity
import com.company.primus2.ui.state.SessionUiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    state: SessionUiState,
    onSend: (String) -> Unit,
    onErrorShown: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToJournal: () -> Unit,
    // VM と配線するためのパラメータ
    isMuted: Boolean = state.status.isMuted,
    showText: Boolean = state.status.showText,
    voiceId: Int = state.status.voiceId,
    onToggleMute: (() -> Unit)? = null,
    onSetShowText: ((Boolean) -> Unit)? = null,
    onSetVoice: ((Int) -> Unit)? = null
) {
    val isPaid = LocalPlan.current.isPaid
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // エラー表示
    LaunchedEffect(state.status.error) {
        state.status.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            onErrorShown()
        }
    }

    // 新しいメッセージ or Thinking 開始で自動スクロール
    LaunchedEffect(state.messages.size, state.status.isThinking) {
        val extra = if (state.status.isThinking) 1 else 0
        val lastIndex = (state.messages.size + extra).coerceAtLeast(1) - 1
        listState.animateScrollToItem(lastIndex)
    }

    // 標準 ASR（音声入力）
    var isListening by remember { mutableStateOf(false) }
    val recognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }

    // 権限リクエストランチャー
    val askMic = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startSpeechRecognition(
                recognizer = recognizer,
                onStart = { isListening = true },
                onResult = { text -> input = text },
                onError = { code ->
                    Toast.makeText(context, "Speech error: $code", Toast.LENGTH_SHORT).show()
                    isListening = false
                }
            )
        } else {
            Toast.makeText(context, "マイク権限がありません", Toast.LENGTH_SHORT).show()
        }
    }

    var voiceMenu by remember { mutableStateOf(false) }
    val voices = listOf(
        1 to "Voice #1",
        2 to "Voice #2",
        3 to "Voice #3",
        4 to "Voice #4"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AssistantPulse(active = state.status.isThinking || state.status.isSynthesizing)
                        Spacer(Modifier.width(10.dp))
                        Text("Primus")
                    }
                },
                actions = {
                    // テキスト表示 ON/OFF
                    IconButton(onClick = { onSetShowText?.invoke(!showText) }) {
                        if (showText) {
                            Icon(
                                Icons.Filled.Visibility,
                                contentDescription = "Hide Text"
                            )
                        } else {
                            Icon(
                                Icons.Filled.VisibilityOff,
                                contentDescription = "Show Text"
                            )
                        }
                    }
                    // 音声出力 ON/OFF（FREE では無効）
                    IconButton(
                        onClick = { onToggleMute?.invoke() },
                        enabled = isPaid
                    ) {
                        if (isMuted) {
                            Icon(
                                Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = "Unmute"
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Mute"
                            )
                        }
                    }
                    // ボイス選択（FREE では無効）
                    Box {
                        IconButton(
                            onClick = { voiceMenu = true },
                            enabled = isPaid
                        ) {
                            Icon(
                                Icons.Filled.KeyboardVoice,
                                contentDescription = "Select Voice"
                            )
                        }
                        DropdownMenu(
                            expanded = voiceMenu,
                            onDismissRequest = { voiceMenu = false }
                        ) {
                            voices.forEach { (id, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (id == voiceId) "✓ $label" else label
                                        )
                                    },
                                    onClick = {
                                        onSetVoice?.invoke(id)
                                        voiceMenu = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = onNavigateToJournal) {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = "Journal"
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // マイクボタン（FREE では無効）
                    FilledIconButton(
                        enabled = isPaid,
                        onClick = {
                            if (isListening) {
                                try {
                                    recognizer?.stopListening()
                                } catch (_: Throwable) {
                                }
                                isListening = false
                            } else {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                if (granted) {
                                    startSpeechRecognition(
                                        recognizer = recognizer,
                                        onStart = { isListening = true },
                                        onResult = { text -> input = text },
                                        onError = { code ->
                                            Toast.makeText(
                                                context,
                                                "Speech error: $code",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            isListening = false
                                        }
                                    )
                                } else {
                                    askMic.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape
                    ) {
                        if (isListening) {
                            Icon(Icons.Filled.Stop, contentDescription = "Stop")
                        } else {
                            Icon(Icons.Filled.Mic, contentDescription = "Mic")
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    TextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("メッセージを入力…") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onSend = {
                                if (!state.status.isThinking && !state.status.isSynthesizing) {
                                    val msg = input.trim()
                                    if (msg.isNotEmpty()) {
                                        onSend(msg)
                                        input = ""
                                    }
                                }
                            }
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            val msg = input.trim()
                            if (msg.isNotEmpty()) {
                                onSend(msg)
                                input = ""
                            }
                        },
                        enabled = !state.status.isThinking && !state.status.isSynthesizing,
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                state = listState
            ) {
                if (showText) {
                    items(state.messages, key = { it.id }) { m ->
                        MessageBubble(
                            message = m,
                            highlightSpeaking = isLastAiAndSpeaking(m, state)
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                } else {
                    item {
                        Text(
                            "音声のみモード",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AssistantPulse(active: Boolean) {
    val t = rememberInfiniteTransition(label = "pulse")
    val scale by t.animateFloat(
        initialValue = 1f,
        targetValue = if (active) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = if (active) 800 else 1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAnim"
    )
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(
                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                else Color.Transparent
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size((10.dp.value * scale).dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun MessageBubble(
    message: MessageEntity,
    highlightSpeaking: Boolean
) {
    val isMine = message.role.equals("USER", ignoreCase = true)
    val bg =
        if (isMine) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant
    val fg =
        if (isMine) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomEnd = if (isMine) 2.dp else 16.dp,
        bottomStart = if (isMine) 16.dp else 2.dp
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = bg)
        ) {
            Text(
                text = message.content,
                color = fg,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        if (highlightSpeaking) {
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Filled.VolumeUp,
                contentDescription = "Synthesizing",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun isLastAiAndSpeaking(
    m: MessageEntity,
    state: SessionUiState
): Boolean {
    val last = state.messages.lastOrNull() ?: return false
    return last === m &&
            m.role.equals("AI", ignoreCase = true) &&
            state.status.isSynthesizing
}

private fun startSpeechRecognition(
    recognizer: SpeechRecognizer?,
    onStart: () -> Unit,
    onResult: (String) -> Unit,
    onError: (Int) -> Unit
) {
    if (recognizer == null) {
        onError(SpeechRecognizer.ERROR_CLIENT)
    } else {
        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onStart()
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                onError(error)
            }

            override fun onResults(results: Bundle) {
                val t =
                    results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!t.isNullOrBlank()) {
                    onResult(t)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        recognizer.startListening(intent)
    }
}
