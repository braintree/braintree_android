package com.braintreepayments.api.datacollector

import android.text.TextUtils
import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.testutils.Fixtures
import lib.android.paypal.com.magnessdk.InvalidInputException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4ClassRunner::class)
class MagnesInternalClientTest {

    @Test
    fun getClientMetadataId_withSandboxEnvironment_returnsNonEmptyResult() {
        val configuration =
            Configuration.fromJson(Fixtures.SANDBOX_CONFIGURATION_WITHOUT_GRAPHQL)
        val request = DataCollectorInternalRequest(hasUserLocationConsent = true).apply {
            applicationGuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
        }

        val sut = MagnesInternalClient()
        val result = sut.getClientMetadataId(
            ApplicationProvider.getApplicationContext(),
            configuration,
            request
        )

        assertFalse(TextUtils.isEmpty(result))
    }

    @Test
    fun getClientMetadataId_withLiveEnvironment_returnsNonEmptyResult() {
        val configuration =
            Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
        val request = DataCollectorInternalRequest(hasUserLocationConsent = true).apply {
            applicationGuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
        }

        val sut = MagnesInternalClient()
        val result = sut.getClientMetadataId(
            ApplicationProvider.getApplicationContext(),
            configuration,
            request
        )

        assertFalse(TextUtils.isEmpty(result))
    }

    @Test
    fun getClientMetadataId_withInvalidAppGuid_returnsEmptyString() {
        val configuration =
            Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
        val request = DataCollectorInternalRequest(hasUserLocationConsent = true).apply {
            applicationGuid = "invalid-guid"
        }

        val sut = MagnesInternalClient()
        val result = sut.getClientMetadataId(
            ApplicationProvider.getApplicationContext(),
            configuration,
            request
        )

        assertTrue(result.isEmpty())
    }

    @Test(timeout = 10000)
    fun getClientMetadataIdWithCallback_returnsResultViaCallback() {
        val configuration =
            Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
        val request = DataCollectorInternalRequest(hasUserLocationConsent = true).apply {
            applicationGuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
        }

        val countDownLatch = CountDownLatch(1)
        val sut = MagnesInternalClient()

        sut.getClientMetadataIdWithCallback(
            context = ApplicationProvider.getApplicationContext(),
            configuration = configuration,
            request = request
        ) { correlationId, _ ->
            assertNotNull(correlationId)
            assertFalse(TextUtils.isEmpty(correlationId))
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    fun getClientMetadataIdWithCallback_withSandboxEnvironment_returnsResult() {
        val configuration =
            Configuration.fromJson(Fixtures.SANDBOX_CONFIGURATION_WITHOUT_GRAPHQL)
        val request = DataCollectorInternalRequest(hasUserLocationConsent = true).apply {
            applicationGuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
        }

        val countDownLatch = CountDownLatch(1)
        val sut = MagnesInternalClient()

        sut.getClientMetadataIdWithCallback(
            context = ApplicationProvider.getApplicationContext(),
            configuration = configuration,
            request = request
        ) { correlationId, _ ->
            assertNotNull(correlationId)
            assertFalse(TextUtils.isEmpty(correlationId))
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    fun getClientMetadataIdWithCallback_withInvalidAppGuid_callsBackWithError() {
        val configuration =
            Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
        val request = DataCollectorInternalRequest(hasUserLocationConsent = true).apply {
            applicationGuid = "invalid-guid"
        }

        val countDownLatch = CountDownLatch(1)
        val sut = MagnesInternalClient()

        sut.getClientMetadataIdWithCallback(
            context = ApplicationProvider.getApplicationContext(),
            configuration = configuration,
            request = request
        ) { _, error ->
            assertNotNull(error)
            assertTrue(error is InvalidInputException)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }
}
