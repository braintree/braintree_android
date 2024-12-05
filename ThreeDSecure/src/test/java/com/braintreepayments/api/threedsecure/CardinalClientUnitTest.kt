package com.braintreepayments.api.threedsecure

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.Configuration
import com.cardinalcommerce.cardinalmobilesdk.Cardinal
import com.cardinalcommerce.cardinalmobilesdk.enums.CardinalEnvironment
import com.cardinalcommerce.cardinalmobilesdk.enums.CardinalUiType
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalChallengeObserver
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalConfigurationParameters
import com.cardinalcommerce.cardinalmobilesdk.services.CardinalInitService
import com.cardinalcommerce.cardinalmobilesdk.services.CardinalValidateReceiver
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertSame
import junit.framework.TestCase.assertTrue
import junit.framework.TestCase.fail
import org.junit.Before
import org.junit.Test

class CardinalClientUnitTest {

    private lateinit var cardinalInstance: Cardinal
    private lateinit var activity: FragmentActivity
    private lateinit var configuration: Configuration
    private lateinit var context: Context
    private lateinit var cardinalInitializeCallback: CardinalInitializeCallback
    private lateinit var cardinalValidateReceiver: CardinalValidateReceiver
    private lateinit var cardinalChallengeObserver: CardinalChallengeObserver

    @Before
    fun beforeEach() {
        mockkStatic(Cardinal::class)

        context = mockk(relaxed = true)
        configuration = mockk(relaxed = true)
        cardinalInitializeCallback = mockk(relaxed = true)
        cardinalInstance = mockk(relaxed = true)
        activity = mockk(relaxed = true)
        cardinalValidateReceiver = mockk(relaxed = true)
        cardinalChallengeObserver = mockk(relaxed = true)
    }

    @Test
    @Throws(BraintreeException::class)
    fun initialize_configuresDefaultCardinalConfigurationParameters() {
        every { Cardinal.getInstance() } returns cardinalInstance

        val sut = CardinalClient()
        val request = ThreeDSecureRequest()
        sut.initialize(context, configuration, request, cardinalInitializeCallback)

        val parametersSlot = slot<CardinalConfigurationParameters>()
        verify { cardinalInstance.configure(context, capture(parametersSlot)) }

        val parameters = parametersSlot.captured
        assertEquals(CardinalEnvironment.STAGING, parameters.environment)
        assertEquals(8000, parameters.requestTimeout.toLong())
        assertTrue(parameters.isEnableDFSync)
    }

    @Test
    @Throws(BraintreeException::class)
    fun initialize_whenV2UiCustomizationNotNull_setsCardinalConfigurationParameters() {
        every { Cardinal.getInstance() } returns cardinalInstance

        val sut = CardinalClient()
        val v2UiCustomization = ThreeDSecureV2UiCustomization()
        val request = ThreeDSecureRequest()
        request.v2UiCustomization = v2UiCustomization
        sut.initialize(context, configuration, request, cardinalInitializeCallback)

        val parametersSlot = slot<CardinalConfigurationParameters>()
        verify { cardinalInstance.configure(context, capture(parametersSlot)) }

        val parameters = parametersSlot.captured
        assertEquals(
            request.v2UiCustomization!!.cardinalUiCustomization.toolbarCustomization,
            parameters.uiCustomization.toolbarCustomization
        )
    }

    @Test
    @Throws(BraintreeException::class)
    fun initialize_whenEnvironmentProduction_configuresCardinalEnvironmentProduction() {
        every { Cardinal.getInstance() } returns cardinalInstance
        every { configuration.environment } returns "production"

        val sut = CardinalClient()
        val request = ThreeDSecureRequest()
        sut.initialize(context, configuration, request, cardinalInitializeCallback)

        val parametersSlot = slot<CardinalConfigurationParameters>()
        verify { cardinalInstance.configure(context, capture(parametersSlot)) }

        val parameters = parametersSlot.captured
        assertEquals(CardinalEnvironment.PRODUCTION, parameters.environment)
    }

    @Test
    @Throws(BraintreeException::class)
    fun initialize_whenUiTypeNotNull_setsCardinalConfigurationParameters() {
        every { Cardinal.getInstance() } returns cardinalInstance

        val sut = CardinalClient()
        val request = ThreeDSecureRequest().apply {
            uiType = ThreeDSecureUiType.BOTH
        }
        sut.initialize(context, configuration, request, cardinalInitializeCallback)

        val parametersSlot = slot<CardinalConfigurationParameters>()
        verify { cardinalInstance.configure(context, capture(parametersSlot)) }

        val parameters = parametersSlot.captured
        assertEquals(CardinalUiType.BOTH, parameters.uiType)
    }

    @Test
    @Throws(BraintreeException::class)
    fun initialize_whenRenderTypeNotNull_setsCardinalConfigurationParameters() {
        every { Cardinal.getInstance() } returns cardinalInstance

        val sut = CardinalClient()
        val request = ThreeDSecureRequest().apply {
            renderTypes = listOf(
                ThreeDSecureRenderType.OTP,
                ThreeDSecureRenderType.SINGLE_SELECT,
                ThreeDSecureRenderType.MULTI_SELECT,
                ThreeDSecureRenderType.OOB,
                ThreeDSecureRenderType.RENDER_HTML,
            )
        }
        sut.initialize(context, configuration, request, cardinalInitializeCallback)

        val parametersSlot = slot<CardinalConfigurationParameters>()
        verify { cardinalInstance.configure(context, capture(parametersSlot)) }

        val parameters = parametersSlot.captured
        assertEquals(5, parameters.renderType.length())
    }

    @Test
    @Throws(BraintreeException::class)
    fun initialize_returnsConsumerSessionIdToListener() {
        every { Cardinal.getInstance() } returns cardinalInstance
        every { configuration.cardinalAuthenticationJwt } returns "token"

        val sut = CardinalClient()
        val request = ThreeDSecureRequest()
        sut.initialize(context, configuration, request, cardinalInitializeCallback)

        val cardinalInitServiceSlot = slot<CardinalInitService>()
        verify { cardinalInstance.init("token", capture(cardinalInitServiceSlot)) }

        val cardinalInitService = cardinalInitServiceSlot.captured
        cardinalInitService.onSetupCompleted("session-id")

        verify { cardinalInitializeCallback.onResult(sut.consumerSessionId, null) }
        assertEquals("session-id", sut.consumerSessionId)
    }

    @Test
    @Throws(BraintreeException::class)
    fun initialize_whenConsumerSessionIdIsNull_returnsBraintreeExceptionToListener() {
        every { Cardinal.getInstance() } returns cardinalInstance
        every { configuration.cardinalAuthenticationJwt } returns "token"

        val sut = CardinalClient()
        val request = ThreeDSecureRequest()
        sut.initialize(context, configuration, request, cardinalInitializeCallback)

        val cardinalInitServiceSlot = slot<CardinalInitService>()
        verify { cardinalInstance.init("token", capture(cardinalInitServiceSlot)) }

        val cardinalInitService = cardinalInitServiceSlot.captured
        cardinalInitService.onValidated(null, null)

        val exceptionSlot = slot<BraintreeException>()
        verify { cardinalInitializeCallback.onResult(isNull(), capture(exceptionSlot)) }
        assertEquals(exceptionSlot.captured.message, "consumer session id not available")
    }


    @Test
    fun `when cardinal configuration is called with a requestorAppUrl, sets threeDSRequestorAppURL`() {
        every { Cardinal.getInstance() } returns cardinalInstance

        val sut = CardinalClient()
        val request = ThreeDSecureRequest()
        request.requestorAppUrl = "www.paypal.com"
        sut.initialize(context, configuration, request, cardinalInitializeCallback)

        val parametersSlot = slot<CardinalConfigurationParameters>()
        verify { cardinalInstance.configure(context, capture(parametersSlot)) }

        val parameters = parametersSlot.captured
        assertEquals("www.paypal.com", parameters.threeDSRequestorAppURL)
    }

    @Test
    fun initialize_onCardinalConfigureRuntimeException_throwsError() {
        every { Cardinal.getInstance() } returns cardinalInstance
        every { configuration.cardinalAuthenticationJwt } returns "token"

        val sut = CardinalClient()
        val runtimeException = RuntimeException("fake message")
        every { cardinalInstance.configure(any(), any()) } throws runtimeException

        val request = ThreeDSecureRequest()
        try {
            sut.initialize(context, configuration, request, cardinalInitializeCallback)
            fail("should not get here")
        } catch (e: BraintreeException) {
            assertEquals("Cardinal SDK configure Error.", e.message)
            assertSame(runtimeException, e.cause)
        }
    }

    @Test
    fun initialize_onCardinalInitRuntimeException_throwsError() {
        every { Cardinal.getInstance() } returns cardinalInstance
        every { configuration.cardinalAuthenticationJwt } returns "token"

        val sut = CardinalClient()
        val runtimeException = RuntimeException("fake message")
        every { cardinalInstance.init(any(), any()) } throws runtimeException

        val request = ThreeDSecureRequest()
        try {
            sut.initialize(context, configuration, request, cardinalInitializeCallback)
            fail("should not get here")
        } catch (e: BraintreeException) {
            assertEquals("Cardinal SDK init Error.", e.message)
            assertSame(runtimeException, e.cause)
        }
    }

    @Test
    @Throws(BraintreeException::class)
    fun continueLookup_continuesCardinalLookup() {
        every { Cardinal.getInstance() } returns cardinalInstance

        val sut = CardinalClient()
        val threeDSecureLookup = mockk<ThreeDSecureLookup>(relaxed = true)

        every { threeDSecureLookup.transactionId } returns "sample-transaction-id"
        every { threeDSecureLookup.pareq } returns "sample-payer-authentication-request"

        val threeDSecureParams = mockk<ThreeDSecureParams>(relaxed = true)
        every { threeDSecureParams.lookup } returns threeDSecureLookup

        sut.continueLookup(threeDSecureParams, cardinalChallengeObserver)
        verify {
            cardinalInstance.cca_continue(
                "sample-transaction-id",
                "sample-payer-authentication-request",
                cardinalChallengeObserver
            )
        }
    }

    @Test
    fun continueLookup_onCardinalRuntimeException_throwsError() {
        every { Cardinal.getInstance() } returns cardinalInstance

        val runtimeException = RuntimeException("fake message")
        every { cardinalInstance.cca_continue(any(), any(), any()) } throws runtimeException

        val sut = CardinalClient()
        val threeDSecureLookup = mockk<ThreeDSecureLookup>(relaxed = true)
        every { threeDSecureLookup.transactionId } returns "sample-transaction-id"
        every { threeDSecureLookup.pareq } returns "sample-payer-authentication-request"

        val threeDSecureParams = mockk<ThreeDSecureParams>(relaxed = true)
        every { threeDSecureParams.lookup } returns threeDSecureLookup

        try {
            sut.continueLookup(threeDSecureParams, cardinalChallengeObserver)
            fail("should not get here")
        } catch (e: BraintreeException) {
            assertEquals("Cardinal SDK cca_continue Error.", e.message)
            assertSame(runtimeException, e.cause)
        }
    }

    @Test
    fun `when continueLookup is called with a null challengeObserver, throws BraintreeException`() {
        val sut = CardinalClient()

        try {
            sut.continueLookup(mockk(), null)
        } catch (e: BraintreeException) {
            assertEquals("challengeObserver is null", e.message)
        }
    }

    @Test
    fun cleanup_cleansUpCardinalInstance() {
        every { Cardinal.getInstance() } returns cardinalInstance

        val sut = CardinalClient()
        sut.cleanup()
        verify { cardinalInstance.cleanup() }
    }
}
