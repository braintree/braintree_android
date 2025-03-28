package com.braintreepayments.api.core

import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class AnalyticsEventRepositoryUnitTest {

    private lateinit var sut: AnalyticsEventRepository

    private val mockEvent: AnalyticsEvent = mockk()

    @Before
    fun setUp() {
        sut = AnalyticsEventRepository()
    }

    @Test
    fun `when addEvent is called then flushAndReturnEvents, event is returned`() {
        sut.addEvent(mockEvent)

        val result = sut.flushAndReturnEvents()

        assertEquals(listOf(mockEvent), result)
    }

    @Test
    fun `when addEvent is called multiple times, all events are returned`() {
        sut.addEvent(mockEvent)
        sut.addEvent(mockEvent)

        val result = sut.flushAndReturnEvents()

        assertEquals(listOf(mockEvent, mockEvent), result)
    }

    @Test
    fun `when flushAndReturnEvents is called, events are flushed`() {
        sut.addEvent(mockEvent)
        sut.flushAndReturnEvents()

        val result = sut.flushAndReturnEvents()

        assertEquals(emptyList(), result)
    }
}
