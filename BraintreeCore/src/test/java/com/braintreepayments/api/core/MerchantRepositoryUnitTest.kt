package com.braintreepayments.api.core

import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MerchantRepositoryUnitTest {

    @Test
    fun `authorizationOrNull returns null before the authorization is initialized`() {
        val sut = MerchantRepository()

        assertNull(sut.authorizationOrNull)
    }

    @Test
    fun `authorizationOrNull returns the authorization after it is initialized`() {
        val authorization = Authorization.fromString("sandbox_fjajdkd_integration_merchant_id")
        val sut = MerchantRepository().also { it.authorization = authorization }

        assertSame(authorization, sut.authorizationOrNull)
    }
}
