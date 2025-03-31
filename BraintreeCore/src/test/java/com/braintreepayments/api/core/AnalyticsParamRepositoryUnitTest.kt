package com.braintreepayments.api.core

import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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

        sut.payPalServerSideAttemptedAppSwitch = true
        sut.merchantEnabledAppSwitch = true
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
    fun `invoking reset resets all of the repository's values`() {
        assertEquals(uuid, sut.sessionId)
        assertEquals(true, sut.payPalServerSideAttemptedAppSwitch)
        assertEquals(true, sut.merchantEnabledAppSwitch)

        sut.reset()

        assertEquals(newUuid, sut.sessionId)
        assertNull(sut.payPalServerSideAttemptedAppSwitch)
        assertNull(sut.merchantEnabledAppSwitch)
    }
}
