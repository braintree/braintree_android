package com.braintreepayments.api.uicomponents.compose

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.core.app.ApplicationProvider
import com.braintreepayments.api.card.CardClient
import com.braintreepayments.api.core.AnalyticsClient
import com.braintreepayments.api.uicomponents.R
import com.braintreepayments.api.uicomponents.cardfields.CardFieldsViewModel
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CardFieldsCvvTruncationUnitTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val analyticsClient: AnalyticsClient = mockk(relaxed = true)

    private fun String.asTextFieldValue() = TextFieldValue(text = this, selection = TextRange(length))

    private fun controllerWith(cardNumber: String, cvv: String): CardFieldsController {
        val viewModel = CardFieldsViewModel()
        viewModel.onCardNumberChanged(cardNumber)
        viewModel.onCvvChanged(cvv)
        return CardFieldsController(
            viewModel,
            mutableStateOf(cardNumber.asTextFieldValue()),
            mutableStateOf("".asTextFieldValue()),
            mutableStateOf(cvv.asTextFieldValue()),
            mockk<CardClient>(relaxed = true),
            analyticsClient = analyticsClient
        )
    }

    private fun cardNumberField() =
        composeRule.onNodeWithContentDescription(context.getString(R.string.card_number_accessibility))

    @Test
    fun `changing from Amex to Visa truncates a 4 digit CVV to 3 digits`() {
        val controller = controllerWith(cardNumber = "378282246310005", cvv = "1234")
        composeRule.setContent { CardFields(controller = controller) }

        cardNumberField().performTextReplacement("4111111111111111")
        composeRule.waitForIdle()

        assertEquals("123", controller.cvv.value.text)
        assertEquals(TextRange(3), controller.cvv.value.selection)
        assertEquals("123", controller.viewModel.currentCvv)
    }

    @Test
    fun `changing between brands with the same CVV length leaves the CVV untouched`() {
        val controller = controllerWith(cardNumber = "4111111111111111", cvv = "123")
        composeRule.setContent { CardFields(controller = controller) }

        cardNumberField().performTextReplacement("5555555555554444")
        composeRule.waitForIdle()

        assertEquals("123", controller.cvv.value.text)
    }

    @Test
    fun `a 4 digit CVV is kept when the card stays Amex`() {
        val controller = controllerWith(cardNumber = "378282246310005", cvv = "1234")
        composeRule.setContent { CardFields(controller = controller) }

        cardNumberField().performTextReplacement("371449635398431")
        composeRule.waitForIdle()

        assertEquals("1234", controller.cvv.value.text)
    }
}
