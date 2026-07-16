package com.braintreepayments.api.datacollector

import android.content.Context
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.Configuration.Companion.fromJson
import com.braintreepayments.api.core.UUIDHelper
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DataCollectorUnitTest {

    private val testDispatcher = StandardTestDispatcher()

    @MockK
    lateinit var context: Context

    @MockK
    lateinit var configuration: Configuration

    @MockK
    lateinit var uuidHelper: UUIDHelper

    @MockK
    lateinit var magnesInternalClient: MagnesInternalClient

    @MockK
    lateinit var callback: DataCollectorCallback

    // this uuid has no actual meaning; magnes requires a valid guid for tests
    private val sampleInstallationGUID: String = "0665203b-16e4-4ce2-be98-d7d73ec32e8a"
    private val riskCorrelationId = "risk_correlation_id"
    private val dataCollectorRequest: DataCollectorRequest =
        DataCollectorRequest(false, riskCorrelationId)
    private val braintreeClient: BraintreeClient =
        MockkBraintreeClientBuilder()
            .configurationSuccess(fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN))
            .build()

    @Before
    fun beforeEach() {

        MockKAnnotations.init(this, relaxed = true)

        every { configuration.environment } returns "sandbox"
        every { uuidHelper.getInstallationGUID(context) } returns sampleInstallationGUID
        every { magnesInternalClient.getClientMetadataId(
            context,
            configuration,
            any()
        ) } returns "paypal-clientmetadata-id"

        every {
            magnesInternalClient.getClientMetadataIdWithCallback(
                context,
                configuration,
                any(),
                any()
            )
        } answers {
            val callback = arg<(String?, Exception?) -> Unit>(3)
            callback("paypal-clientmetadata-id", null)
        }
    }

    @Test
    fun `when getPayPalInstallationGUID is called, the installation GUID is returned from UUIDHelper`() {

        val sut = DataCollector(braintreeClient, magnesInternalClient, uuidHelper)

        Assert.assertEquals(sampleInstallationGUID, sut.getPayPalInstallationGUID(context))
    }

    @Test
    fun `when getClientMetadataId is called with a boolean consent flag, a request with the installation guid and consent flag is forwarded to magnes`() {
        val hasUserLocationConsent = true

        val sut = DataCollector(braintreeClient, magnesInternalClient, uuidHelper)
        sut.getClientMetadataId(context, configuration, hasUserLocationConsent)

        val captor = slot<DataCollectorInternalRequest>()
        verify {
            magnesInternalClient.getClientMetadataId(
                context,
                configuration,
                capture(captor)
            )
        }

        val request = captor.captured

        Assert.assertEquals(sampleInstallationGUID, request.applicationGuid)
        Assert.assertEquals(hasUserLocationConsent, request.hasUserLocationConsent)
    }

    @Test
    fun `when getClientMetadataId is called with a custom request, the custom request is forwarded to magnes`() {

        val customRequest =
            DataCollectorInternalRequest(true)

        every { magnesInternalClient.getClientMetadataId(
            context,
            configuration,
            customRequest
        ) } returns "paypal-clientmetadata-id"

        val sut = DataCollector(braintreeClient, magnesInternalClient, uuidHelper)
        sut.getClientMetadataId(context, customRequest, configuration)

        verify {
            magnesInternalClient.getClientMetadataId(
                context,
                configuration,
                customRequest
            )
        }
    }

    @Test
    fun `when getClientMetadataId is called, the client metadata id from magnes is returned`() {

        val sut = DataCollector(braintreeClient, magnesInternalClient, uuidHelper)
        val result = sut.getClientMetadataId(context, configuration, true)

        Assert.assertEquals("paypal-clientmetadata-id", result)
    }

    @Test
    fun `when collectDeviceData is called and configuration fetch fails, callback receives a failure result with the configuration error`() = runTest(testDispatcher) {
        val configError = IOException("configuration error")
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationError(configError)
            .build()

        val testScope = TestScope(testDispatcher)
        val sut = DataCollector(braintreeClient, magnesInternalClient, uuidHelper, testDispatcher, testScope)
        sut.collectDeviceData(context, dataCollectorRequest, callback)
        advanceUntilIdle()

        val deviceDataCaptor = slot<DataCollectorResult>()
        verify { callback.onDataCollectorResult(capture(deviceDataCaptor)) }

        val result = deviceDataCaptor.captured
        Assert.assertTrue(result is DataCollectorResult.Failure)
        Assert.assertEquals(configError, (result as DataCollectorResult.Failure).error)
    }

    @Test
    fun `when collectDeviceData is called, the installation guid is forwarded to magnes and user location consent defaults to false`() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .build()

        val testScope = TestScope(testDispatcher)
        val sut = DataCollector(braintreeClient, magnesInternalClient, uuidHelper, testDispatcher, testScope)
        sut.collectDeviceData(context, dataCollectorRequest, callback)
        advanceUntilIdle()

        val captor = slot<DataCollectorInternalRequest>()
        verify {
            magnesInternalClient.getClientMetadataId(
                context,
                configuration,
                capture(captor)
            )
        }

        val request = captor.captured
        Assert.assertEquals(sampleInstallationGUID, request.applicationGuid)
        Assert.assertFalse(request.hasUserLocationConsent)
    }

    @Test
    fun `when collectDeviceData is called with a request containing a risk correlation id, the risk correlation id is forwarded to magnes as the client metadata id`() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .build()

        val testScope = TestScope(testDispatcher)
        val sut = DataCollector(braintreeClient, magnesInternalClient, uuidHelper, testDispatcher, testScope)
        sut.collectDeviceData(context, dataCollectorRequest, callback)
        advanceUntilIdle()

        val captor = slot<DataCollectorInternalRequest>()
        verify {
            magnesInternalClient.getClientMetadataId(
                context,
                configuration,
                capture(captor)
            )
        }

        val request = captor.captured
        Assert.assertEquals(sampleInstallationGUID, request.applicationGuid)
        Assert.assertEquals(riskCorrelationId, request.clientMetadataId)
        Assert.assertFalse(request.hasUserLocationConsent)
    }

    @Test
    @Throws(Exception::class)
    fun `when collectDeviceData succeeds, callback receives a success result containing device data json with the correlation id from magnes`() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .build()

        val testScope = TestScope(testDispatcher)
        val sut = DataCollector(braintreeClient, magnesInternalClient, uuidHelper, testDispatcher, testScope)
        sut.collectDeviceData(context, dataCollectorRequest, callback)
        advanceUntilIdle()

        val deviceDataCaptor = slot<DataCollectorResult>()
        verify { callback.onDataCollectorResult(capture(deviceDataCaptor)) }

        val result = deviceDataCaptor.captured
        Assert.assertTrue(result is DataCollectorResult.Success)
        val deviceData = (result as DataCollectorResult.Success).deviceData
        val json = JSONObject(deviceData)
        Assert.assertEquals("paypal-clientmetadata-id", json.getString("correlation_id"))
    }

    @Test
    fun `when collectDeviceData is called with a request that has no user location consent, the resulting magnes request has user location consent set to false`() =
        runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .build()

        val testScope = TestScope(testDispatcher)
        val sut = DataCollector(braintreeClient, magnesInternalClient, uuidHelper, testDispatcher, testScope)
        sut.collectDeviceData(context, dataCollectorRequest, callback)
        advanceUntilIdle()

        val deviceDataCaptor = slot<DataCollectorResult>()
        verify { callback.onDataCollectorResult(capture(deviceDataCaptor)) }

        val captor = slot<DataCollectorInternalRequest>()
        verify {
            magnesInternalClient.getClientMetadataId(
                context,
                configuration,
                capture(captor)
            )
        }
        Assert.assertFalse(captor.captured.hasUserLocationConsent)
    }

    @Test
    fun `when collectDeviceData is called with a request that has user location consent true, the resulting magnes request has user location consent set to true`() =
        runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .build()

        val testScope = TestScope(testDispatcher)
        val sut = DataCollector(braintreeClient, magnesInternalClient, uuidHelper, testDispatcher, testScope)
        val dataCollectorRequest = DataCollectorRequest(true)
        sut.collectDeviceData(context, dataCollectorRequest, callback)
        advanceUntilIdle()

        val deviceDataCaptor = slot<DataCollectorResult>()
        verify { callback.onDataCollectorResult(capture(deviceDataCaptor)) }

        val captor = slot<DataCollectorInternalRequest>()
        verify {
            magnesInternalClient.getClientMetadataId(
                context,
                configuration,
                capture(captor)
            )
        }
        Assert.assertTrue(captor.captured.hasUserLocationConsent)
    }

    @Test
    fun `when collectDeviceData configuration fetch throws a CancellationException, the callback is never invoked`() =
    runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationError(kotlin.coroutines.cancellation.CancellationException("cancelled"))
            .build()

        val testScope = TestScope(testDispatcher)
        val sut = DataCollector(braintreeClient, magnesInternalClient, uuidHelper, testDispatcher, testScope)
        sut.collectDeviceData(context, dataCollectorRequest, callback)
        advanceUntilIdle()

        verify(exactly = 0) { callback.onDataCollectorResult(any()) }
    }

    // Tests for collectDeviceDataOnSuccess

    @Test
    fun `when collectDeviceDataOnSuccess is called and configuration fetch fails, callback receives a failure result with the configuration error`() = runTest(testDispatcher) {
        val configError = IOException("configuration error")
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationError(configError)
            .build()

        val testScope = TestScope(testDispatcher)
        val sut = DataCollector(braintreeClient, magnesInternalClient, uuidHelper, testDispatcher, testScope)
        sut.collectDeviceDataOnSuccess(context, dataCollectorRequest, callback)
        advanceUntilIdle()

        val deviceDataCaptor = slot<DataCollectorResult>()
        verify { callback.onDataCollectorResult(capture(deviceDataCaptor)) }

        val result = deviceDataCaptor.captured
        Assert.assertTrue(result is DataCollectorResult.Failure)
        Assert.assertEquals(configError, (result as DataCollectorResult.Failure).error)
    }

    @Test
    fun `when collectDeviceDataOnSuccess is called, the installation guid is forwarded to magnes and user location consent defaults to false`() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .build()

        val testScope = TestScope(testDispatcher)
        val sut = DataCollector(braintreeClient, magnesInternalClient, uuidHelper, testDispatcher, testScope)
        sut.collectDeviceDataOnSuccess(context, dataCollectorRequest, callback)
        advanceUntilIdle()

        val captor = slot<DataCollectorInternalRequest>()
        verify {
            magnesInternalClient.getClientMetadataIdWithCallback(
                context,
                configuration,
                capture(captor),
                any()
            )
        }

        val request = captor.captured
        Assert.assertEquals(sampleInstallationGUID, request.applicationGuid)
        Assert.assertFalse(request.hasUserLocationConsent)
    }

    @Test
    fun `when collectDeviceDataOnSuccess is called with a request containing a risk correlation id, the risk correlation id is forwarded to magnes as the client metadata id`() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .build()

        val testScope = TestScope(testDispatcher)
        val sut = DataCollector(braintreeClient, magnesInternalClient, uuidHelper, testDispatcher, testScope)
        sut.collectDeviceDataOnSuccess(context, dataCollectorRequest, callback)
        advanceUntilIdle()

        val captor = slot<DataCollectorInternalRequest>()
        verify {
            magnesInternalClient.getClientMetadataIdWithCallback(
                context,
                configuration,
                capture(captor),
                any()
            )
        }

        val request = captor.captured
        Assert.assertEquals(sampleInstallationGUID, request.applicationGuid)
        Assert.assertEquals("risk_correlation_id", request.clientMetadataId)
        Assert.assertFalse(request.hasUserLocationConsent)
    }

    @Test
    @Throws(Exception::class)
    fun `when collectDeviceDataOnSuccess succeeds, callback receives a success result containing device data json with the correlation id from magnes`() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .build()

        val testScope = TestScope(testDispatcher)
        val sut = DataCollector(braintreeClient, magnesInternalClient, uuidHelper, testDispatcher, testScope)
        sut.collectDeviceDataOnSuccess(context, dataCollectorRequest, callback)
        advanceUntilIdle()

        val deviceDataCaptor = slot<DataCollectorResult>()
        verify { callback.onDataCollectorResult(capture(deviceDataCaptor)) }

        val result = deviceDataCaptor.captured
        Assert.assertTrue(result is DataCollectorResult.Success)
        val deviceData = (result as DataCollectorResult.Success).deviceData
        val json = JSONObject(deviceData)
        Assert.assertEquals("paypal-clientmetadata-id", json.getString("correlation_id"))
    }

    @Test
    fun `when collectDeviceDataOnSuccess magnes callback returns a submit error, callback receives a failure result with the submit error`() = runTest(testDispatcher) {
        val submitError = CallbackSubmitException.SubmitError
        every {
            magnesInternalClient.getClientMetadataIdWithCallback(
                context,
                configuration,
                any(),
                any()
            )
        } answers {
            val callback = arg<(String?, Exception?) -> Unit>(3)
            callback(null, submitError)
        }

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .build()

        val testScope = TestScope(testDispatcher)
        val sut = DataCollector(braintreeClient, magnesInternalClient, uuidHelper, testDispatcher, testScope)
        sut.collectDeviceDataOnSuccess(context, dataCollectorRequest, callback)
        advanceUntilIdle()

        val deviceDataCaptor = slot<DataCollectorResult>()
        verify { callback.onDataCollectorResult(capture(deviceDataCaptor)) }

        val result = deviceDataCaptor.captured
        Assert.assertTrue(result is DataCollectorResult.Failure)
        Assert.assertTrue((result as DataCollectorResult.Failure).error is CallbackSubmitException.SubmitError)
    }

    @Test
    fun `when collectDeviceDataOnSuccess magnes callback returns a submit timeout, callback receives a failure result with the submit timeout error`() = runTest(testDispatcher) {
        val submitTimeout = CallbackSubmitException.SubmitTimeout
        every {
            magnesInternalClient.getClientMetadataIdWithCallback(
                context,
                configuration,
                any(),
                any()
            )
        } answers {
            val callback = arg<(String?, Exception?) -> Unit>(3)
            callback(null, submitTimeout)
        }

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .build()

        val testScope = TestScope(testDispatcher)
        val sut = DataCollector(braintreeClient, magnesInternalClient, uuidHelper, testDispatcher, testScope)
        sut.collectDeviceDataOnSuccess(context, dataCollectorRequest, callback)
        advanceUntilIdle()

        val deviceDataCaptor = slot<DataCollectorResult>()
        verify { callback.onDataCollectorResult(capture(deviceDataCaptor)) }

        val result = deviceDataCaptor.captured
        Assert.assertTrue(result is DataCollectorResult.Failure)
        Assert.assertTrue((result as DataCollectorResult.Failure).error is CallbackSubmitException.SubmitTimeout)
    }

    @Test
    fun `when collectDeviceDataOnSuccess is called with a request that has user location consent true, the resulting magnes request has user location consent set to true`() =
        runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .build()

        val testScope = TestScope(testDispatcher)
        val sut = DataCollector(braintreeClient, magnesInternalClient, uuidHelper, testDispatcher, testScope)
        val dataCollectorRequest = DataCollectorRequest(true)
        sut.collectDeviceDataOnSuccess(context, dataCollectorRequest, callback)
        advanceUntilIdle()

        val deviceDataCaptor = slot<DataCollectorResult>()
        verify { callback.onDataCollectorResult(capture(deviceDataCaptor)) }

        val captor = slot<DataCollectorInternalRequest>()
        verify {
            magnesInternalClient.getClientMetadataIdWithCallback(
                context,
                configuration,
                capture(captor),
                any()
            )
        }
        Assert.assertTrue(captor.captured.hasUserLocationConsent)
    }

    @Test
    fun `when collectDeviceDataOnSuccess configuration fetch throws a CancellationException, the callback is never invoked`() =
    runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationError(kotlin.coroutines.cancellation.CancellationException("cancelled"))
            .build()

        val testScope = TestScope(testDispatcher)
        val sut = DataCollector(braintreeClient, magnesInternalClient, uuidHelper, testDispatcher, testScope)
        sut.collectDeviceDataOnSuccess(context, dataCollectorRequest, callback)
        advanceUntilIdle()

        verify(exactly = 0) { callback.onDataCollectorResult(any()) }
    }
}
