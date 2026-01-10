package com.company.primus2.autonomy

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.company.primus2.autonomy.search.AutonomyBudget
import com.company.primus2.billing.LocalPlan
import com.company.primus2.billing.isPaid
import com.company.primus2.consent.ConsentStore
import com.company.primus2.ui.session.AutonomyControllerViewModel
import kotlinx.coroutines.launch

@Composable
fun AutonomyPanel(
    vm: AutonomyControllerViewModel,
    modifier: Modifier = Modifier
) {
    val isPaid = LocalPlan.current.isPaid
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // DataStore バックエンド
    val consentStore = remember { ConsentStore(ctx) }
    val budget = remember { AutonomyBudget(ctx) }

    val consent by consentStore.allowedFlow.collectAsState(initial = false)
    var budgetOk by remember { mutableStateOf<Boolean?>(null) }

    Surface(tonalElevation = 2.dp, modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Autonomy (dev)", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(12.dp))

            Text("Consent")
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = consent,
                onCheckedChange = { on ->
                    scope.launch { consentStore.setAllowed(on) }
                },
                enabled = isPaid
            )

            Spacer(Modifier.width(16.dp))
            Button(onClick = { vm.startIfNeeded() }, enabled = isPaid) { Text("Start") }
            Spacer(Modifier.width(8.dp))
            // Stop は安全のため常時有効（FREEでも停止可能）
            OutlinedButton(onClick = { vm.stop() }) { Text("Stop") }

            Spacer(Modifier.width(16.dp))
            Button(onClick = {
                scope.launch {
                    budgetOk = budget.check()
                }
            }, enabled = isPaid) { Text("Budget?") }
            Spacer(Modifier.width(8.dp))
            Text(
                when (budgetOk) {
                    null -> ""
                    true -> "OK"
                    false -> "NG"
                }
            )
        }
    }
}
