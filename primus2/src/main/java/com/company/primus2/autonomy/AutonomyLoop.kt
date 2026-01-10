package com.company.primus2.autonomy

import android.util.Log
import com.company.primus2.autonomy.search.AutonomyBudget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toDuration

class AutonomyLoop(
    private val consent: ConsentGate,
    private val budget: AutonomyBudget,
    private val planner: Planner,
    private val critic: Critic,
    private val scope: CoroutineScope,
    private val interval: Duration = 1.minutes, // デバッグしやすいように1分間隔に変更
    private val cooldown: Duration = 30_000L.toDuration(kotlin.time.DurationUnit.MILLISECONDS), // 30秒に短縮
    private val onAction: (Action) -> Unit // ▼▼▼ 実行したアクションを通知するためのコールバックを追加 ▼▼▼
) {
    private var job: Job? = null
    private var lastFiredAt: Long = 0L

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            Log.i(TAG, "[Loop] start")
            while (isActive) {
                try {
                    tick()
                } catch (t: Throwable) {
                    Log.w(TAG, "[Loop] tick error: ${t.message}")
                }
                delay(interval)
            }
        }
    }

    fun stop() {
        job?.cancel(); job = null
        Log.i(TAG, "[Loop] stop")
    }

    private suspend fun tick() {
        if (!consent.isAllowed()) {
            Log.d(TAG, "[Loop] consent=OFF -> skip"); return
        }
        if (!budget.check()) {
            Log.d(TAG, "[Loop] budget exceeded -> NOOP"); return
        }
        val now = System.currentTimeMillis()
        if (now - lastFiredAt < cooldown.inWholeMilliseconds) {
            Log.d(TAG, "[Loop] cooldown -> skip"); return
        }

        val plan = planner.next()
        Log.i(TAG, "[Planner] plan=${plan.action} cost=${plan.cost} reason='${plan.reason}'")

        val exec = when (plan.action) {
            Action.NOOP -> ExecResult.Skipped("noop")
            Action.REMIND -> ExecResult.Done("remind")
            Action.ASK_CLARIFY -> ExecResult.Done("ask")
        }
        lastFiredAt = now
        budget.consume(plan.cost)

        val r = critic.log(Feedback.AutoTriggered(plan, exec))
        Log.i(TAG, "[Critic] R=${r.value} details='${r.detail}'")

        // ▼▼▼ 計画されたアクションをコールバックで通知 ▼▼▼
        onAction(plan.action)
    }

    companion object {
        private const val TAG = "Primus/Autonomy"
    }
}