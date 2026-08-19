package com.braintreepayments.api.uicomponents.compose

import androidx.compose.ui.text.TextRange
import com.braintreepayments.api.card.Card
import com.braintreepayments.api.card.CardClient
import com.braintreepayments.api.card.CardNonce
import com.braintreepayments.api.card.CardResult
import com.braintreepayments.api.card.CardTokenizeCallback
import com.braintreepayments.api.core.BraintreeException
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

class CardFieldsControllerUnitTest {

    private val cardClient: CardClient = mockk(relaxed = true)

    private fun createCardFieldsController(viewModel: CardFieldsViewModel = CardFieldsViewModel()) =
        CardFieldsController(viewModel)

    private fun createCardFieldsControllerWithClient(viewModel: CardFieldsViewModel = CardFieldsViewModel()) =
        CardFieldsController(viewModel, cardClient)

    @Test
    fun `cardNumber initializes empty with cursor at the start when the view model has no value`() {
        val controller = CardFieldsController(CardFieldsViewModel())
        assertEquals("", controller.cardNumber.value.text)
        assertEquals(TextRange(0), controller.cardNumber.value.selection)
    }

    @Test
    fun `cardNumber initializes from the view model's current card number with cursor at the end`() {
        val viewModel = CardFieldsViewModel()
        viewModel.onCardNumberChanged("4111")

        val controller = CardFieldsController(viewModel)

        assertEquals("4111", controller.cardNumber.value.text)
        assertEquals(TextRange(4), controller.cardNumber.value.selection)
    }

    @Test
    fun `expiration initializes empty with cursor at the start when the view model has no value`() {
        val controller = CardFieldsController(CardFieldsViewModel())
        assertEquals("", controller.expiration.value.text)
        assertEquals(TextRange(0), controller.expiration.value.selection)
    }

    @Test
    fun `expiration initializes from the view model's current expiration with cursor at the end`() {
        val viewModel = CardFieldsViewModel()
        viewModel.onExpiryChanged("1225")

        val controller = CardFieldsController(viewModel)

        assertEquals("1225", controller.expiration.value.text)
        assertEquals(TextRange(4), controller.expiration.value.selection)
    }

    @Test
    fun `cvv initializes empty with cursor at the start when the view model has no value`() {
        val controller = CardFieldsController(CardFieldsViewModel())
        assertEquals("", controller.cvv.value.text)
        assertEquals(TextRange(0), controller.cvv.value.selection)
    }

    @Test
    fun `cvv initializes from the view model's current cvv with cursor at the end`() {
        val viewModel = CardFieldsViewModel()
        viewModel.onCvvChanged("123")

        val controller = CardFieldsController(viewModel)

        assertEquals("123", controller.cvv.value.text)
        assertEquals(TextRange(3), controller.cvv.value.selection)
    }

    // region isFormValid

    @Test
    fun `isFormValid reflects the view model's isFormValid flow`() {
        val viewModel = CardFieldsViewModel()
        val state = createCardFieldsController(viewModel)

        assertEquals(viewModel.isFormValid, state.isFormValid)
    }

    // endregion

    // region Tokenization

    @Test
    fun `submit before initialize delivers a Failure to the callback`() {
        // No client injected and initialize() never called, so cardClient is null.
        val state = createCardFieldsController()
        var result: CardFieldsResult? = null

        state.submit { result = it }

        assertTrue(result is CardFieldsResult.Failure)
        assertTrue((result as CardFieldsResult.Failure).error is BraintreeException)
    }

    @Test
    fun `submit before initialize does not tokenize`() {
        // No client is injected (null cardClient), so submit() hits the pre-init guard and
        // skips tokenization entirely — cardClient.tokenize is never reached.
        val state = createCardFieldsController()

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
        val state = createCardFieldsControllerWithClient()
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
        val state = createCardFieldsControllerWithClient()
        var result: CardFieldsResult? = null

        state.submit { result = it }

        assertTrue(result is CardFieldsResult.Failure)
        assertEquals(error, (result as CardFieldsResult.Failure).error)
    }

    @Test
    fun `submit tokenizes the user-entered card fields`() {
        val cardSlot = slot<Card>()
        every { cardClient.tokenize(capture(cardSlot), any()) } just Runs
        val state = createCardFieldsControllerWithClient()
        state.cardNumber.value = state.cardNumber.value.copy(text = "4111111111111111")
        state.expiration.value = state.expiration.value.copy(text = "1226")
        state.cvv.value = state.cvv.value.copy(text = "123")

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
        val state = createCardFieldsControllerWithClient()
        state.cardNumber.value = state.cardNumber.value.copy(text = "4111111111111111")
        state.expiration.value = state.expiration.value.copy(text = "1226")
        state.cvv.value = state.cvv.value.copy(text = "123")
        // The merchant card carries metadata AND a number the user did not type.
        state.setPaymentRequest(
            Card(cardholderName = "Jane Doe", postalCode = "94107", number = "0000")
        )

        state.submit { }

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
        val state = createCardFieldsControllerWithClient()
        state.cardNumber.value = state.cardNumber.value.copy(text = "4111111111111111")
        state.expiration.value = state.expiration.value.copy(text = "1226")
        state.cvv.value = state.cvv.value.copy(text = "123")

        state.submit { }

        val captured = cardSlot.captured
        assertEquals("4111111111111111", captured.number)
        // No payment request was set, so metadata fields fall back to the empty Card() defaults.
        assertNull(captured.cardholderName)
        assertNull(captured.postalCode)
    }

    // endregion
}
