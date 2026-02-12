package com.braintreepayments.api.core

import com.braintreepayments.api.paypal.PayPalRecurringBillingPlanType
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
    private val expectedFundingSource = "funding-source"
    private val expectedBillingPlanType = PayPalRecurringBillingPlanType.RECURRING.name

    @Before
    fun setUp() {
        uuidHelper = mockk()
        every { uuidHelper.formattedUUID } returnsMany listOf(uuid, newUuid)
        sut = AnalyticsParamRepository(uuidHelper)

        sut.didPayPalServerAttemptAppSwitch = true
        sut.didSdkAttemptAppSwitch = true
        sut.didEnablePayPalAppSwitch = true
        sut.fundingSource = expectedFundingSource
        sut.isPurchase = true
        sut.isBillingAgreement = true
        sut.recurringBillingPlanType = expectedBillingPlanType
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
    fun `invoking reset resets all of the repository's values except sessionId`() {
        assertEquals(uuid, sut.sessionId)
        assertEquals(true, sut.didPayPalServerAttemptAppSwitch)
        assertEquals(true, sut.didEnablePayPalAppSwitch)
        assertEquals(true, sut.didSdkAttemptAppSwitch)
        assertEquals(expectedFundingSource, sut.fundingSource)
        assertEquals(true, sut.isPurchase)
        assertEquals(true, sut.isBillingAgreement)
        assertEquals(expectedBillingPlanType, sut.recurringBillingPlanType)

        sut.reset()

        assertEquals(uuid, sut.sessionId)
        assertNull(sut.didPayPalServerAttemptAppSwitch)
        assertNull(sut.didEnablePayPalAppSwitch)
        assertNull(sut.didSdkAttemptAppSwitch)
        assertNull(sut.fundingSource)
        assertNull(sut.isPurchase)
        assertNull(sut.isBillingAgreement)
        assertNull(sut.recurringBillingPlanType)
    }
}
