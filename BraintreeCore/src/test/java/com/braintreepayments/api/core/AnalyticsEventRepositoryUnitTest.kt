package com.braintreepayments.api.core

import com.braintreepayments.api.testutils.Fixtures
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class AnalyticsEventRepositoryUnitTest {

    private lateinit var sut: AnalyticsEventRepository

    private val mockEvent: AnalyticsEvent = mockk()
    private val otherMockEvent: AnalyticsEvent = mockk()

    private val authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY)
    private val sameTokenAuthorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY)
    private val otherAuthorization = Authorization.fromString(Fixtures.PROD_TOKENIZATION_KEY)

    @Before
    fun setUp() {
        sut = AnalyticsEventRepository()
    }

    @Test
    fun `when addEvent is called then flushAndReturnEvents, event is returned in its batch`() {
        sut.addEvent(mockEvent, authorization)

        val result = sut.flushAndReturnEvents()

        assertEquals(1, result.size)
        assertEquals(authorization, result[0].authorization)
        assertEquals(listOf(mockEvent), result[0].events)
    }

    @Test
    fun `when addEvent is called multiple times, all events are returned in the same batch`() {
        sut.addEvent(mockEvent, authorization)
        sut.addEvent(otherMockEvent, authorization)

        val result = sut.flushAndReturnEvents()

        assertEquals(1, result.size)
        assertEquals(listOf(mockEvent, otherMockEvent), result[0].events)
    }

    @Test
    fun `when two Authorization instances have the same token, their events land in one batch`() {
        sut.addEvent(mockEvent, authorization)
        sut.addEvent(otherMockEvent, sameTokenAuthorization)

        val result = sut.flushAndReturnEvents()

        assertEquals(1, result.size)
        assertEquals(listOf(mockEvent, otherMockEvent), result[0].events)
    }

    @Test
    fun `when addEvent is called with a different token, a second batch is created`() {
        sut.addEvent(mockEvent, authorization)
        sut.addEvent(otherMockEvent, otherAuthorization)

        val result = sut.flushAndReturnEvents()

        assertEquals(2, result.size)
        assertEquals(listOf(mockEvent), result[0].events)
        assertEquals(listOf(otherMockEvent), result[1].events)
    }

    @Test
    fun `when flushAndReturnEvents is called, batches are returned in insertion order`() {
        sut.addEvent(mockEvent, otherAuthorization)
        sut.addEvent(otherMockEvent, authorization)

        val result = sut.flushAndReturnEvents()

        assertEquals(otherAuthorization, result[0].authorization)
        assertEquals(authorization, result[1].authorization)
    }

    @Test
    fun `when flushAndReturnEvents is called, the repository is emptied and batches are detached`() {
        sut.addEvent(mockEvent, authorization)
        val batches = sut.flushAndReturnEvents()

        assertEquals(1, batches.size)
        batches[0].events.clear()

        sut.addEvent(otherMockEvent, authorization)
        val result = sut.flushAndReturnEvents()

        assertEquals(1, result.size)
        assertEquals(listOf(otherMockEvent), result[0].events)
        assertTrue(sut.flushAndReturnEvents().isEmpty())
    }

    @Test
    fun `when two separate ClientToken instances of the same token are used, their events land in one batch`() {
        sut.addEvent(mockEvent, Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
        sut.addEvent(otherMockEvent, Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))

        val result = sut.flushAndReturnEvents()

        assertEquals(1, result.size)
        assertEquals(listOf(mockEvent, otherMockEvent), result[0].events)
    }

    @Test
    fun `when a ClientToken and a TokenizationKey are used, events land in two batches`() {
        sut.addEvent(mockEvent, Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
        sut.addEvent(otherMockEvent, authorization)

        assertEquals(2, sut.flushAndReturnEvents().size)
    }

    @Test
    fun `when two InvalidAuthorizations have the same text, events land in one batch`() {
        sut.addEvent(mockEvent, Authorization.fromString("invalid-a"))
        sut.addEvent(otherMockEvent, Authorization.fromString("invalid-a"))

        val result = sut.flushAndReturnEvents()

        assertEquals(1, result.size)
        assertEquals(listOf(mockEvent, otherMockEvent), result[0].events)
    }

    @Test
    fun `when two InvalidAuthorizations have different text, events land in two batches`() {
        sut.addEvent(mockEvent, Authorization.fromString("invalid-a"))
        sut.addEvent(otherMockEvent, Authorization.fromString("invalid-b"))

        assertEquals(2, sut.flushAndReturnEvents().size)
    }
}
