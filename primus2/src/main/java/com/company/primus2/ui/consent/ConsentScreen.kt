package com.company.primus2.ui.consent

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun ConsentScreen(
    onAgree: () -> Unit,
    onDisagree: () -> Unit
) {
    val script = """
        はじめまして。私は自律学習AIアシスタントの Primus です。
        このアプリは、会話の要点を記録・学習し、あなた好みに成長させることができます。
        まずは利用上のご案内と同意の確認からはじめましょう。
    """.trimIndent()

    val context = LocalContext.current

    // 最小構成の TTS。Context を必ず渡す（null 禁止）。
    val tts = remember(context) {
        TextToSpeech(context) { /* 初期化ステータスは今回は無視 */ }
    }

    DisposableEffect(tts) {
        // 失敗してもクラッシュさせない
        try {
            tts.language = Locale.JAPANESE
            tts.speak(
                script,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "primus_intro"
            )
        } catch (_: Exception) {
            // ここで落ちるのは避ける。単に無音で続行。
        }

        onDispose {
            try {
                tts.stop()
                tts.shutdown()
            } catch (_: Exception) {
                // 無視
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("同意事項", style = MaterialTheme.typography.headlineSmall)
        Text("・PrimusはAI。発話が常に正確とは限らない。")
        Text("・応答生成のためテキスト送信。学習内容はサーバーに一時保存→学習後削除。学習済みデータは要望がない限り保持。")
        Text("・表層学習は端末内保存。設定から削除可能。")

        Spacer(Modifier.weight(1f))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onDisagree) { Text("同意しない（無料版へ）") }
            Button(onClick = onAgree) { Text("同意する") }
        }
    }
}
