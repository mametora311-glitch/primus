package com.company.primus2.billing

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * プラン変更を監視して、PAID→start / FREE→stop を自動で呼び分ける軽量ガード。
 *
 * 使い方（例: MainActivity.onCreate）:
 *   private var planEnforceJob: Job? = null
 *   planEnforceJob = PlanEnforcer.start(
 *       context = applicationContext,
 *       scope = lifecycleScope,
 *       onStart = { autoVm.startIfNeeded() },
 *       onStop  = { autoVm.stop() }
 *   )
 *   // onDestroy で planEnforceJob?.cancel() 推奨
 */
object PlanEnforcer {
    fun start(
        context: Context,
        scope: CoroutineScope,
        onStart: () -> Unit,
        onStop: () -> Unit
    ): Job {
        return scope.launch {
            PlanStore.observe(context)
                .map { it.isPaid }
                .distinctUntilChanged()
                .collect { paid ->
                    if (paid) onStart() else onStop()
                }
        }
    }
}
