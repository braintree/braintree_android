package com.braintreepayments.api.uicomponents.cardfields

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.braintreepayments.api.uicomponents.R
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BaseTextInputViewUnitTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `constructor inflates view with empty text`() {
        val view = BaseTextInputView(context)

        assertEquals("", view.getText().toString())
    }

    @Test
    fun `setHint sets hint label text`() {
        val view = BaseTextInputView(context)

        view.setHint("Card number")

        val hintLabel = view.findViewById<TextView>(R.id.hint_label)
        assertEquals("Card number", hintLabel.text.toString())
    }

    @Test
    fun `setHint sets editText contentDescription for accessibility`() {
        val view = BaseTextInputView(context)

        view.setHint("Card number")

        val editText = view.findViewById<View>(R.id.text_input_edit_text)
        assertEquals("Card number", editText.contentDescription.toString())
    }

    @Test
    fun `setText sets text on edit text`() {
        val view = BaseTextInputView(context)

        view.setText("4111111111111111")

        assertEquals("4111111111111111", view.getText().toString())
    }

    @Test
    fun `getText returns empty editable when no text is set`() {
        val view = BaseTextInputView(context)

        assertEquals("", view.getText().toString())
    }

    @Test
    fun `setError shows error label with message`() {
        val view = BaseTextInputView(context)

        view.setError("Invalid card number")

        val errorLabel = view.findViewById<TextView>(R.id.error_label)
        assertEquals(View.VISIBLE, errorLabel.visibility)
        assertEquals("Invalid card number", errorLabel.text.toString())
    }

    @Test
    fun `setError with null hides error label`() {
        val view = BaseTextInputView(context)
        view.setError("Invalid card number")

        view.setError(null)

        val errorLabel = view.findViewById<TextView>(R.id.error_label)
        assertEquals(View.GONE, errorLabel.visibility)
    }
}
