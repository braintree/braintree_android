package com.braintreepayments.api.uicomponents.compose

import androidx.compose.ui.text.TextRange
import com.braintreepayments.api.uicomponents.cardfields.CardFieldsViewModel
import org.junit.Assert.assertEquals
import org.junit.Test

class CardFieldsControllerUnitTest {

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
}
