package com.company.primus2.core_ai.think

import com.company.primus2.core_ai.model.UserInput
import com.company.primus2.core_ai.thought.Thought
import java.util.UUID

class SimpleThoughtEngine : ThoughtEngine {
    override fun think(input: UserInput): Thought {
        // Thoughtクラスのコンストラクタに合わせて引数を修正
        return Thought(
            id = UUID.randomUUID().toString(),
            text = input.text,
            score = 0.5,
            rationale = "N/A in simple engine"
        )
    }
}
