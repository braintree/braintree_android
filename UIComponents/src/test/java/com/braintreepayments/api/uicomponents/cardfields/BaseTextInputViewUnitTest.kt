package com.braintreepayments.api.uicomponents.cardfields

import android.content.Context
import android.text.InputType
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.R as MaterialR
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BaseTextInputViewUnitTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        context = ContextThemeWrapper(appContext, MaterialR.style.Theme_MaterialComponents_Light)
    }

    @Test
    fun `constructor inflates text input layout and edit text`() {
        val view = BaseTextInputView(context)

        assertNotNull(view.textInputLayout)
        assertNotNull(view.editText)
    }

    @Test
    fun `setHint sets hint on text input layout`() {
        val view = BaseTextInputView(context)

        view.setHint("Card number")

        assertEquals("Card number", view.getHint())
        assertEquals("Card number", view.textInputLayout.hint)
    }

    @Test
    fun `getHint returns null when no hint is set`() {
        val view = BaseTextInputView(context)

        assertNull(view.getHint())
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
    fun `setInputType sets input type on edit text`() {
        val view = BaseTextInputView(context)

        view.setInputType(InputType.TYPE_CLASS_NUMBER)

        assertEquals(InputType.TYPE_CLASS_NUMBER, view.editText.inputType)
    }

    @Test
    fun `setError sets error on text input layout`() {
        val view = BaseTextInputView(context)

        view.setError("Invalid card number")

        assertEquals("Invalid card number", view.textInputLayout.error)
    }

    @Test
    fun `setError with null clears error`() {
        val view = BaseTextInputView(context)
        view.setError("Invalid card number")

        view.setError(null)

        assertNull(view.textInputLayout.error)
    }
}