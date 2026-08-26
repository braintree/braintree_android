package com.braintreepayments.api.uicomponents.compose

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.TextRange
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.text.input.TextFieldValue
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
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CardFieldsControllerUnitTest {

    private val cardClient: CardClient = mockk(relaxed = true)
    private val analyticsClient: AnalyticsClient = mockk(relaxed = true)

    private fun String.asTextFieldValue() = TextFieldValue(text = this, selection = TextRange(length))

    private fun createCardFieldsController(viewModel: CardFieldsViewModel = CardFieldsViewModel()) =
        CardFieldsController(
            viewModel,
            mutableStateOf(viewModel.currentCardNumber.asTextFieldValue()),
            mutableStateOf(viewModel.currentExpiration.asTextFieldValue()),
            mutableStateOf(viewModel.currentCvv.asTextFieldValue()),
            analyticsClient = analyticsClient
        )

    private fun createCardFieldsControllerWithClient(viewModel: CardFieldsViewModel = CardFieldsViewModel()) =
        CardFieldsController(
            viewModel,
            mutableStateOf(viewModel.currentCardNumber.asTextFieldValue()),
            mutableStateOf(viewModel.currentExpiration.asTextFieldValue()),
            mutableStateOf(viewModel.currentCvv.asTextFieldValue()),
            cardClient,
            analyticsClient
        )

    @Test
    fun `cardNumber initializes empty with cursor at the start when the view model has no value`() {
        val controller = createCardFieldsController()
        assertEquals("", controller.cardNumber.value.text)
        assertEquals(TextRange(0), controller.cardNumber.value.selection)
    }

    @Test
    fun `cardNumber initializes from the view model's current card number with cursor at the end`() {
        val viewModel = CardFieldsViewModel()
        viewModel.onCardNumberChanged("4111")

        val controller = createCardFieldsController(viewModel)

        assertEquals("4111", controller.cardNumber.value.text)
        assertEquals(TextRange(4), controller.cardNumber.value.selection)
    }

    @Test
    fun `expiration initializes empty with cursor at the start when the view model has no value`() {
        val controller = createCardFieldsController()
        assertEquals("", controller.expiration.value.text)
        assertEquals(TextRange(0), controller.expiration.value.selection)
    }

    @Test
    fun `expiration initializes from the view model's current expiration with cursor at the end`() {
        val viewModel = CardFieldsViewModel()
        viewModel.onExpiryChanged("1225")

        val controller = createCardFieldsController(viewModel)

        assertEquals("1225", controller.expiration.value.text)
        assertEquals(TextRange(4), controller.expiration.value.selection)
    }

    @Test
    fun `cvv initializes empty with cursor at the start when the view model has no value`() {
        val controller = createCardFieldsController()
        assertEquals("", controller.cvv.value.text)
        assertEquals(TextRange(0), controller.cvv.value.selection)
    }

    @Test
    fun `cvv initializes from the view model's current cvv with cursor at the end`() {
        val viewModel = CardFieldsViewModel()
        viewModel.onCvvChanged("123")

        val controller = createCardFieldsController(viewModel)

        assertEquals("123", controller.cvv.value.text)
        assertEquals(TextRange(3), controller.cvv.value.selection)
    }

    // region isFormValid

    @Test
    fun `isFormValid reflects the view model's isFormValid flow`() {
        val viewModel = CardFieldsViewModel()
        val controller = createCardFieldsController(viewModel)

        assertEquals(viewModel.isFormValid, controller.isFormValid)
    }

    // endregion

    // region Tokenization

    @Test
    fun `submit before initialize delivers a Failure to the callback and does not tokenize`() {
        // No client injected and initialize() never called, so cardClient is null.
        val controller = createCardFieldsController()
        var result: CardFieldsResult? = null

        controller.submit { result = it }

        assertTrue(result is CardFieldsResult.Failure)
        assertTrue((result as CardFieldsResult.Failure).error is BraintreeException)

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
        val controller = createCardFieldsControllerWithClient()
        var result: CardFieldsResult? = null

        controller.submit { result = it }

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
        val controller = createCardFieldsControllerWithClient()
        var result: CardFieldsResult? = null

        controller.submit { result = it }

        assertTrue(result is CardFieldsResult.Failure)
        assertEquals(error, (result as CardFieldsResult.Failure).error)
    }

    @Test
    fun `submit tokenizes the user-entered card fields`() {
        val cardSlot = slot<Card>()
        every { cardClient.tokenize(capture(cardSlot), any()) } just Runs
        val controller = createCardFieldsControllerWithClient()
        controller.cardNumber.value = controller.cardNumber.value.copy(text = "4111111111111111")
        controller.expiration.value = controller.expiration.value.copy(text = "1226")
        controller.cvv.value = controller.cvv.value.copy(text = "123")

        controller.submit { }

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
        val controller = createCardFieldsControllerWithClient()
        controller.cardNumber.value = controller.cardNumber.value.copy(text = "4111111111111111")
        controller.expiration.value = controller.expiration.value.copy(text = "1226")
        controller.cvv.value = controller.cvv.value.copy(text = "123")
        // The merchant card carries metadata AND a number the user did not type.
        controller.setPaymentRequest(
            Card(cardholderName = "Jane Doe", postalCode = "94107", number = "0000")
        )

        controller.submit { }

        val captured = cardSlot.captured
        // UI-entered number overrides the merchant-supplied "0000".
        assertEquals("4111111111111111", captured.number)
        // Merchant-only metadata is preserved by copy().
        assertEquals("Jane Doe", captured.cardholderName)
        assertEquals("94107", captured.postalCode)
    }

    @Test
    fun `submit without a payment request still tokenizes the UI fields`() {
        val cardSlot = slot<Card>()
        every { cardClient.tokenize(capture(cardSlot), any()) } just Runs
        val controller = createCardFieldsControllerWithClient()
        controller.cardNumber.value = controller.cardNumber.value.copy(text = "4111111111111111")
        controller.expiration.value = controller.expiration.value.copy(text = "1226")
        controller.cvv.value = controller.cvv.value.copy(text = "123")

        controller.submit { }

        val captured = cardSlot.captured
        assertEquals("4111111111111111", captured.number)
        // No payment request was set, so metadata fields fall back to the empty Card() defaults.
        assertNull(captured.cardholderName)
        assertNull(captured.postalCode)
    }

    // endregion

    // region Analytics

    @Test
    fun `initialize sends the card fields presented event`() {
        val controller = createCardFieldsController()

        controller.initialize(ApplicationProvider.getApplicationContext(), "fake-authorization")

        verify {
            analyticsClient.sendEvent(
                UIComponentsAnalytics.CARD_FIELDS_PRESENTED,
                AnalyticsEventParams(uiType = UIComponentsAnalytics.UI_TYPE_COMPOSE)
            )
        }
    }

    @Test
    fun `initialize does not resend the presented event on a subsequent controller sharing the same flag`() {
        val viewModel = CardFieldsViewModel()
        val shouldSendPresentedEvent = mutableStateOf(true)
        val firstController = CardFieldsController(
            viewModel,
            mutableStateOf(viewModel.currentCardNumber.asTextFieldValue()),
            mutableStateOf(viewModel.currentExpiration.asTextFieldValue()),
            mutableStateOf(viewModel.currentCvv.asTextFieldValue()),
            analyticsClient = analyticsClient,
            shouldSendPresentedEvent = shouldSendPresentedEvent
        )
        firstController.initialize(ApplicationProvider.getApplicationContext(), "fake-authorization")

        // Simulates a rotation: a new CardFieldsController is created (as rememberCardFieldsController
        // would do), but the rememberSaveable-backed flag survives and is passed in again.
        val secondController = CardFieldsController(
            viewModel,
            mutableStateOf(viewModel.currentCardNumber.asTextFieldValue()),
            mutableStateOf(viewModel.currentExpiration.asTextFieldValue()),
            mutableStateOf(viewModel.currentCvv.asTextFieldValue()),
            analyticsClient = analyticsClient,
            shouldSendPresentedEvent = shouldSendPresentedEvent
        )
        secondController.initialize(ApplicationProvider.getApplicationContext(), "fake-authorization")

        verify(exactly = 1) {
            analyticsClient.sendEvent(
                UIComponentsAnalytics.CARD_FIELDS_PRESENTED,
                AnalyticsEventParams(uiType = UIComponentsAnalytics.UI_TYPE_COMPOSE)
            )
        }
    }

    @Test
    fun `submit sends the card fields validated event`() {
        val controller = createCardFieldsControllerWithClient()

        controller.submit { }

        verify {
            analyticsClient.sendEvent(
                UIComponentsAnalytics.CARD_FIELDS_VALIDATED,
                AnalyticsEventParams(uiType = UIComponentsAnalytics.UI_TYPE_COMPOSE)
            )
        }
    }

    @Test
    fun `submit before initialize does not send the validated event`() {
        val controller = createCardFieldsController()

        controller.submit { }

        verify(exactly = 0) {
            analyticsClient.sendEvent(UIComponentsAnalytics.CARD_FIELDS_VALIDATED, any())
        }
    }

    // endregion
}
