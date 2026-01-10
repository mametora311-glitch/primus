package com.company.primus2.core_ai.think

import com.company.primus2.core_ai.model.UserInput
import com.company.primus2.core_ai.thought.Thought

interface ThoughtEngine {
    fun think(input: UserInput): Thought
}
