package com.braintreepayments.api.uicomponents.cardfields

import android.content.Context
import android.content.res.ColorStateList
import android.text.Editable
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.RestrictTo
import androidx.core.content.ContextCompat
import com.braintreepayments.api.uicomponents.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * A base text input view that provides an outlined text field with a floating hint label.
 *
 * Features:
 * - Gray border in the default state
 * - Blue border when focused
 * - Hint text that floats up as a label when the user begins typing
 *
 * This view is intended to be used as a building block for card entry fields
 * (card number, expiration, CVV, etc.).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class BaseTextInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    internal val textInputLayout: TextInputLayout
    internal val editText: TextInputEditText

    init {
        val materialContext = ContextThemeWrapper(
            context,
            com.google.android.material.R.style.Theme_MaterialComponents_Light
        )
        LayoutInflater.from(materialContext).inflate(R.layout.base_text_input, this, true)

        textInputLayout = findViewById(R.id.text_input_layout)
        editText = findViewById(R.id.text_input_edit_text)

        applyBoxStrokeColors()
        applyErrorColors()
    }

    private fun applyBoxStrokeColors() {
        val states = arrayOf(
            intArrayOf(android.R.attr.state_focused),
            intArrayOf()
        )
        val colors = intArrayOf(
            ContextCompat.getColor(context, R.color.card_field_border_focused),
            ContextCompat.getColor(context, R.color.card_field_border_default)
        )
        textInputLayout.setBoxStrokeColorStateList(ColorStateList(states, colors))
    }

    private fun applyErrorColors() {
        val errorColor = ContextCompat.getColor(context, R.color.card_field_error)
        val errorColorStateList = ColorStateList.valueOf(errorColor)
        textInputLayout.setErrorTextColor(errorColorStateList)
        textInputLayout.setErrorIconTintList(errorColorStateList)
        textInputLayout.boxStrokeErrorColor = errorColorStateList
    }

    fun setHint(hint: CharSequence?) {
        textInputLayout.hint = hint
    }

    fun getHint(): CharSequence? = textInputLayout.hint

    fun setText(text: CharSequence?) {
        editText.setText(text)
    }

    fun getText(): Editable? = editText.text

    fun setInputType(type: Int) {
        editText.inputType = type
    }

    fun setError(error: CharSequence?) {
        textInputLayout.error = error
    }

    fun isErrorEnabled(): Boolean = textInputLayout.isErrorEnabled
}