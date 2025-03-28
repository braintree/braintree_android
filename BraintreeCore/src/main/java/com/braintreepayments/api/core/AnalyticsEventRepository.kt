package com.braintreepayments.api.core

/**
 * This class is responsible for holding analytic events in memory that will be batched with other events.
 */
internal class AnalyticsEventRepository {

    private val events: MutableList<AnalyticsEvent> = mutableListOf()

    /**
     * Adds an event to the internal event buffer.
     */
    fun addEvent(event: AnalyticsEvent) {
        events.add(event)
    }

    /**
     * Flushes the current event buffer and returns the accumulated events.
     *
     * This function atomically clears the internal `events` list and returns a new list containing all the events that
     * were previously in the buffer.
     *
     * @return A new list containing all events that were previously in the buffer.
     */
    fun flushAndReturnEvents(): List<AnalyticsEvent> {
        synchronized(events) {
            val eventsToReturn = events.toList()
            events.clear()
            return eventsToReturn
        }
    }

    companion object {

        /**
         * Singleton instance of the AnalyticsParamRepository.
         */
        val instance: AnalyticsEventRepository by lazy { AnalyticsEventRepository() }
    }
}
