package com.company.primus2.core_ai

import kotlinx.coroutines.flow.Flow

interface AiEngine {
    fun generateStages(
        turn: ChatTurn,
        config: EngineConfig = EngineConfig()
    ): Flow<EngineOutput>
}
