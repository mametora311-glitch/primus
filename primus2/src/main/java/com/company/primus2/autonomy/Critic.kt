package com.company.primus2.autonomy

/**
 * 👍/👎/無反応を報酬へ簡易変換するMVP Critic。
 */
data class Reward(val value: Double, val detail: String)

sealed interface Feedback {
    data class AutoTriggered(val plan: Plan, val result: ExecResult) : Feedback
    data class UserThumb(val up: Boolean) : Feedback
    data object NoResponse : Feedback
}

interface Critic {
    fun log(feedback: Feedback): Reward
}

class SimpleCritic(
    private val alpha: Double = 1.0, // up
    private val beta: Double = 1.0,  // down
    private val gamma: Double = 0.3  // no response
) : Critic {
    override fun log(feedback: Feedback): Reward = when (feedback) {
        is Feedback.AutoTriggered -> Reward(0.0, "exec=${feedback.result}")
        is Feedback.UserThumb -> {
            val r = if (feedback.up) alpha else -beta
            Reward(r, if (feedback.up) "thumb_up" else "thumb_down")
        }
        Feedback.NoResponse -> Reward(-gamma, "no_response")
    }
}
