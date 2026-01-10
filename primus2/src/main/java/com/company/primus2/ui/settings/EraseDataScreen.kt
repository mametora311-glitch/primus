package com.company.primus2.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.company.primus2.wipe.LocalWipeUseCase
import kotlinx.coroutines.launch

/**
 * 表示データのみを端末からリセットする画面。
 * サーバ上の記憶データは削除しない。
 */
@Composable
fun EraseDataScreen(
    onNavigateBack: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val useCase = remember { LocalWipeUseCase(ctx.applicationContext) }

    var confirming by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Throwable?>(null) }

    Surface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                "表示データのリセット",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "端末に表示されるデータのみをリセットします。サーバー上の記憶データは削除されません。"
            )
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { confirming = true },
                enabled = !working
            ) {
                Text("表示データをリセットする")
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onNavigateBack,
                enabled = !working
            ) {
                Text("戻る")
            }

            if (working) {
                Spacer(Modifier.height(24.dp))
                RowProgress()
            }
        }
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("表示データをリセットしますか？") },
            text = {
                Text(
                    "この操作は端末内の表示データのみを対象とします。サーバー上の記憶データは削除されません。"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirming = false
                        working = true
                        error = null
                        done = false
                        scope.launch {
                            val result = useCase.invoke()
                            working = false
                            result.onSuccess {
                                done = true
                                Toast.makeText(ctx, "表示データをリセットしました。", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            }.onFailure { t ->
                                error = t
                                Toast.makeText(
                                    ctx,
                                    "リセットに失敗しました: ${t.message ?: "unknown"}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                ) { Text("リセットする") }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirming = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    LaunchedEffect(error, done) {
        // no-op（通知はToastで実施済み）
    }
}

@Composable
private fun RowProgress() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(Modifier.height(8.dp))
        Text("処理中…")
    }
}
