package com.company.primus2.ui.session

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.company.primus2.memory.db.entities.MessageEntity
import com.company.primus2.ui.state.SessionUiState

@Composable
fun SessionBar(
    // ▼▼▼ ここを正しい SessionUiState に修正しました ▼▼▼
    state: SessionUiState,
    onCopySummary: (() -> Unit)? = null,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxWidth(), elevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Session #${state.currentSessionId ?: "-"} · total=${state.sessions.size}",
                    style = MaterialTheme.typography.subtitle1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "messages=${state.messages.size}",
                    style = MaterialTheme.typography.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("debug", style = MaterialTheme.typography.caption)
                Switch(checked = false, onCheckedChange = {})
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { onCopySummary?.invoke() }) { Text("コピー") }
        }
    }
}

@Composable
fun ThinkingBubble(visible: Boolean, modifier: Modifier = Modifier) {
    if (!visible) return
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .background(MaterialTheme.colors.onSurface.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("考え中…")
        }
    }
}

@Composable
fun ErrorToastHost(message: String?) {
    val ctx = LocalContext.current
    LaunchedEffect(message) {
        message?.let { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() }
    }
}

/** data.db側の MessageEntity をそのまま受ける版 */
@Composable
fun MemoryChips(
    memories: List<MessageEntity>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        memories.take(8).forEach { m ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f)
            ) { Text("#${m.id}", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) }
            Spacer(Modifier.width(6.dp))
        }
    }
}