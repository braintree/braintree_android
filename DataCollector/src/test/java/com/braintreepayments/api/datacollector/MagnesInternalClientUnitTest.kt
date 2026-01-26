package com.braintreepayments.api.datacollector

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.braintreepayments.api.core.Configuration
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import lib.android.paypal.com.magnessdk.Environment
import lib.android.paypal.com.magnessdk.InvalidInputException
import lib.android.paypal.com.magnessdk.MagnesResult
import lib.android.paypal.com.magnessdk.MagnesSDK
import lib.android.paypal.com.magnessdk.MagnesSettings
import lib.android.paypal.com.magnessdk.MagnesSource
import lib.android.paypal.com.magnessdk.MagnesSubmitListener
import lib.android.paypal.com.magnessdk.MagnesSubmitStatus
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MagnesInternalClientUnitTest {

    @MockK
    lateinit var magnesSDK: MagnesSDK

    @MockK
    lateinit var prodConfiguration: Configuration

    @MockK
    lateinit var sandboxConfiguration: Configuration

    @MockK
    lateinit var magnesResult: MagnesResult

    @MockK
    lateinit var mapData: HashMap<String, String>

    private lateinit var context: Context
    private lateinit var dataCollectorInternalRequest: DataCollectorInternalRequest

    // NOTE: this uuid has no actual meaning; Magnes requires a valid guid for tests
    private var validApplicationGUID: String = "0665203b-16e4-4ce2-be98-d7d73ec32e8a"
    private val hasUserLocationConsent = true

    @Before
    fun beforeEach() {

        context = ApplicationProvider.getApplicationContext()

        MockKAnnotations.init(this, relaxed = true)

        every { prodConfiguration.environment } returns "production"
        every { sandboxConfiguration.environment } returns "sandbox"
        every { magnesResult.paypalClientMetaDataId } returns "magnes-client-metadata-id"
        every {
            magnesSDK.collectAndSubmit(any(), any(), any(), any())
        } returns magnesResult

        dataCollectorInternalRequest = DataCollectorInternalRequest(hasUserLocationConsent).apply {
            clientMetadataId = "sample-client-metadata-id"
            isDisableBeacon = true
            additionalData = mapData
            applicationGuid = validApplicationGUID
        }
    }

    @Test
    fun getClientMetaDataId_returnsEmptyStringWhenContextIsNull() {
        val sut = MagnesInternalClient(magnesSDK)
        var receivedClientMetadataId = "non-empty"

        sut.getClientMetadataId(
            null, sandboxConfiguration,
            dataCollectorInternalRequest
        ) { clientMetadataId, _ ->
            receivedClientMetadataId = clientMetadataId ?: ""
        }

        Assert.assertTrue(receivedClientMetadataId.isEmpty())
    }

    @Throws(InvalidInputException::class)
    @Test
    fun getClientMetaDataId_configuresMagnesSourceAsBraintree() {

        val sut = MagnesInternalClient(magnesSDK)
        sut.getClientMetadataId(context, sandboxConfiguration, dataCollectorInternalRequest) { _, _ -> }

        val captor = slot<MagnesSettings>()
        verify { magnesSDK.setUp(capture(captor)) }

        val magnesSettings = captor.captured
        Assert.assertEquals(
            MagnesSource.BRAINTREE.version.toLong(),
            magnesSettings.magnesSource.toLong()
        )
    }

    @Throws(InvalidInputException::class)
    @Test
    fun getClientMetaDataId_whenBraintreeEnvironmentIsSandbox_configuresMagnesEnvironmentToSandbox() {

        val sut = MagnesInternalClient(magnesSDK)
        sut.getClientMetadataId(context, sandboxConfiguration, dataCollectorInternalRequest) { _, _ -> }

        val captor = slot<MagnesSettings>()
        verify { magnesSDK.setUp(capture(captor)) }

        val magnesSettings = captor.captured
        Assert.assertEquals(Environment.SANDBOX, magnesSettings.environment)
    }

    @Throws(InvalidInputException::class)
    @Test
    fun getClientMetaDataId_whenBraintreeEnvironmentIsProd_configuresMagnesEnvironmentToLive() {

        val sut = MagnesInternalClient(magnesSDK)
        sut.getClientMetadataId(context, prodConfiguration, dataCollectorInternalRequest) { _, _ -> }

        val captor = slot<MagnesSettings>()
        verify { magnesSDK.setUp(capture(captor)) }

        val magnesSettings = captor.captured
        Assert.assertEquals(Environment.LIVE, magnesSettings.environment)
    }

    @Throws(InvalidInputException::class)
    @Test
    fun getClientMetaDataId_forwardsDisableBeaconOptionToMagnes() {

        val sut = MagnesInternalClient(magnesSDK)
        sut.getClientMetadataId(context, prodConfiguration, dataCollectorInternalRequest) { _, _ -> }

        val captor = slot<MagnesSettings>()
        verify { magnesSDK.setUp(capture(captor)) }

        val magnesSettings = captor.captured
        Assert.assertTrue(magnesSettings.isDisableBeacon)
    }

    @Throws(InvalidInputException::class)
    @Test
    fun getClientMetaDataId_forwardsApplicationGUIDOptionToMagnes() {

        val sut = MagnesInternalClient(magnesSDK)
        sut.getClientMetadataId(context, prodConfiguration, dataCollectorInternalRequest) { _, _ -> }

        val captor = slot<MagnesSettings>()
        verify { magnesSDK.setUp(capture(captor)) }

        val magnesSettings = captor.captured
        Assert.assertEquals(validApplicationGUID, magnesSettings.appGuid)
    }

    @Throws(InvalidInputException::class)
    @Test
    fun getClientMetaDataId_setsHasUserLocationConsent() {

        val sut = MagnesInternalClient(magnesSDK)
        sut.getClientMetadataId(context, prodConfiguration, dataCollectorInternalRequest) { _, _ -> }

        val captor = slot<MagnesSettings>()
        verify { magnesSDK.setUp(capture(captor)) }

        val magnesSettings = captor.captured
        Assert.assertEquals(hasUserLocationConsent, magnesSettings.hasUserLocationConsent())
    }

    @Test
    fun getClientMetaDataId_returnsAnEmptyStringWhenApplicationGUIDIsInvalid() {

        val requestWithInvalidGUID =
            DataCollectorInternalRequest(hasUserLocationConsent, null, null, false)
        requestWithInvalidGUID.applicationGuid = "invalid guid"

        val sut = MagnesInternalClient(magnesSDK)
        var receivedClientMetadataId = "non-empty"
        sut.getClientMetadataId(
            null, sandboxConfiguration,
            dataCollectorInternalRequest
        ) { clientMetadataId, _ ->
            receivedClientMetadataId = clientMetadataId ?: ""
        }

        Assert.assertTrue(receivedClientMetadataId.isEmpty())
    }

    @Throws(InvalidInputException::class)
    @Test
    fun getClientMetaDataId_forwardsClientMetadataIdFromMagnesStart() {
        val callbackSlot = slot<MagnesSubmitListener>()
        every {
            magnesSDK.collectAndSubmit(
                context,
                "sample-client-metadata-id",
                mapData,
                capture(callbackSlot)
            )
        } returns magnesResult

        val sut = MagnesInternalClient(magnesSDK)
        var receivedClientMetadataId = "non-empty"
        sut.getClientMetadataId(
            context, prodConfiguration,
            dataCollectorInternalRequest
        ) { clientMetadataId, _ ->
            receivedClientMetadataId = clientMetadataId ?: ""
        }

        callbackSlot.captured.onSubmitComplete(MagnesSubmitStatus.SUCCESS, "magnes-client-metadata-id")

        Assert.assertEquals("magnes-client-metadata-id", receivedClientMetadataId)
    }

    @Throws(InvalidInputException::class)
    @Test
    fun getClientMetaDataId_returnsAnEmptyStringWhenCollectAndSubmitThrows() {
        every {
            magnesSDK.collectAndSubmit(
                context,
                "sample-client-metadata-id",
                mapData,
                any()
            )
        } throws InvalidInputException("invalid input")

        val sut = MagnesInternalClient(magnesSDK)
        var receivedClientMetadataId = "non-empty"

        sut.getClientMetadataId(
            context, prodConfiguration,
            dataCollectorInternalRequest
        ) { clientMetadataId, _ ->
            receivedClientMetadataId = clientMetadataId ?: ""
        }

        Assert.assertTrue(receivedClientMetadataId.isEmpty())
    }

    @Test
    fun getClientMetaDataId_returnsSubmitErrorWhenMagnesReturnsError() {
        every {
            magnesSDK.collectAndSubmit(
                context,
                "sample-client-metadata-id",
                mapData,
                any()
            )
        } answers {
            val callback = arg<lib.android.paypal.com.magnessdk.MagnesSubmitListener>(3)
            callback.onSubmitComplete(MagnesSubmitStatus.ERROR, "")
            magnesResult
        }

        val sut = MagnesInternalClient(magnesSDK)
        var receivedError: Exception? = null

        sut.getClientMetadataId(
            context, prodConfiguration,
            dataCollectorInternalRequest
        ) { _, error ->
            receivedError = error
        }

        Assert.assertTrue(receivedError is CallbackSubmitException.SubmitError)
    }

    @Test
    fun getClientMetaDataId_returnsSubmitTimeoutWhenMagnesReturnsTimeout() {
        every {
            magnesSDK.collectAndSubmit(
                context,
                "sample-client-metadata-id",
                mapData,
                any()
            )
        } answers {
            val callback = arg<lib.android.paypal.com.magnessdk.MagnesSubmitListener>(3)
            callback.onSubmitComplete(MagnesSubmitStatus.TIMEOUT, "")
            magnesResult
        }

        val sut = MagnesInternalClient(magnesSDK)
        var receivedError: Exception? = null

        sut.getClientMetadataId(
            context, prodConfiguration,
            dataCollectorInternalRequest
        ) { _, error ->
            receivedError = error
        }

        Assert.assertTrue(receivedError is CallbackSubmitException.SubmitTimeout)
    }
}
