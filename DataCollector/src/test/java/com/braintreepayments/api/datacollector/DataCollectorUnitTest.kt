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
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DataCollectorUnitTest {

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
        every {
            magnesInternalClient.getClientMetadataId(
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
    fun payPalInstallationGUID_returnsInstallationIdentifier() {

        val sut = DataCollector(braintreeClient, magnesInternalClient, uuidHelper)

        Assert.assertEquals(sampleInstallationGUID, sut.getPayPalInstallationGUID(context))
    }

    @Test
    fun getClientMetadataId_configuresMagnesWithDefaultRequest() {
        val hasUserLocationConsent = true

        val sut = DataCollector(braintreeClient, magnesInternalClient, uuidHelper)
        sut.getClientMetadataId(context, configuration, hasUserLocationConsent) { _, _ -> }

        val captor = slot<DataCollectorInternalRequest>()
        verify {
            magnesInternalClient.getClientMetadataId(
                context,
                configuration,
                capture(captor),
                any()
            )
        }

        val request = captor.captured

        Assert.assertEquals(sampleInstallationGUID, request.applicationGuid)
        Assert.assertEquals(hasUserLocationConsent, request.hasUserLocationConsent)
    }

    @Test
    fun getClientMetadataId_configuresMagnesWithCustomRequestAndForwardsClientMetadataIdFromMagnesResult() {

        val customRequest =
            DataCollectorInternalRequest(true)

        every {
            magnesInternalClient.getClientMetadataId(
                context,
                configuration,
                customRequest,
                any()
            )
        } answers {
            val callback = arg<(String?, Exception?) -> Unit>(3)
            callback("paypal-clientmetadata-id", null)
        }

        val sut = DataCollector(braintreeClient, magnesInternalClient, uuidHelper)
        sut.getClientMetadataId(context, customRequest, configuration) { _, _ -> }

        verify {
            magnesInternalClient.getClientMetadataId(
                context,
                configuration,
                customRequest,
                any()
            )
        }
    }

    @Test
    fun getClientMetadataId_forwardsClientMetadataIdFromMagnesResult() {

        val sut = DataCollector(braintreeClient, magnesInternalClient, uuidHelper)
        var receivedClientMetadataId: String = "non-empty"

        sut.getClientMetadataId(
            context, configuration,
            true
        ) { clientMetadataId, _ ->
            receivedClientMetadataId = clientMetadataId ?: ""
        }
        Assert.assertEquals("paypal-clientmetadata-id", receivedClientMetadataId)
    }

    @Test
    fun collectDeviceData_forwardsConfigurationFetchErrors() {
        val configError = Exception("configuration error")
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationError(configError)
            .build()

        val sut = DataCollector(braintreeClient, magnesInternalClient, uuidHelper)
        sut.collectDeviceData(context, dataCollectorRequest, callback)

        val deviceDataCaptor = slot<DataCollectorResult>()
        verify { callback.onDataCollectorResult(capture(deviceDataCaptor)) }

        val result = deviceDataCaptor.captured
        Assert.assertTrue(result is DataCollectorResult.Failure)
        Assert.assertEquals(configError, (result as DataCollectorResult.Failure).error)
    }

    @Test
    fun collectDeviceData_configuresMagnesWithDefaultRequest() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .build()

        val sut = DataCollector(braintreeClient, magnesInternalClient, uuidHelper)
        sut.collectDeviceData(context, dataCollectorRequest, callback)

        val captor = slot<DataCollectorInternalRequest>()
        verify {
            magnesInternalClient.getClientMetadataId(
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
    fun collectDeviceData_with_request_configuresMagnesWithDefaultRequest() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .build()

        val sut = DataCollector(braintreeClient, magnesInternalClient, uuidHelper)
        sut.collectDeviceData(context, dataCollectorRequest, callback)

        val captor = slot<DataCollectorInternalRequest>()
        verify {
            magnesInternalClient.getClientMetadataId(
                context,
                configuration,
                capture(captor),
                any()
            )
        }

        val request = captor.captured
        Assert.assertEquals(sampleInstallationGUID, request.applicationGuid)
        Assert.assertEquals(riskCorrelationId, request.clientMetadataId)
        Assert.assertFalse(request.hasUserLocationConsent)
    }

    @Test
    fun collectDeviceData_configuresMagnesWithClientId() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .build()

        val sut = DataCollector(braintreeClient, magnesInternalClient, uuidHelper)
        sut.collectDeviceData(context, dataCollectorRequest, callback)

        val captor = slot<DataCollectorInternalRequest>()
        verify {
            magnesInternalClient.getClientMetadataId(
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
    fun collectDeviceData_getsDeviceDataJSONWithCorrelationIdFromPayPal() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .build()

        val sut = DataCollector(braintreeClient, magnesInternalClient, uuidHelper)
        sut.collectDeviceData(context, dataCollectorRequest, callback)

        val deviceDataCaptor = slot<DataCollectorResult>()
        verify { callback.onDataCollectorResult(capture(deviceDataCaptor)) }

        val result = deviceDataCaptor.captured
        Assert.assertTrue(result is DataCollectorResult.Success)
        val deviceData = (result as DataCollectorResult.Success).deviceData
        val json = JSONObject(deviceData)
        Assert.assertEquals("paypal-clientmetadata-id", json.getString("correlation_id"))
    }

    @Test
    fun collectDeviceData_without_DataCollectorRequest_sets_hasUserLocationConsent_to_false() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .build()

        val sut = DataCollector(braintreeClient, magnesInternalClient, uuidHelper)
        sut.collectDeviceData(context, dataCollectorRequest, callback)

        val deviceDataCaptor = slot<DataCollectorResult>()
        verify { callback.onDataCollectorResult(capture(deviceDataCaptor)) }

        val captor = slot<DataCollectorInternalRequest>()
        verify {
            magnesInternalClient.getClientMetadataId(
                context,
                configuration,
                capture(captor),
                any()
            )
        }
        Assert.assertFalse(captor.captured.hasUserLocationConsent)
    }

    @Test
    fun collectDeviceData_with_DataCollectorRequest_sets_correct_values_for_getClientMetadataId() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .build()

        val sut = DataCollector(braintreeClient, magnesInternalClient, uuidHelper)
        val dataCollectorRequest = DataCollectorRequest(true)
        sut.collectDeviceData(context, dataCollectorRequest, callback)

        val deviceDataCaptor = slot<DataCollectorResult>()
        verify { callback.onDataCollectorResult(capture(deviceDataCaptor)) }

        val captor = slot<DataCollectorInternalRequest>()
        verify {
            magnesInternalClient.getClientMetadataId(
                context,
                configuration,
                capture(captor),
                any()
            )
        }
        Assert.assertTrue(captor.captured.hasUserLocationConsent)
    }

    @Test
    fun collectDeviceData_forwardsSubmitError() {
        val submitError = CallbackSubmitException.SubmitError()
        every {
            magnesInternalClient.getClientMetadataId(
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

        val sut = DataCollector(braintreeClient, magnesInternalClient, uuidHelper)
        sut.collectDeviceData(context, dataCollectorRequest, callback)

        val deviceDataCaptor = slot<DataCollectorResult>()
        verify { callback.onDataCollectorResult(capture(deviceDataCaptor)) }

        val result = deviceDataCaptor.captured
        Assert.assertTrue(result is DataCollectorResult.Failure)
        Assert.assertEquals(submitError, (result as DataCollectorResult.Failure).error)
    }

    @Test
    fun collectDeviceData_forwardsSubmitTimeoutError() {
        val submitTimeoutError = CallbackSubmitException.SubmitTimeout()
        every {
            magnesInternalClient.getClientMetadataId(
                context,
                configuration,
                any(),
                any()
            )
        } answers {
            val callback = arg<(String?, Exception?) -> Unit>(3)
            callback(null, submitTimeoutError)
        }

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .build()

        val sut = DataCollector(braintreeClient, magnesInternalClient, uuidHelper)
        sut.collectDeviceData(context, dataCollectorRequest, callback)

        val deviceDataCaptor = slot<DataCollectorResult>()
        verify { callback.onDataCollectorResult(capture(deviceDataCaptor)) }

        val result = deviceDataCaptor.captured
        Assert.assertTrue(result is DataCollectorResult.Failure)
        Assert.assertEquals(submitTimeoutError, (result as DataCollectorResult.Failure).error)
    }
}
