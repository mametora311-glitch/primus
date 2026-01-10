package com.company.primus2.autonomy

import com.company.primus2.autonomy.search.AutonomyBudget


data class Plan(
    val action: Action,
    val cost: Int = 1,
    val reason: String = ""
)

/**
 * 最小Planner: 予算に合わせて行動を選択（MVP）
 */
interface Planner {
    suspend fun next(): Plan
}

/** 単純なルールベース（MVP） */
class SimplePlanner(
    private val budget: AutonomyBudget
) : Planner {
    override suspend fun next(): Plan {
        // 予算が厳しければ NOOP、余裕があれば REMIND を返すだけのMVP
        return if (!budget.check()) {
            Plan(Action.NOOP, cost = 0, reason = "no budget")
        } else {
            Plan(Action.REMIND, cost = 1, reason = "periodic suggestion")
        }
    }
}

sealed interface ExecResult {
    data class Done(val what: String) : ExecResult
    data class Skipped(val reason: String) : ExecResult
}
