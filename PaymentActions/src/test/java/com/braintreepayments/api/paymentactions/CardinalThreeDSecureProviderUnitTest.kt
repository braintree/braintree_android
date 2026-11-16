package com.braintreepayments.api.paymentactions

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import com.cardinalcommerce.cardinalmobilesdk.Cardinal
import com.cardinalcommerce.cardinalmobilesdk.enums.CardinalEnvironment
import com.cardinalcommerce.cardinalmobilesdk.enums.CardinalUiType
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalActionCode
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalChallengeObserver
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalConfigurationParameters
import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse
import com.cardinalcommerce.cardinalmobilesdk.services.CardinalInitService
import com.cardinalcommerce.cardinalmobilesdk.services.CardinalValidateReceiver
import com.cardinalcommerce.shared.models.exceptions.InvalidInputException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
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
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CardinalThreeDSecureProviderUnitTest {

    private val context = mockk<Context>(relaxed = true)
    private val cardinal = mockk<Cardinal>(relaxed = true)
    private val challengePresenter = mockk<ChallengePresenter>()
    private val activity = mockk<FragmentActivity>()
    private val observer = mockk<CardinalChallengeObserver>()
    private var observerActivity: FragmentActivity? = null
    private var receiver: CardinalValidateReceiver? = null

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
        challengePresenter,
        createObserver = { activity, receiver ->
            observerActivity = activity
            this.receiver = receiver
            observer
        },
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
    fun `collectDeviceData returns Failed with Cardinal's error description when one is provided`() = runTest {
        val validateResponse = mockk<ValidateResponse> {
            every { errorDescription } returns "Invalid JWT signature"
        }
        every { cardinal.init(any(), any<CardinalInitService>()) } answers {
            secondArg<CardinalInitService>().onValidated(validateResponse, null)
        }

        val result = buildSut().collectDeviceData()

        val error = assertIs<BraintreeException>(assertIs<DeviceDataResult.Failed>(result).error)
        assertEquals("Invalid JWT signature", error.message)
    }

    @Test
    fun `collectDeviceData falls back to the default message when Cardinal's error description is blank`() =
        runTest {
            val validateResponse = mockk<ValidateResponse> {
                every { errorDescription } returns "  "
            }
            every { cardinal.init(any(), any<CardinalInitService>()) } answers {
                secondArg<CardinalInitService>().onValidated(validateResponse, null)
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

    @Test
    fun `presentChallenge sets up Cardinal, then presents the challenge with itself as the registrar`() = runTest {
        stubSetupCompleted()
        val challengeResult = ChallengeResult.Completed("server-jwt")
        coEvery { challengePresenter.present(any(), any(), any()) } returns challengeResult
        val sut = buildSut()

        val result = sut.presentChallenge("mpi-tx-id", "challenge-payload")

        assertSame(challengeResult, result)
        coVerifyOrder {
            cardinal.configure(context, any())
            cardinal.init("cardinal-jwt", any<CardinalInitService>())
            challengePresenter.present("mpi-tx-id", "challenge-payload", sut)
        }
    }

    @Test
    fun `presentChallenge with a missing JWT returns Failed without presenting the challenge`() = runTest {
        val result = buildSut(configuration(jwt = null)).presentChallenge("mpi-tx-id", "challenge-payload")

        val error = assertIs<BraintreeException>(assertIs<ChallengeResult.Failed>(result).error)
        assertEquals(CardinalThreeDSecureProvider.MISSING_CARDINAL_JWT_MESSAGE, error.message)
        coVerify(exactly = 0) { challengePresenter.present(any(), any(), any()) }
    }

    @Test
    fun `presentChallenge returns Failed without presenting the challenge when setup fails`() = runTest {
        every { cardinal.init(any(), any<CardinalInitService>()) } answers {
            secondArg<CardinalInitService>().onValidated(null, null)
        }

        val result = buildSut().presentChallenge("mpi-tx-id", "challenge-payload")

        val error = assertIs<BraintreeException>(assertIs<ChallengeResult.Failed>(result).error)
        assertEquals(CardinalThreeDSecureProvider.CARDINAL_SETUP_VALIDATION_FAILED_MESSAGE, error.message)
        coVerify(exactly = 0) { challengePresenter.present(any(), any(), any()) }
    }

    @Test
    fun `the challenge watchdog outlasts Cardinal's own challenge timeout`() {
        assertTrue(
            CardinalThreeDSecureProvider.CHALLENGE_WATCHDOG_TIMEOUT.inWholeMinutes >
                CardinalThreeDSecureProvider.CHALLENGE_TIMEOUT_MINUTES
        )
    }

    private fun map(
        actionCode: CardinalActionCode?,
        serverJwt: String? = "server-jwt",
        errorDescription: String? = null,
    ) = CardinalThreeDSecureProvider.toChallengeResult(actionCode, serverJwt, errorDescription)

    private fun ChallengeResult.errorMessage(): String? =
        assertIs<BraintreeException>(assertIs<ChallengeResult.Failed>(this).error).message

    @Test
    fun `SUCCESS, NOACTION and FAILURE with a server JWT complete with that JWT`() {
        listOf(CardinalActionCode.SUCCESS, CardinalActionCode.NOACTION, CardinalActionCode.FAILURE).forEach {
            assertEquals(ChallengeResult.Completed("server-jwt"), map(it))
        }
    }

    @Test
    fun `SUCCESS, NOACTION and FAILURE without a server JWT fail`() {
        listOf(CardinalActionCode.SUCCESS, CardinalActionCode.NOACTION, CardinalActionCode.FAILURE).forEach { code ->
            listOf(null, "").forEach { jwt ->
                val message = map(code, serverJwt = jwt).errorMessage()

                assertEquals(CardinalThreeDSecureProvider.MISSING_SERVER_JWT_MESSAGE, message)
            }
        }
    }

    @Test
    fun `CANCEL and TIMEOUT mean the customer abandoned the challenge`() {
        assertSame(ChallengeResult.Abandoned, map(CardinalActionCode.CANCEL, serverJwt = null))
        assertSame(ChallengeResult.Abandoned, map(CardinalActionCode.TIMEOUT, serverJwt = null))
    }

    @Test
    fun `ERROR fails with Cardinal's error description`() {
        val result = map(CardinalActionCode.ERROR, errorDescription = "Invalid transition")

        assertEquals("Invalid transition", result.errorMessage())
    }

    @Test
    fun `ERROR without an error description fails with the default message`() {
        listOf(null, " ").forEach { description ->
            val result = map(CardinalActionCode.ERROR, errorDescription = description)

            assertEquals(CardinalThreeDSecureProvider.CHALLENGE_ERROR_MESSAGE, result.errorMessage())
        }
    }

    @Test
    fun `a missing action code fails as unrecognized`() {
        val message = map(actionCode = null).errorMessage()

        assertTrue(message!!.startsWith(CardinalThreeDSecureProvider.UNRECOGNIZED_ACTION_CODE_MESSAGE))
    }

    @Test
    fun `register builds Cardinal's observer on the shell activity`() {
        buildSut().register(activity) {}

        assertSame(activity, observerActivity)
    }

    @Test
    fun `the starter continues the challenge with Cardinal through the registered observer`() {
        buildSut().register(activity) {}.start("mpi-tx-id", "challenge-payload")

        verify { cardinal.cca_continue("mpi-tx-id", "challenge-payload", observer) }
    }

    @Test
    fun `the starter throws when Cardinal rejects the challenge`() {
        val cause = InvalidInputException("invalid payload", Throwable())
        every { cardinal.cca_continue(any(), any(), any<CardinalChallengeObserver>()) } throws cause

        val starter = buildSut().register(activity) {}

        assertSame(cause, assertFailsWith<InvalidInputException> { starter.start("mpi-tx-id", "challenge-payload") })
    }

    @Test
    fun `Cardinal's verdict reaches onResult as a ChallengeResult`() {
        var result: ChallengeResult? = null
        buildSut().register(activity) { result = it }
        val validateResponse = mockk<ValidateResponse> {
            every { actionCode } returns CardinalActionCode.SUCCESS
            every { errorDescription } returns null
        }

        receiver!!.onValidated(mockk<Context>(), validateResponse, "server-jwt")

        assertEquals(ChallengeResult.Completed("server-jwt"), result)
    }

    @Test
    fun `a verdict without a ValidateResponse reaches onResult as a failure`() {
        var result: ChallengeResult? = null
        buildSut().register(activity) { result = it }

        receiver!!.onValidated(mockk<Context>(), null, null)

        assertIs<ChallengeResult.Failed>(result)
    }
}
