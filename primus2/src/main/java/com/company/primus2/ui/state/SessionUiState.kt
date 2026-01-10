package com.company.primus2.ui.state

import com.company.primus2.core_ai.model.DispositionState
import com.company.primus2.core_ai.model.EmotionState
import com.company.primus2.memory.db.entities.BeliefEntity
import com.company.primus2.memory.db.entities.GoalEntity
import com.company.primus2.memory.db.entities.MessageEntity
import com.company.primus2.memory.db.entities.SessionEntity

data class AiState(
    val disposition: DispositionState,
    val emotion: EmotionState
)

data class VmStatus(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isThinking: Boolean = false,
    val isSynthesizing: Boolean = false,
    // ▼ 追加
    val isMuted: Boolean = false,
    val showText: Boolean = true,
    val voiceId: Int = 1
)

data class SessionUiState(
    val sessions: List<SessionEntity> = emptyList(),
    val currentSessionId: Long? = null,
    val messages: List<MessageEntity> = emptyList(),
    val aiState: AiState? = null,
    val status: VmStatus = VmStatus(),
    val beliefs: List<BeliefEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList()
)
