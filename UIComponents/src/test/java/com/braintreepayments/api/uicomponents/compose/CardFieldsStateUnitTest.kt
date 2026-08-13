package com.braintreepayments.api.uicomponents.compose

import androidx.compose.ui.text.TextRange
import com.braintreepayments.api.uicomponents.cardfields.CardFieldsViewModel
import org.junit.Assert.assertEquals
import org.junit.Test

class CardFieldsStateUnitTest {

    @Test
    fun `cardNumber initializes empty with cursor at the start when the view model has no value`() {
        val state = CardFieldsState(CardFieldsViewModel())
        assertEquals("", state.cardNumber.value.text)
        assertEquals(TextRange(0), state.cardNumber.value.selection)
    }

    @Test
    fun `cardNumber initializes from the view model's current card number with cursor at the end`() {
        val viewModel = CardFieldsViewModel()
        viewModel.onCardNumberChanged("4111")

        val state = CardFieldsState(viewModel)

        assertEquals("4111", state.cardNumber.value.text)
        assertEquals(TextRange(4), state.cardNumber.value.selection)
    }

    @Test
    fun `expiration initializes empty with cursor at the start when the view model has no value`() {
        val state = CardFieldsState(CardFieldsViewModel())
        assertEquals("", state.expiration.value.text)
        assertEquals(TextRange(0), state.expiration.value.selection)
    }

    @Test
    fun `expiration initializes from the view model's current expiration with cursor at the end`() {
        val viewModel = CardFieldsViewModel()
        viewModel.onExpiryChanged("1225")

        val state = CardFieldsState(viewModel)

        assertEquals("1225", state.expiration.value.text)
        assertEquals(TextRange(4), state.expiration.value.selection)
    }
}
