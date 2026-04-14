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

        view.setCardIcon(R.drawable.card_fields_visa_cc, "Visa")

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
        assertEquals(raw, raw.filter { it.isDigit() })
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
