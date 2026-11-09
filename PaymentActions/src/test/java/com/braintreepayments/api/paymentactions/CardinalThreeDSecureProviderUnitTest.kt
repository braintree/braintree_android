package com.braintreepayments.api.paymentactions

import android.content.Context
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import com.cardinalcommerce.cardinalmobilesdk.Cardinal
import com.cardinalcommerce.cardinalmobilesdk.enums.CardinalEnvironment
import com.cardinalcommerce.cardinalmobilesdk.enums.CardinalUiType
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalConfigurationParameters
import com.cardinalcommerce.cardinalmobilesdk.services.CardinalInitService
import com.cardinalcommerce.shared.models.exceptions.InvalidInputException
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CardinalThreeDSecureProviderUnitTest {

    private val context = mockk<Context>(relaxed = true)
    private val cardinal = mockk<Cardinal>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(Cardinal::class)
        every { Cardinal.getInstance() } returns cardinal
    }

    @After
    fun tearDown() {
        unmockkStatic(Cardinal::class)
    }

    private fun configuration(environment: String = "sandbox", jwt: String? = "cardinal-jwt") =
        mockk<Configuration> {
            every { this@mockk.environment } returns environment
            every { cardinalAuthenticationJwt } returns jwt
        }

    private fun buildSut(configuration: Configuration = configuration()) = CardinalThreeDSecureProvider(
        context,
        MockkBraintreeClientBuilder().configurationSuccess(configuration).build(),
    )

    /** Makes Cardinal answer `init` by completing setup with [sessionId]. */
    private fun stubSetupCompleted(sessionId: String = "df-ref") {
        every { cardinal.init(any(), any<CardinalInitService>()) } answers {
            secondArg<CardinalInitService>().onSetupCompleted(sessionId)
        }
    }

    private fun captureParameters(): CardinalConfigurationParameters {
        val parameters = slot<CardinalConfigurationParameters>()
        verify { cardinal.configure(context, capture(parameters)) }
        return parameters.captured
    }

    @Test
    fun `collectDeviceData configures Cardinal for a non-production environment`() = runTest {
        stubSetupCompleted()

        buildSut(configuration(environment = "sandbox")).collectDeviceData()

        val parameters = captureParameters()
        assertEquals(CardinalEnvironment.STAGING, parameters.environment)
        assertEquals(CardinalThreeDSecureProvider.REQUEST_TIMEOUT_MS, parameters.requestTimeout)
        assertEquals(CardinalThreeDSecureProvider.CHALLENGE_TIMEOUT_MINUTES, parameters.challengeTimeout)
        assertTrue(parameters.isEnableDFSync)
        assertTrue(parameters.isEnableLogging)
        assertEquals(CardinalUiType.BOTH, parameters.uiType)
    }

    @Test
    fun `collectDeviceData configures Cardinal for production with logging disabled`() = runTest {
        stubSetupCompleted()

        buildSut(configuration(environment = "production")).collectDeviceData()

        val parameters = captureParameters()
        assertEquals(CardinalEnvironment.PRODUCTION, parameters.environment)
        assertFalse(parameters.isEnableLogging)
    }

    @Test
    fun `collectDeviceData configures, then initializes with the 2-arg init and the configuration JWT`() =
        runTest {
            stubSetupCompleted()

            buildSut().collectDeviceData()

            verifyOrder {
                cardinal.configure(context, any())
                cardinal.init("cardinal-jwt", any<CardinalInitService>())
            }
            verify(exactly = 0) { cardinal.init(any(), any<String>(), any()) }
        }

    @Test
    fun `collectDeviceData returns the setup session id as the dfReferenceId`() = runTest {
        stubSetupCompleted(sessionId = "df-ref")

        val result = buildSut().collectDeviceData()

        assertEquals("df-ref", assertIs<DeviceDataResult.Collected>(result).dfReferenceId)
    }

    @Test
    fun `collectDeviceData with a missing JWT returns Failed without touching Cardinal`() = runTest {
        val result = buildSut(configuration(jwt = null)).collectDeviceData()

        val error = assertIs<BraintreeException>(assertIs<DeviceDataResult.Failed>(result).error)
        assertEquals(CardinalThreeDSecureProvider.MISSING_CARDINAL_JWT_MESSAGE, error.message)
        verify(exactly = 0) { cardinal.configure(any(), any()) }
        verify(exactly = 0) { cardinal.init(any(), any<CardinalInitService>()) }
    }

    @Test
    fun `collectDeviceData with an empty JWT returns Failed`() = runTest {
        val result = buildSut(configuration(jwt = "")).collectDeviceData()

        val error = assertIs<BraintreeException>(assertIs<DeviceDataResult.Failed>(result).error)
        assertEquals(CardinalThreeDSecureProvider.MISSING_CARDINAL_JWT_MESSAGE, error.message)
    }

    @Test
    fun `collectDeviceData returns Failed when Cardinal validates instead of completing setup`() = runTest {
        every { cardinal.init(any(), any<CardinalInitService>()) } answers {
            secondArg<CardinalInitService>().onValidated(null, null)
        }

        val result = buildSut().collectDeviceData()

        val error = assertIs<BraintreeException>(assertIs<DeviceDataResult.Failed>(result).error)
        assertEquals(CardinalThreeDSecureProvider.CARDINAL_SETUP_VALIDATION_FAILED_MESSAGE, error.message)
    }

    @Test
    fun `collectDeviceData keeps the first result when Cardinal calls back twice`() = runTest {
        every { cardinal.init(any(), any<CardinalInitService>()) } answers {
            val service = secondArg<CardinalInitService>()
            service.onSetupCompleted("df-ref")
            service.onValidated(null, null)
        }

        val result = buildSut().collectDeviceData()

        assertEquals("df-ref", assertIs<DeviceDataResult.Collected>(result).dfReferenceId)
    }

    @Test
    fun `collectDeviceData returns Failed wrapping the cause when configure throws`() = runTest {
        val cause = InvalidInputException("bad input", Throwable())
        every { cardinal.configure(any(), any()) } throws cause

        val result = buildSut().collectDeviceData()

        val error = assertIs<BraintreeException>(assertIs<DeviceDataResult.Failed>(result).error)
        assertEquals(CardinalThreeDSecureProvider.CARDINAL_SETUP_ERROR_MESSAGE, error.message)
        assertSame(cause, error.cause)
        verify(exactly = 0) { cardinal.init(any(), any<CardinalInitService>()) }
    }

    @Test
    fun `collectDeviceData returns Failed wrapping the cause when init throws`() = runTest {
        val cause = IllegalStateException("init failed")
        every { cardinal.init(any(), any<CardinalInitService>()) } throws cause

        val result = buildSut().collectDeviceData()

        val error = assertIs<BraintreeException>(assertIs<DeviceDataResult.Failed>(result).error)
        assertEquals(CardinalThreeDSecureProvider.CARDINAL_SETUP_ERROR_MESSAGE, error.message)
        assertSame(cause, error.cause)
    }

    @Test
    fun `collectDeviceData returns Failed when the configuration can't be fetched`() = runTest {
        val configurationError = IOException("No configuration")
        val sut = CardinalThreeDSecureProvider(
            context,
            MockkBraintreeClientBuilder().configurationError(configurationError).build(),
        )

        val result = sut.collectDeviceData()

        assertSame(configurationError, assertIs<DeviceDataResult.Failed>(result).error)
        verify(exactly = 0) { cardinal.configure(any(), any()) }
    }
}
