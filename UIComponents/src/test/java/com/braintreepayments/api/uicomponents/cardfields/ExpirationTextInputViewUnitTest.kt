package com.braintreepayments.api.uicomponents.cardfields

import android.content.Context
import android.text.InputType
import android.widget.EditText
import androidx.test.core.app.ApplicationProvider
import com.braintreepayments.api.uicomponents.R
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExpirationTextInputViewUnitTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `constructor sets input type to number`() {
        val view = ExpirationTextInputView(context)

        val editText = view.findViewById<EditText>(R.id.text_input_edit_text)
        assertEquals(InputType.TYPE_CLASS_NUMBER, editText.inputType)
    }

    @Test
    fun `constructor sets expiration hint`() {
        val view = ExpirationTextInputView(context)

        val editText = view.findViewById<EditText>(R.id.text_input_edit_text)
        assertEquals("Expiration (MM/YY)", editText.contentDescription.toString())
    }

    @Test
    fun `getRawExpiration returns empty string when no text`() {
        val view = ExpirationTextInputView(context)

        assertEquals("", view.getRawExpiration())
    }

    @Test
    fun `getRawExpiration returns only digits without slash`() {
        val view = ExpirationTextInputView(context)

        view.setText("1234")

        assertEquals("1234", view.getRawExpiration())
    }

    @Test
    fun `typing 0 as first digit does not auto-prefix`() {
        val view = ExpirationTextInputView(context)

        view.setText("0")

        assertEquals("0", view.getText().toString())
    }

    @Test
    fun `typing 1 as first digit does not auto-prefix`() {
        val view = ExpirationTextInputView(context)

        view.setText("1")

        assertEquals("1", view.getText().toString())
    }

    @Test
    fun `typing 2 as first digit auto-prefixes with leading zero`() {
        val view = ExpirationTextInputView(context)

        view.setText("2")

        assertEquals("02", view.getText().toString())
    }

    @Test
    fun `typing 9 as first digit auto-prefixes with leading zero`() {
        val view = ExpirationTextInputView(context)

        view.setText("9")

        assertEquals("09", view.getText().toString())
    }

    @Test
    fun `typing two digits shows no slash`() {
        val view = ExpirationTextInputView(context)

        view.setText("12")

        assertEquals("12", view.getText().toString())
    }

    @Test
    fun `typing three digits auto-inserts slash`() {
        val view = ExpirationTextInputView(context)

        view.setText("123")

        assertEquals("12/3", view.getText().toString())
    }

    @Test
    fun `typing four digits formats as MM slash YY`() {
        val view = ExpirationTextInputView(context)

        view.setText("1234")

        assertEquals("12/34", view.getText().toString())
    }

    @Test
    fun `input is capped at four digits`() {
        val view = ExpirationTextInputView(context)

        view.setText("123456")

        assertEquals("12/34", view.getText().toString())
    }

    @Test
    fun `invalid month 00 rejects second digit`() {
        val view = ExpirationTextInputView(context)

        view.setText("00")

        assertEquals("0", view.getText().toString())
    }

    @Test
    fun `invalid month 13 rejects second digit`() {
        val view = ExpirationTextInputView(context)

        view.setText("13")

        assertEquals("1", view.getText().toString())
    }

    @Test
    fun `invalid month 19 rejects second digit`() {
        val view = ExpirationTextInputView(context)

        view.setText("19")

        assertEquals("1", view.getText().toString())
    }

    @Test
    fun `valid month 12 is accepted`() {
        val view = ExpirationTextInputView(context)

        view.setText("12")

        assertEquals("12", view.getText().toString())
    }

    @Test
    fun `valid month 01 is accepted`() {
        val view = ExpirationTextInputView(context)

        view.setText("01")

        assertEquals("01", view.getText().toString())
    }

    @Test
    fun `clearing text resets to empty`() {
        val view = ExpirationTextInputView(context)

        view.setText("1234")
        view.setText("")

        assertEquals("", view.getText().toString())
    }
}
