package com.company.primus2.reason

import android.util.Log
import com.company.primus2.core_ai.model.EmotionState
import com.company.primus2.core_ai.model.KnowledgeBase
import com.company.primus2.core_ai.model.ReasoningResult
import com.company.primus2.core_ai.model.UserInput
import com.company.primus2.memory.db.dao.MessageDao
import com.company.primus2.memory.db.entities.MessageEntity
import com.company.primus2.memory.select.MemorySelector

/**
 * Reasoner を“そのまま”使いつつ、DAOから候補メモリを取得して MemorySelector をかけ、
 * 選抜結果を Primus ログへ出す統合ハブ。
 * - Reasoner の署名を変更しない（既存呼び出しを壊さない）
 * - DB移行なし / suspend対応
 */
class ReasoningCoordinator(
    private val dao: MessageDao,
    private val selectorWeights: MemorySelector.Weights = MemorySelector.Weights(),
    private val log: (String) -> Unit = { msg -> Log.d("Primus", msg) }
) {
    data class Result(
        val reasoning: ReasoningResult,
        val selectedMemories: List<MessageEntity>
    )

    /**
     * @param sessionId セッションID（0なら横断の最新N件から候補抽出）
     */
    suspend fun run(
        sessionId: Long,
        input: UserInput,
        emotion: EmotionState,
        kb: Map<String, List<KnowledgeBase.Fact>>?
    ): Result {
        val text = input.text

        // 候補収集：セッション限定 or 横断最新
        val candidates: List<MessageEntity> = if (sessionId > 0)
            dao.listBySession(sessionId)
        else
            dao.latest(200)

                // メモリ選抜（ログ出力は MemorySelector 内で Primus タグへ）
        val selected = MemorySelector.select(
            thought = text,
            candidates = candidates,
            k = 5,                        // 上位件数
            threshold = 0.35,             // 最低スコア
            roleWeigher = { role ->       // 役割重み
                when (role.uppercase()) {
                    "SUMMARY" -> 0.25
                    "USER"    -> 0.15
                    "AI"      -> 0.00
                    else      -> 0.00
                }
            },
            weights = selectorWeights,    // 既存の Weights をそのまま
            logger = { line -> log(line) }
        )
        // 既存 Reasoner をそのまま実行
        val reasoner = Reasoner()
        val res = reasoner.reason(input, emotion, kb)

        return Result(res, selected.map { it.entity })
    }
}
