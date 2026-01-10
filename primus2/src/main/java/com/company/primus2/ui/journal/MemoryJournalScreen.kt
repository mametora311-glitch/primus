package com.company.primus2.ui.journal

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.company.primus2.memory.db.entities.BeliefEntity
import com.company.primus2.ui.state.SessionUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryJournalScreen(
    beliefs: List<BeliefEntity>,
    goals: SessionUiState,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Memory Journal") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text("Beliefs (学習したこと)", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
            }
            if (beliefs.isEmpty()) {
                item { Text("まだ何も学習していません。") }
            } else {
                items(beliefs) { belief ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        ListItem(
                            headlineContent = { Text(belief.key) },
                            supportingContent = { Text(belief.value) }
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
                Text("Goals (目標)", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
            }
            if (goals.goals.isEmpty()) {
                item { Text("まだ目標はありません。") }
            } else {
                items(goals.goals) { goal ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        ListItem(
                            headlineContent = { Text(goal.title) },
                          //supportingContent = { Text("優先度: ${goal.priority} | 状態: ${goal.status}") }
                        )
                    }
                }
            }
        }
    }
}