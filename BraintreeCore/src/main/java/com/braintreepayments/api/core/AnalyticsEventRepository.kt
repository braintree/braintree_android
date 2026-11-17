package com.braintreepayments.api.core

/**
 * Accumulated events for a single [Authorization]. Not a data class so no generated
 * toString prints the authorization token.
 */
internal class AnalyticsEventBatch(val authorization: Authorization) {
    val events = mutableListOf<AnalyticsEvent>()
}

/**
 * This class is responsible for holding analytic events in memory that will be batched with other events,
 * partitioned by the authorization of the client that produced them.
 */
internal class AnalyticsEventRepository {

    private val lock = Any()
    private val partitions = LinkedHashMap<Authorization, AnalyticsEventBatch>()

    /**
     * Adds an event to the batch of the given [authorization].
     */
    fun addEvent(event: AnalyticsEvent, authorization: Authorization) {
        synchronized(lock) {
            partitions.getOrPut(authorization) { AnalyticsEventBatch(authorization) }
                .events
                .add(event)
        }
    }

    /**
     * Flushes all batches and returns them.
     *
     * This function atomically clears the internal partitions and returns a list containing one
     * [AnalyticsEventBatch] per authorization, in insertion order.
     *
     * @return A new list of batches that were previously held; the repository is left empty.
     */
    fun flushAndReturnEvents(): List<AnalyticsEventBatch> = synchronized(lock) {
        val batches = partitions.values.toList()
        partitions.clear()
        batches
    }

    companion object {

        /**
         * Singleton instance of the AnalyticsParamRepository.
         */
        val instance: AnalyticsEventRepository by lazy { AnalyticsEventRepository() }
    }
}
