package com.company.primus2.autonomy

import com.company.primus2.repository.PrimusRepository
import kotlin.time.Duration.Companion.minutes

/**
 * より高度な判断を下すプランナー。
 * 役割：AIの内部状態や対話履歴、そして「目標」を元に、次に取るべき行動を決定する。
 */
class AdvancedPlanner(
    private val repository: PrimusRepository
) : Planner {

    // ユーザーが5分以上沈黙していたら、行動を起こす
    private val idleThreshold = 5.minutes

    override suspend fun next(): Plan {

        // --- ▼▼▼ ここからが最終進化のコード ▼▼▼ ---

        // 1. データベースから、現在進行中の目標をすべて取得する
        val activeGoals = repository.getAllGoals().filter { it.status == com.company.primus2.memory.db.GoalStatus.TODO }

        // 2. 実行すべき目標があれば、それを元に行動計画を立てる
        if (activeGoals.isNotEmpty()) {
            // (簡易的な実装として、最も優先度の高い目標をランダムに1つ選ぶ)
            val selectedGoal = activeGoals.maxByOrNull { it.priority }!!

            // 目標の内容に応じて、LLMに渡すための「行動の種」となるプロンプトを作成
            val actionSeed = when {
                selectedGoal.title.contains("面白い話題") ->
                    "ユーザーが好きな${selectedGoal.title.substringAfter("「").substringBefore("」")}に関する、何か面白い豆知識や最近のニュースを披露して、会話を盛り上げよう。"
                else ->
                    "「${selectedGoal.title}」という目標を達成するために、何かユーザーに働きかけてみよう。"
            }

            // 将来的には、このactionSeedをSelfAgentに渡して、より豊かな自律行動を生成させる
            // 今回は、目標があった、という事実を元に「REMIND」アクションを返す
            return Plan(
                action = Action.REMIND, // 「何かを思い出させる・話しかける」という行動を計画
                cost = 1,
                reason = "Executing goal #${selectedGoal.id}: ${selectedGoal.title}"
            )
        }

        // --- ▲▲▲ 進化コードここまで ▲▲▲ ---


        // 3. 実行すべき目標がない場合は、以前の「ユーザーの沈黙」をチェックするロジックにフォールバックする
        val lastMessage = repository.getLatestMessages(1).firstOrNull()

        if (lastMessage == null) {
            return Plan(Action.NOOP, cost = 0, reason = "no history")
        }

        val now = System.currentTimeMillis()
        val lastMessageTime = lastMessage.createdAt
        val isIdle = (now - lastMessageTime) > idleThreshold.inWholeMilliseconds

        if (lastMessage.role == "AI" && isIdle) {
            return Plan(
                action = Action.ASK_CLARIFY, // 「何か問いかける」という行動を計画
                cost = 1,
                reason = "User has been idle for ${idleThreshold.inWholeMinutes} minutes. Re-engaging."
            )
        }

        // それ以外の場合は何もしない
        return Plan(Action.NOOP, cost = 0, reason = "user is active")
    }
}