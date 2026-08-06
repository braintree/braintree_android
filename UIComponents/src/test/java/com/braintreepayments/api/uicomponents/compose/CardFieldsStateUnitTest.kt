package com.braintreepayments.api.uicomponents.compose

import android.content.Context
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.core.app.ApplicationProvider
import com.braintreepayments.api.card.Card
import com.braintreepayments.api.card.CardClient
import com.braintreepayments.api.card.CardNonce
import com.braintreepayments.api.card.CardResult
import com.braintreepayments.api.card.CardTokenizeCallback
import com.braintreepayments.api.core.AnalyticsClient
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.uicomponents.UIComponentsAnalytics
import com.braintreepayments.api.uicomponents.cardfields.CardFieldsResult
import com.braintreepayments.api.uicomponents.cardfields.CardFieldsViewModel
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CardFieldsStateUnitTest {

    private val viewModel = CardFieldsViewModel()
    private val cardClient: CardClient = mockk(relaxed = true)
    private val analyticsClient: AnalyticsClient = mockk(relaxed = true)

    private fun createState(withClient: Boolean = true) =
        CardFieldsState(
            viewModel = viewModel,
            cardClient = if (withClient) cardClient else null,
            analyticsClient = analyticsClient
        )

    private fun CardFieldsState.enterCardDetails(
        number: String = "4111111111111111",
        expiration: String = "1226",
        cvv: String = "123"
    ) {
        cardNumber.value = TextFieldValue(number)
        this.expiration.value = TextFieldValue(expiration)
        this.cvv.value = TextFieldValue(cvv)
    }

    @Before
    fun setUp() {
        every { cardClient.tokenize(any(), any()) } just Runs
    }

    // region Tokenization

    @Test
    fun `submit before initialize delivers a Failure to the callback`() {
        val state = createState(withClient = false)
        var result: CardFieldsResult? = null

        state.submit { result = it }

        assertTrue(result is CardFieldsResult.Failure)
        assertTrue((result as CardFieldsResult.Failure).error is BraintreeException)
    }

    @Test
    fun `submit before initialize does not tokenize`() {
        val state = createState(withClient = false)

        state.submit { }

        verify(exactly = 0) { cardClient.tokenize(any(), any()) }
    }

    @Test
    fun `submit maps a CardResult Success to a CardFieldsResult Success`() {
        val nonce = mockk<CardNonce>()
        val successResult = mockk<CardResult.Success>()
        every { successResult.nonce } returns nonce
        every { cardClient.tokenize(any(), any()) } answers {
            secondArg<CardTokenizeCallback>().onCardResult(successResult)
        }
        val state = createState()
        var result: CardFieldsResult? = null

        state.submit { result = it }

        assertTrue(result is CardFieldsResult.Success)
        assertEquals(nonce, (result as CardFieldsResult.Success).nonce)
    }

    @Test
    fun `submit maps a CardResult Failure to a CardFieldsResult Failure`() {
        val error = BraintreeException("tokenization failed")
        val failureResult = mockk<CardResult.Failure>()
        every { failureResult.error } returns error
        every { cardClient.tokenize(any(), any()) } answers {
            secondArg<CardTokenizeCallback>().onCardResult(failureResult)
        }
        val state = createState()
        var result: CardFieldsResult? = null

        state.submit { result = it }

        assertTrue(result is CardFieldsResult.Failure)
        assertEquals(error, (result as CardFieldsResult.Failure).error)
    }

    @Test
    fun `submit tokenizes the user-entered card fields`() {
        val cardSlot = slot<Card>()
        every { cardClient.tokenize(capture(cardSlot), any()) } just Runs
        val state = createState()
        state.enterCardDetails(number = "4111111111111111", expiration = "1226", cvv = "123")

        state.submit { }

        val captured = cardSlot.captured
        assertEquals("4111111111111111", captured.number)
        assertEquals("12", captured.expirationMonth)
        assertEquals("26", captured.expirationYear)
        assertEquals("123", captured.cvv)
    }

    @Test
    fun `submit merges UI fields over the payment request, with all fields correctly preserved`() {
        val cardSlot = slot<Card>()
        every { cardClient.tokenize(capture(cardSlot), any()) } just Runs
        val state = createState()
        state.enterCardDetails()
        state.setPaymentRequest(Card(cardholderName = "Jane Doe", postalCode = "94107", number = "0000"))

        state.submit { }

        val captured = cardSlot.captured
        assertEquals("4111111111111111", captured.number)
        assertEquals("Jane Doe", captured.cardholderName)
        assertEquals("94107", captured.postalCode)
    }

    @Test
    fun `submit without a payment request still tokenizes the UI fields`() {
        val cardSlot = slot<Card>()
        every { cardClient.tokenize(capture(cardSlot), any()) } just Runs
        val state = createState()
        state.enterCardDetails()

        state.submit { }

        val captured = cardSlot.captured
        assertEquals("4111111111111111", captured.number)
        assertNull(captured.cardholderName)
        assertNull(captured.postalCode)
    }

    // endregion

    // region Analytics

    @Test
    fun `initialize sends the card fields presented event`() {
        val state = createState(withClient = false)

        state.initialize(ApplicationProvider.getApplicationContext<Context>(), "fake-authorization")

        verify {
            analyticsClient.sendEvent(
                UIComponentsAnalytics.CARD_FIELDS_PRESENTED,
                AnalyticsEventParams(uiType = UIComponentsAnalytics.UI_TYPE_COMPOSE)
            )
        }
    }

    @Test
    fun `submit sends the card fields validated event`() {
        val state = createState()

        state.submit { }

        verify {
            analyticsClient.sendEvent(
                UIComponentsAnalytics.CARD_FIELDS_VALIDATED,
                AnalyticsEventParams(uiType = UIComponentsAnalytics.UI_TYPE_COMPOSE)
            )
        }
    }

    @Test
    fun `submit before initialize does not send the validated event`() {
        val state = createState(withClient = false)

        state.submit { }

        verify(exactly = 0) {
            analyticsClient.sendEvent(UIComponentsAnalytics.CARD_FIELDS_VALIDATED, any())
        }
    }

    // endregion
}
