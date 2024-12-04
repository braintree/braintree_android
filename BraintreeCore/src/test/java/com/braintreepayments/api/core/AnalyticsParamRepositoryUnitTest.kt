package com.braintreepayments.api.core

import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class AnalyticsParamRepositoryUnitTest {

    private lateinit var uuidHelper: UUIDHelper
    private lateinit var sut: AnalyticsParamRepository

    private val uuid = "test-uuid"
    private val newUuid = "new-uuid"

    @Before
    fun setUp() {
        uuidHelper = mockk()
        sut = AnalyticsParamRepository(uuidHelper)

        every { uuidHelper.formattedUUID } returnsMany listOf(uuid, newUuid)
    }

    @Test
    fun `sessionId getter generates a new value when called for the first time`() {
        assertEquals(uuid, sut.sessionId)
    }

    @Test
    fun `sessionId getter returns the same value when called for the second time`() {
        assertEquals(uuid, sut.sessionId)
        assertEquals(uuid, sut.sessionId)
    }

    @Test
    fun `resetSessionId resets the session ID`() {
        assertEquals(uuid, sut.sessionId)

        sut.resetSessionId()

        assertEquals(newUuid, sut.sessionId)
    }

    @Test
    fun `setSessionId sets the session ID with input value`() {
        sut.setSessionId("override-session-id")
        assertEquals("override-session-id", sut.sessionId)
    }
}
