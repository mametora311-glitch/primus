package com.company.primus2.memory.select

import com.company.primus2.memory.db.entities.MessageEntity
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object MemorySelector {

    data class Weights(
        val wSim: Double = 0.55,
        val wJaccard: Double = 0.15,
        val wRecency: Double = 0.18,
        val wRole: Double = 0.08,
        val wLenAdj: Double = 0.04,
        val recencyHalfLife: Long = 24L * 60 * 60 * 1000L
    )
    data class Scored<T>(val entity: T, val score: Double)

    private fun tokens(s: String) =
        s.lowercase().replace(Regex("[^\\p{L}\\p{N}]+"), " ").split(Regex("\\s+")).filter { it.length >= 2 }

    private fun cosineLike(a: String, b: String): Double {
        val va = tokens(a).groupingBy { it }.eachCount()
        val vb = tokens(b).groupingBy { it }.eachCount()
        if (va.isEmpty() || vb.isEmpty()) return 0.0
        var dot = 0.0; for ((t, ca) in va) vb[t]?.let { cb -> if (cb > 0) dot += ca * cb }
        val na = sqrt(va.values.sumOf { it * it }.toDouble())
        val nb = sqrt(vb.values.sumOf { it * it }.toDouble())
        return if (na == 0.0 || nb == 0.0) 0.0 else (dot / (na * nb)).coerceIn(0.0, 1.0)
    }

    private fun jaccard(a: Set<String>, b: Set<String>): Double =
        if (a.isEmpty() && b.isEmpty()) 0.0 else a.intersect(b).size.toDouble() / a.union(b).size.toDouble()

    fun select(
        thought: String,
        candidates: List<MessageEntity>,
        k: Int,
        threshold: Double,
        roleWeigher: (String) -> Double,
        weights: Weights = Weights(),
        logger: (String) -> Unit = {}
    ): List<Scored<MessageEntity>> {
        val now = System.currentTimeMillis()
        val tTokens = tokens(thought).toSet()
        val lambda = ln(2.0) / weights.recencyHalfLife

        return candidates.asSequence().map { e ->
            val sim = cosineLike(thought, e.content)
            val jac = jaccard(tTokens, tokens(e.content).toSet())
            val ageMs = max(0L, now - e.createdAt)
            val rec = exp(-lambda * ageMs)
            val r = roleWeigher(e.role).coerceIn(0.0, 1.0)
            val lenAdj = (min(e.content.length, 400).toDouble() / 400.0)
            val score = weights.wSim * sim + weights.wJaccard * jac + weights.wRecency * rec + weights.wRole * r + weights.wLenAdj * lenAdj
            logger("score=${"%.3f".format(score)} id=${e.id} role=${e.role} ageMs=$ageMs sim=${"%.3f".format(sim)} jac=${"%.3f".format(jac)} rec=${"%.3f".format(rec)} r=${"%.2f".format(r)}")
            Scored(e, score)
        }.filter { it.score >= threshold }
            .sortedByDescending { it.score }
            .take(k)
            .toList()
    }
}
