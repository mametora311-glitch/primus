package com.company.primus2.autonomy.goal

import android.util.Log
import com.company.primus2.memory.db.GoalStatus
import com.company.primus2.memory.db.entities.GoalEntity
import com.company.primus2.repository.PrimusRepository

/**
 * 信念(Beliefs)を元に、自律的に目標(Goals)を設定するエンジン。
 */
class GoalEngine(private val repository: PrimusRepository) {

    /**
     * 現在の信念を評価し、新しい目標を作成すべきか判断する。
     */
    suspend fun evaluateAndCreateGoals() {
        Log.i("Primus/GoalEngine", "目標評価プロセスを開始します...")

        val beliefs = repository.getAllBeliefs()
        val existingGoals = repository.getAllGoals()

        val userLikes = beliefs.filter { it.key.startsWith("user_like") }
        userLikes.forEach { likeBelief ->
            val topic = likeBelief.value
            val goalExists = existingGoals.any { it.title.contains(topic) }

            if (!goalExists) {
                Log.i("Primus/GoalEngine", "新しい目標を発見: ユーザーは「$topic」が好きです。")
                val newGoal = GoalEntity(
                    title = "ユーザーの好きな「$topic」に関する面白い話題を提供する",
                    priority = 5,
                    status = GoalStatus.TODO, // ▼▼▼ あなたの定義に合わせて修正 ▼▼▼
                    dueAt = null
                )
                repository.insertGoal(newGoal)
                Log.i("Primus/GoalEngine", "新しい目標を作成しました: ${newGoal.title}")
            }
        }

        Log.i("Primus/GoalEngine", "目標評価プロセスが完了しました。")
    }
}