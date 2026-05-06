package com.braintreepayments.api.uicomponents.cardfields

import android.content.Context
import android.text.InputType
import android.widget.EditText
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import com.braintreepayments.api.uicomponents.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CardNumberTextInputViewUnitTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `constructor sets input type to number`() {
        val view = CardNumberTextInputView(context)

        val editText = view.findViewById<EditText>(R.id.text_input_edit_text)
        assertEquals(InputType.TYPE_CLASS_NUMBER, editText.inputType)
    }

    @Test
    fun `constructor sets card number hint`() {
        val view = CardNumberTextInputView(context)

        val editText = view.findViewById<EditText>(R.id.text_input_edit_text)
        assertEquals("Card number", editText.contentDescription.toString())
    }

    @Test
    fun `constructor sets default card icon`() {
        val view = CardNumberTextInputView(context)

        val leadingIcon = findLeadingIconView(view)
        assertNotNull(leadingIcon)
        assertEquals("Unknown Card Brand", leadingIcon?.contentDescription.toString())
    }

    @Test
    fun `setCardIcon updates icon content description`() {
        val view = CardNumberTextInputView(context)

        view.setCardIcon(R.drawable.card_fields_cc_visa, "Visa")

        val leadingIcon = findLeadingIconView(view)
        assertEquals("Visa", leadingIcon?.contentDescription.toString())
    }

    @Test
    fun `getRawCardNumber returns empty string when no text`() {
        val view = CardNumberTextInputView(context)

        assertEquals("", view.getRawCardNumber())
    }

    @Test
    fun `getRawCardNumber returns digits without spaces`() {
        val view = CardNumberTextInputView(context)

        view.setText("4111111111111111")

        val raw = view.getRawCardNumber()
        assertEquals("4111111111111111", raw)
    }

    @Test
    fun `currentBrand is UNKNOWN initially`() {
        val view = CardNumberTextInputView(context)
        assertEquals(CardBrand.UNKNOWN, view.currentBrand)
    }

    @Test
    fun `typing Visa prefix updates currentBrand to VISA`() {
        val view = CardNumberTextInputView(context)

        view.setText("4111")

        assertEquals(CardBrand.VISA, view.currentBrand)
    }

    @Test
    fun `clearing text resets currentBrand to UNKNOWN`() {
        val view = CardNumberTextInputView(context)

        view.setText("4111")
        assertEquals(CardBrand.VISA, view.currentBrand)

        view.setText("")
        assertEquals(CardBrand.UNKNOWN, view.currentBrand)
    }

    @Test
    fun `typing Visa prefix updates icon to Visa`() {
        val view = CardNumberTextInputView(context)

        view.setText("4111")

        val leadingIcon = findLeadingIconView(view)
        assertEquals("Visa", leadingIcon?.contentDescription.toString())
    }

    @Test
    fun `clearing text resets icon to unknown`() {
        val view = CardNumberTextInputView(context)

        view.setText("4111")
        view.setText("")

        val leadingIcon = findLeadingIconView(view)
        assertEquals("Unknown Card Brand", leadingIcon?.contentDescription.toString())
    }

    @Test
    fun `ambiguous prefix shows unknown icon`() {
        val view = CardNumberTextInputView(context)

        view.setText("622")

        val leadingIcon = findLeadingIconView(view)
        assertEquals("Unknown Card Brand", leadingIcon?.contentDescription.toString())
    }

    @Test
    fun `cardBrandChangeListener is called when brand changes`() {
        val view = CardNumberTextInputView(context)
        val brands = mutableListOf<CardBrand>()
        view.cardBrandChangeListener =
            CardNumberTextInputView.CardBrandChangeListener { brands.add(it) }

        view.setText("4111")

        assertEquals(listOf(CardBrand.VISA), brands)
    }

    @Test
    fun `cardBrandChangeListener is not called when brand stays the same`() {
        val view = CardNumberTextInputView(context)
        view.setText("4")
        val brands = mutableListOf<CardBrand>()
        view.cardBrandChangeListener =
            CardNumberTextInputView.CardBrandChangeListener { brands.add(it) }

        view.setText("41")

        assertEquals(emptyList<CardBrand>(), brands)
    }

    @Test
    fun `full Visa number is formatted with spaces`() {
        val view = CardNumberTextInputView(context)

        view.setText("4111111111111111")

        val displayed = view.getText()?.toString()
        assertEquals("4111 1111 1111 1111", displayed)
    }

    private fun findLeadingIconView(parent: CardNumberTextInputView): ImageView? {
        val container = parent.findViewById<android.widget.FrameLayout>(R.id.input_container)
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child is ImageView) return child
        }
        return null
    }
}
