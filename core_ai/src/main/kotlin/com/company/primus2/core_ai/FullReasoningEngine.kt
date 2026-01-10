package com.company.primus2.core_ai

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FullReasoningEngine : AiEngine {
    override fun generateStages(
        turn: ChatTurn,
        config: EngineConfig
    ): Flow<EngineOutput> = flow {
        emit(EngineOutput(Stage.PURPOSE,     StageFormatter.purpose(turn.userText)))
        delay(120)
        emit(EngineOutput(Stage.INPUTS,      StageFormatter.inputs(turn.context)))
        delay(120)
        emit(EngineOutput(Stage.CONSTRAINTS, StageFormatter.constraints()))
        delay(120)
        emit(EngineOutput(Stage.CONFIRM,     StageFormatter.confirm()))
    }
}
