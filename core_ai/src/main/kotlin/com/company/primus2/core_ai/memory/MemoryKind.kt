// ========================
// File: core_ai/memory/MemoryKind.kt
// Package: com.company.primus2.core_ai.memory
// ========================
package com.company.primus2.core_ai.memory


/**
 * 記憶の種類を表す列挙。仕様の用語に対応する最小公倍のセット。
 * - LOG: 発話ログなど一次ログ（短期・未精査）
 * - META: 自己内省（Meta-Memory）や行動理由の記録
 * - SHORT_TERM: 時事メモリ（鮮度重視の短期保持）
 * - LONG_TERM: 重要度の高い長期保持（重大事案・核記憶 等）
 * - SEALED: 封印領域（アクセス制限付き）
 */
enum class MemoryKind {
    SHORT_TERM,
    LONG_TERM,
    META,
    SEALED
}
