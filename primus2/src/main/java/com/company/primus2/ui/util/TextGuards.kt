package com.company.primus2.ui.util

import com.company.primus2.core_ai.excore.ExcoreFilter

object TextGuards {

    /** 送信用正規化（制御系・連続空行の縮約）＋ Excore ガード */
    fun normalize(raw: String): String {
        val notCtl = raw.filter { it == '\n' || it >= ' ' }
        val squashed = notCtl
            .replace(Regex("[\\u0000-\\u001F\\u007F]"), "")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
        return ExcoreFilter.guardOrNull(squashed) ?: ""
    }

    /** エコーを避ける簡易要約（既存処理を軽量維持） */
    fun summarizeWithoutEcho(text: String): String {
        if (text.isBlank()) return ""
        val lines = text.split('\n').map { it.trim() }.filter { it.isNotBlank() }
        val uniq = LinkedHashSet<String>()
        for (l in lines.asReversed()) {
            val v = l.replace(Regex("\\s+"), " ")
            if (v.length in 6..160) uniq.add(v)
            if (uniq.size >= 20) break
        }
        val out = uniq.joinToString(" / ")
        return if (out.length > 600) out.take(600) + "…" else out
    }
}
