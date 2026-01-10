package com.company.primus2

import android.util.Log
import androidx.lifecycle.ViewModel           // ← これが抜けてた
import androidx.lifecycle.viewModelScope
import com.company.primus2.core_ai.model.UserInput
import com.company.primus2.net.ProxyClient
import com.company.primus2.env.ProxyEnv
import kotlinx.coroutines.launch

class PrimusViewModel : ViewModel() {

    // 既定クライアントを1つだけ保持
    private val proxy = ProxyClient.default()

    // 既存：UIからの送信ハンドラ（必要に応じてCoreServices呼び出しはこの中で）
    fun submit(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            // 例）/auth スモーク
            val ok = proxy.authOk()
            Log.i("Primus", "healthz=$ok")

            // 例）/chat 1ショット（失敗時は空文字）
            val resp = proxy.chatOnce(prompt = "テスト、1行で", maxTokens = 32).orEmpty()
            Log.i("Primus", "resp=${resp.take(120)}")

            // TODO: ここで返信UI更新やCoreOut連携を行うなら追記
        }
    }

    // 既存の入力処理を別で持ってるならこの辺に:
    suspend fun process(input: UserInput): String {
        val normalized = input.text.trim().replace(Regex("\\s+"), " ")
        return if (normalized.isBlank()) "（入力が空だよ）" else "了解：「$normalized」"
    }
    // import はそのままでOK（ProxyEnv / Log など既存どおり）

    private fun bootDiagnostics() = viewModelScope.launch {
        Log.i("Primus/Boot", "STEP0 base=${ProxyEnv.BASE_URL}")

        val s1 = proxy.authOk()
        Log.i("Primus/Boot", "STEP1 /auth=$s1")

        val s2 = proxy.fetchConfig() != null
        Log.i("Primus/Boot", "STEP2 /config=$s2")

        // Telemetry: Map<String, String?> を渡す
        val now = System.currentTimeMillis().toString()
        val s3 = proxy.sendTelemetry(
            event = "app-start",
            props = mapOf("ts" to now, "version" to BuildConfig.VERSION_NAME)
        )
        Log.i("Primus/Boot", "STEP3 /telemetry ok=$s3")

        val s4 = proxy.chatOnce("ping", 8) != null
        Log.i("Primus/Boot", "STEP4 /chat=$s4")
    }
}
