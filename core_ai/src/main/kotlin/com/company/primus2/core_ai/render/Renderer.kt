package com.company.primus2.core_ai.render

import com.company.primus2.core_ai.memory.MemoryItem
import com.company.primus2.core_ai.model.DebugTrace
import com.company.primus2.core_ai.model.DispositionState
import com.company.primus2.core_ai.model.EmotionState
import com.company.primus2.core_ai.model.FinalOutput
import com.company.primus2.core_ai.model.SelectedMemory
import com.company.primus2.core_ai.model.UserInput
import com.company.primus2.core_ai.thought.Thought

interface Renderer {
    fun render(input: UserInput, thought: Thought, selected: SelectedMemory): FinalOutput
}

class DefaultRenderer : Renderer {
    override fun render(input: UserInput, thought: Thought, selected: SelectedMemory): FinalOutput {
        val memoryHint = if (selected.items.isEmpty()) {
            "(参考にした記憶はありません)"
        } else {
            val memoryContents = selected.items.joinToString("\n") { "- 「${it.content}」" }
            "(以下の記憶を参考にしました)\n$memoryContents"
        }

        val responseText = "${memoryHint}\n\n(応答) ${thought.text}"

        // ▼▼▼ FinalOutputに不足していた disposition と emotion を追加 ▼▼▼
        // (この古い部品はAIの内部状態にアクセスできないため、デフォルト値を渡します)
        return FinalOutput(
            text = responseText,
            trace = DebugTrace(selectedIds = selected.items.map(MemoryItem::id)),
            disposition = DispositionState(energy = 0.5f, warmth = 0.5f, empathy = 0.5f),
            emotion = EmotionState(mood = 0.0f, arousal = 0.0f)
        )
    }
}