package com.braintreepayments.api.datacollector

import android.text.TextUtils
import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.core.Authorization
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.SharedPreferencesHelper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4ClassRunner::class)
class DataCollectorTest {

    private lateinit var configuration: Configuration

    @Before
    fun setUp() {
        configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
        val authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY)
        SharedPreferencesHelper.overrideConfigurationCache(
            ApplicationProvider.getApplicationContext(),
            authorization,
            configuration
        )
    }

    @Test
    fun getClientMetadataId_returnsClientMetadataId() {
        val sut = DataCollector(
            ApplicationProvider.getApplicationContext(),
            Fixtures.TOKENIZATION_KEY
        )
        val clientMetadataId = sut.getClientMetadataId(
            ApplicationProvider.getApplicationContext(),
            configuration,
            true
        )

        assertFalse(TextUtils.isEmpty(clientMetadataId))
    }

    @Test(timeout = 10000)
    fun collectDeviceData_returnsSuccessWithCorrelationId() {
        val countDownLatch = CountDownLatch(1)
        val sut = DataCollector(
            ApplicationProvider.getApplicationContext(),
            Fixtures.TOKENIZATION_KEY
        )
        val request = DataCollectorRequest(hasUserLocationConsent = true)

        sut.collectDeviceData(
            ApplicationProvider.getApplicationContext(),
            request
        ) { result ->
            assertTrue(result is DataCollectorResult.Success)
            val deviceData = (result as DataCollectorResult.Success).deviceData
            val json = JSONObject(deviceData)
            assertFalse(TextUtils.isEmpty(json.getString("correlation_id")))
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    fun collectDeviceData_withRiskCorrelationId_returnsSuccess() {
        val countDownLatch = CountDownLatch(1)
        val sut = DataCollector(
            ApplicationProvider.getApplicationContext(),
            Fixtures.TOKENIZATION_KEY
        )
        val request = DataCollectorRequest(
            hasUserLocationConsent = true,
            riskCorrelationId = "custom-correlation-id"
        )

        sut.collectDeviceData(
            ApplicationProvider.getApplicationContext(),
            request
        ) { result ->
            assertTrue(result is DataCollectorResult.Success)
            val deviceData = (result as DataCollectorResult.Success).deviceData
            val json = JSONObject(deviceData)
            assertFalse(TextUtils.isEmpty(json.getString("correlation_id")))
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    fun collectDeviceData_whenConfigurationFetchFails_returnsFailure() {
        val countDownLatch = CountDownLatch(1)
        val sut = DataCollector(
            ApplicationProvider.getApplicationContext(),
            "sandbox_invalid_key_xxxxxxxx"
        )
        val request = DataCollectorRequest(hasUserLocationConsent = true)

        sut.collectDeviceData(
            ApplicationProvider.getApplicationContext(),
            request
        ) { result ->
            assertTrue(result is DataCollectorResult.Failure)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    fun collectDeviceDataOnSuccess_returnsSuccessWithCorrelationId() {
        val countDownLatch = CountDownLatch(1)
        val sut = DataCollector(
            ApplicationProvider.getApplicationContext(),
            Fixtures.TOKENIZATION_KEY
        )
        val request = DataCollectorRequest(hasUserLocationConsent = true)

        sut.collectDeviceDataOnSuccess(
            ApplicationProvider.getApplicationContext(),
            request
        ) { result ->
            assertTrue(result is DataCollectorResult.Success)
            val deviceData = (result as DataCollectorResult.Success).deviceData
            val json = JSONObject(deviceData)
            assertFalse(TextUtils.isEmpty(json.getString("correlation_id")))
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    fun collectDeviceDataOnSuccess_withRiskCorrelationId_returnsSuccess() {
        val countDownLatch = CountDownLatch(1)
        val sut = DataCollector(
            ApplicationProvider.getApplicationContext(),
            Fixtures.TOKENIZATION_KEY
        )
        val request = DataCollectorRequest(
            hasUserLocationConsent = true,
            riskCorrelationId = "custom-correlation-id"
        )

        sut.collectDeviceDataOnSuccess(
            ApplicationProvider.getApplicationContext(),
            request
        ) { result ->
            assertTrue(result is DataCollectorResult.Success)
            val deviceData = (result as DataCollectorResult.Success).deviceData
            val json = JSONObject(deviceData)
            assertFalse(TextUtils.isEmpty(json.getString("correlation_id")))
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    fun collectDeviceDataOnSuccess_whenConfigurationFetchFails_returnsFailure() {
        val countDownLatch = CountDownLatch(1)
        val sut = DataCollector(
            ApplicationProvider.getApplicationContext(),
            "sandbox_invalid_key_xxxxxxxx"
        )
        val request = DataCollectorRequest(hasUserLocationConsent = true)

        sut.collectDeviceDataOnSuccess(
            ApplicationProvider.getApplicationContext(),
            request
        ) { result ->
            assertTrue(result is DataCollectorResult.Failure)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }
}
