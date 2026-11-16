package com.braintreepayments.api.paymentactions

import kotlinx.coroutines.CompletableDeferred
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory state for one challenge, shared by [ChallengePresenter] and [ChallengeShellActivity]. A
 * recreated shell finds it again by token; after process death the token is gone.
 *
 * [State.CREATED] → [State.ATTACHED] → [State.ISSUED] → [State.COMPLETED]. The first [complete] wins.
 */
internal class PendingChallenge(
    val transactionId: String,
    val payload: String,
    val registrar: ChallengeRegistrar,
) {

    internal enum class State { CREATED, ATTACHED, ISSUED, COMPLETED }

    private val lock = Any()
    private val attached = CompletableDeferred<Unit>()
    private val outcome = CompletableDeferred<ChallengeResult>()

    var state: State = State.CREATED
        get() = synchronized(lock) { field }
        private set

    /**
     * Called in every shell `onCreate`; the first call completes the handshake.
     *
     * @return `false` if the challenge already ended, so the shell must finish.
     */
    fun attach(): Boolean = synchronized(lock) {
        when (state) {
            State.CREATED -> {
                state = State.ATTACHED
                attached.complete(Unit)
                true
            }
            State.ATTACHED, State.ISSUED -> true
            State.COMPLETED -> false
        }
    }

    /** Records that the challenge was started, so a recreated shell doesn't start it again. */
    fun markIssued() = synchronized(lock) {
        if (state == State.ATTACHED) state = State.ISSUED
    }

    /** Ends the challenge with [result] unless it already ended. Returns `true` if this call did. */
    fun complete(result: ChallengeResult): Boolean = synchronized(lock) {
        if (state == State.COMPLETED) return false
        state = State.COMPLETED
        outcome.complete(result)
    }

    /** Like [complete], but only if the shell never attached. Used when the handshake times out. */
    fun completeIfNotAttached(result: ChallengeResult): Boolean = synchronized(lock) {
        state == State.CREATED && complete(result)
    }

    /** Suspends until the shell first attaches. */
    suspend fun awaitAttached() = attached.await()

    /** Suspends until the challenge ends and returns the outcome. */
    suspend fun awaitOutcome(): ChallengeResult = outcome.await()
}

/** Process-wide registry of [PendingChallenge]s, keyed by the shell's launch token. */
internal object PendingChallenges {

    private val challenges = ConcurrentHashMap<String, PendingChallenge>()

    /** Registers [challenge] and returns its token. */
    fun add(challenge: PendingChallenge): String =
        UUID.randomUUID().toString().also { challenges[it] = challenge }

    fun find(token: String?): PendingChallenge? = token?.let { challenges[it] }

    fun remove(token: String) {
        challenges.remove(token)
    }
}
