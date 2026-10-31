package com.braintreepayments.api.uicomponents.cardfields

import android.content.Context
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.ImageView
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.braintreepayments.api.uicomponents.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

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

    // Hint animates on text change; advance the main looper past the 200ms animation
    private fun finishHintAnimation() = ShadowLooper.idleMainLooper(500, TimeUnit.MILLISECONDS)

    private fun BaseTextInputView.layoutAt(width: Int = 800, height: Int = 300) {
        measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST)
        )
        layout(0, 0, measuredWidth, measuredHeight)
    }

    @Test
    fun `focus change invokes focusChangeCallback with focus state`() {
        val view = BaseTextInputView(context)
        val states = mutableListOf<Boolean>()
        view.focusChangeCallback = { states.add(it) }

        view.editText.onFocusChangeListener.onFocusChange(view.editText, true)
        view.editText.onFocusChangeListener.onFocusChange(view.editText, false)

        assertEquals(listOf(true, false), states)
    }

    @Test
    fun `setError with the same message is a no-op`() {
        val view = BaseTextInputView(context)
        view.setError("Invalid")
        val errorLabel = view.findViewById<TextView>(R.id.error_label)
        errorLabel.text = "changed"

        view.setError("Invalid")

        assertEquals("changed", errorLabel.text.toString())
    }

    @Test
    fun `accessibility node info exposes current error`() {
        val view = BaseTextInputView(context)
        view.setError("Invalid card number")

        val info = AccessibilityNodeInfo.obtain()
        view.editText.onInitializeAccessibilityNodeInfo(info)

        assertEquals("Invalid card number", info.error.toString())
    }

    @Test
    fun `accessibility node info has no error when none is set`() {
        val view = BaseTextInputView(context)

        val info = AccessibilityNodeInfo.obtain()
        view.editText.onInitializeAccessibilityNodeInfo(info)

        assertNull(info.error)
    }

    @Test
    fun `hint floats up when text is entered and returns when cleared`() {
        val view = BaseTextInputView(context)
        view.layoutAt()
        assertEquals(0f, view.hintLabel.translationY, 0f)

        view.setText("4111")
        finishHintAnimation()
        assertNotEquals(0f, view.hintLabel.translationY)

        view.setText("")
        finishHintAnimation()
        assertEquals(0f, view.hintLabel.translationY, 0f)
    }

    @Test
    fun `hint shrinks when floating`() {
        val view = BaseTextInputView(context)
        view.layoutAt()
        val restSize = view.hintLabel.textSize

        view.setText("4111")
        finishHintAnimation()

        assertTrue(view.hintLabel.textSize < restSize)
    }

    @Test
    fun `setCardBrandIcon adds a single leading icon and reuses it on later calls`() {
        val view = BaseTextInputView(context)
        val childrenBefore = view.inputContainer.childCount
        val paddingBefore = view.editText.paddingStart

        view.setCardBrandIcon(R.drawable.card_fields_cc_amex, "Visa")
        view.setCardBrandIcon(R.drawable.card_fields_cc_amex, "Mastercard")

        assertEquals(childrenBefore + 1, view.inputContainer.childCount)
        val icon = view.inputContainer.getChildAt(view.inputContainer.childCount - 1) as ImageView
        assertEquals("Mastercard", icon.contentDescription.toString())
        assertTrue(view.editText.paddingStart > paddingBefore)
    }

    @Test
    fun `setCvvHintIcon adds a trailing icon and pads the end of the edit text`() {
        val view = BaseTextInputView(context)
        val childrenBefore = view.inputContainer.childCount
        val paddingBefore = view.editText.paddingEnd

        view.setCvvHintIcon(R.drawable.card_fields_cc_amex, "CVV hint")
        view.setCvvHintIcon(R.drawable.card_fields_cc_amex, "CVV hint 2")

        assertEquals(childrenBefore + 1, view.inputContainer.childCount)
        val icon = view.inputContainer.getChildAt(view.inputContainer.childCount - 1) as ImageView
        assertEquals("CVV hint 2", icon.contentDescription.toString())
        assertTrue(view.editText.paddingEnd > paddingBefore)
    }

    @Test
    fun `setCvvHintClickListener wires the trailing icon click`() {
        val view = BaseTextInputView(context)
        view.setCvvHintIcon(R.drawable.card_fields_cc_amex, "CVV hint")
        var clicked = false

        view.setCvvHintClickListener { clicked = true }
        view.inputContainer.getChildAt(view.inputContainer.childCount - 1).performClick()

        assertTrue(clicked)
    }

    @Test
    fun `setInputType updates the edit text input type`() {
        val view = BaseTextInputView(context)

        view.setInputType(android.text.InputType.TYPE_CLASS_NUMBER)

        assertEquals(android.text.InputType.TYPE_CLASS_NUMBER, view.editText.inputType)
    }

    @Test
    fun `text watchers can be added and removed`() {
        val view = BaseTextInputView(context)
        var changes = 0
        val watcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                changes++
            }
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        }

        view.addTextChangedListener(watcher)
        view.setText("1")
        view.removeTextChangedListener(watcher)
        view.setText("12")

        assertEquals(1, changes)
    }
}
