package com.braintreepayments.api.uicomponents.cardfields

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
    fun `setHint does not throw`() {
        val view = BaseTextInputView(context)

        view.setHint("Card number")
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
    fun `setError does not throw`() {
        val view = BaseTextInputView(context)

        view.setError("Invalid card number")
    }

    @Test
    fun `setError with null does not throw`() {
        val view = BaseTextInputView(context)
        view.setError("Invalid card number")

        view.setError(null)
    }
}
