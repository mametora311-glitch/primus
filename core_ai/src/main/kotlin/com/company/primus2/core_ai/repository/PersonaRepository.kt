package com.company.primus2.core_ai.repository

import com.company.primus2.core_ai.model.DispositionState

/**
 * 人格の永続化に関する契約書（インターフェース）。
 */
interface PersonaRepository {
    suspend fun getPersonality(): DispositionState?
    suspend fun savePersonality(persona: DispositionState)
}