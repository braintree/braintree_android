package com.braintreepayments.api.uicomponents.compose

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CardFieldsControllerUnitTest {

    private val cardClient: CardClient = mockk(relaxed = true)

    private fun String.asTextFieldValue() = TextFieldValue(text = this, selection = TextRange(length))

    private fun createCardFieldsController(
        viewModel: CardFieldsViewModel = CardFieldsViewModel(),
        request: Card = Card(),
    ) =
        CardFieldsController(
            viewModel,
            mutableStateOf(viewModel.currentCardNumber.asTextFieldValue()),
            mutableStateOf(viewModel.currentExpiration.asTextFieldValue()),
            mutableStateOf(viewModel.currentCvv.asTextFieldValue()),
            cardClient,
            request
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
    fun `submit maps a CardResult Success to a CardFieldsResult Success`() {
        val nonce = mockk<CardNonce>()
        val successResult = mockk<CardResult.Success>()
        every { successResult.nonce } returns nonce
        every { cardClient.tokenize(any(), any()) } answers {
            secondArg<CardTokenizeCallback>().onCardResult(successResult)
        }
        val controller = createCardFieldsController()
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
        val controller = createCardFieldsController()
        var result: CardFieldsResult? = null

        controller.submit { result = it }

        assertTrue(result is CardFieldsResult.Failure)
        assertEquals(error, (result as CardFieldsResult.Failure).error)
    }

    @Test
    fun `submit tokenizes the user-entered card fields`() {
        val cardSlot = slot<Card>()
        every { cardClient.tokenize(capture(cardSlot), any()) } just Runs
        val controller = createCardFieldsController()
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
        val controller = createCardFieldsController(
            request = Card(cardholderName = "Jane Doe", postalCode = "94107", number = "0000")
        )
        controller.cardNumber.value = controller.cardNumber.value.copy(text = "4111111111111111")
        controller.expiration.value = controller.expiration.value.copy(text = "1226")
        controller.cvv.value = controller.cvv.value.copy(text = "123")

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
        val controller = createCardFieldsController()
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
}
