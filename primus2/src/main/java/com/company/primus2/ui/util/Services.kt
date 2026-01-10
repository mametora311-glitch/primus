package com.company.primus2.ui.util

import android.util.Log
import com.company.primus2.PrimusApp
import com.company.primus2.core_ai.PrimusCore
import com.company.primus2.core_ai.model.SpeechLogEntry
import com.company.primus2.memory.db.entities.MessageEntity
import com.company.primus2.net.ProxyClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class Services(private val app: PrimusApp) {

    private val scope: CoroutineScope get() = app.appScope
    private val primusCore = PrimusCore()
    private val proxyClient = ProxyClient.default()

    fun sleepRefine() {
        scope.launch {
            try {
                Log.i("Primus/Sleep", "自己内省プロセスを開始します...")

                val recentMessages = app.db.messageDao().latest(20).reversed()
                if (recentMessages.size < 2) {
                    Log.i("Primus/Sleep", "会話が短すぎるため、自己内省をスキップしました。")
                    return@launch
                }

                // ... (既存のメタ記憶生成ロジックは変更なし) ...
                val speechLogs = mutableListOf<SpeechLogEntry>()
                for (i in 0 until recentMessages.size - 1) {
                    val currentMsg = recentMessages[i]
                    val nextMsg = recentMessages[i + 1]
                    if (currentMsg.role == "USER" && nextMsg.role == "AI") {
                        speechLogs.add(
                            SpeechLogEntry(input = currentMsg.content, output = nextMsg.content, selectedIds = emptyList(), ts = nextMsg.createdAt)
                        )
                    }
                }
                val prompt = primusCore.createConsolidationPrompt(speechLogs)
                if (prompt.isBlank()) return@launch
                val introspectionText = runBlocking { proxyClient.chatOnce(prompt, maxTokens = 200) }
                if (!introspectionText.isNullOrBlank()) {
                    val metaMemoryContent = introspectionText.trim()
                    Log.i("Primus/Sleep", "メタ記憶を生成しました: $metaMemoryContent")
                    val lastSessionId = recentMessages.lastOrNull()?.sessionId
                    if (lastSessionId != null) {
                        val metaMessage = MessageEntity(sessionId = lastSessionId, role = "META", content = metaMemoryContent)
                        app.db.messageDao().insert(metaMessage)
                        Log.i("Primus/Sleep", "メタ記憶をセッション#$lastSessionId に保存しました。")
                    }
                } else {
                    Log.w("Primus/Sleep", "メタ記憶の生成に失敗しました。")
                }

                // ▼▼▼ ここからが今回の追加箇所 ▼▼▼
                // 自己内省の最後に、GoalEngineを呼び出して目標評価を実行する
                app.goalEngine.evaluateAndCreateGoals()
                // ▲▲▲ 追加ここまで ▲▲▲

            } catch (t: Throwable) {
                Log.w("Primus/Sleep", "自己内省プロセスでエラーが発生しました。", t)
            }
        }
    }
}