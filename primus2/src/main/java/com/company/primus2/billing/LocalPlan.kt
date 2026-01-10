package com.company.primus2.billing

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 全画面で参照するための CompositionLocal。
 */
val LocalPlan = staticCompositionLocalOf<Plan> {
    error("LocalPlan not provided")
}

/**
 * 画面ツリーに現在プランを配布する Provider。
 * - DataStore の値を購読して自動更新
 * - 画面破棄/再生成にも強い
 */
@Composable
fun PlanProvider(
    context: Context,
    content: @Composable () -> Unit
) {
    val flow = remember(context) { PlanStore.observe(context) }
    val plan by flow.collectAsStateWithLifecycle(initialValue = Plan.FREE)
    CompositionLocalProvider(LocalPlan provides plan) {
        content()
    }
}

/**
 * どこからでも Plan を上書き保存するためのヘルパ。
 * （/licenseの結果反映に使用）
 */
fun rememberPlanController(
    context: Context,
    scope: CoroutineScope
): (Plan) -> Unit = {
    scope.launch { PlanStore.set(context, it) }
}
