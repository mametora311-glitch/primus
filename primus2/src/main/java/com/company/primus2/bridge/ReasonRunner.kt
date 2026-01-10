package com.company.primus2.bridge

import android.util.Log
import com.company.primus2.core_ai.model.EmotionState
import com.company.primus2.core_ai.model.KnowledgeBase
import com.company.primus2.core_ai.model.ReasoningResult
import com.company.primus2.core_ai.model.UserInput
import com.company.primus2.memory.db.dao.MessageDao
import com.company.primus2.memory.db.entities.MessageEntity
import com.company.primus2.memory.select.MemorySelector
import com.company.primus2.reason.Reasoner
import com.company.primus2.summary.AutoSummarizer

/**
 * ViewModel等から 1 関数呼ぶだけで、AIの思考プロセスを実行する司令塔。
 */
object ReasonRunner {

    data class Output(
        val result: ReasoningResult,
        val selected: List<MessageEntity> // UIチップ表示等に使える
    )

    suspend fun runOnce(
        sessionId: Long,
        inputText: String,
        dao: MessageDao,
        emotion: EmotionState,
        kb: Map<String, List<KnowledgeBase.Fact>>? = null,
        doSummarize: Boolean = true,
        log: (String) -> Unit = { msg -> Log.d("Primus", msg) }
    ): Output {
        val input = UserInput(text = inputText)
        val candidates =
            if (sessionId > 0) dao.listBySession(sessionId) else dao.latest(200)

        // MemorySelectorからの戻り値の型が変わったため、.entityで中身を取り出す
        val selected = MemorySelector.select(
            thought = input.text,
            candidates = candidates,
            k = 5,
            threshold = 0.35,
            roleWeigher = { 0.0 }, // ダミーの重み付け
            logger = { line -> log(line) }
        ).map { it.entity }

        val reasoner = Reasoner()
        val res = reasoner.reason(input, emotion, kb)

        if (doSummarize && sessionId > 0) {
            val summarizer = AutoSummarizer(dao)
            summarizer.summarizeIfNeeded(sessionId)
        }
        return Output(res, selected)
    }
}
