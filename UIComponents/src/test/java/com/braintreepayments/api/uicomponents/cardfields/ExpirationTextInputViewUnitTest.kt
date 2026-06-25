package com.braintreepayments.api.uicomponents.cardfields

import android.content.Context
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.TextView
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
    fun `constructor sets accessible content description on edit text`() {
        val view = ExpirationTextInputView(context)

        val editText = view.findViewById<EditText>(R.id.text_input_edit_text)
        assertEquals("Expiration date, two digit month and two digit year", editText.contentDescription.toString())
    }

    @Test
    fun `constructor sets visual hint label to MM slash YY format`() {
        val view = ExpirationTextInputView(context)

        assertEquals("Expiration (MM/YY)", view.hintLabel.text.toString())
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
    fun `month 00 is accepted inline, validation handled by ViewModel`() {
        val view = ExpirationTextInputView(context)

        view.setText("00")

        assertEquals("00", view.getText().toString())
    }

    @Test
    fun `month 13 is accepted inline, validation handled by ViewModel`() {
        val view = ExpirationTextInputView(context)

        view.setText("13")

        assertEquals("13", view.getText().toString())
    }

    @Test
    fun `month 19 is accepted inline, validation handled by ViewModel`() {
        val view = ExpirationTextInputView(context)

        view.setText("19")

        assertEquals("19", view.getText().toString())
    }

    @Test
    fun `invalid month does not show an inline error`() {
        val view = ExpirationTextInputView(context)
        view.setText("13")

        val errorLabel = view.findViewById<TextView>(R.id.error_label)
        assertEquals(View.GONE, errorLabel.visibility)
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
