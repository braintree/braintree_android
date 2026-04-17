package com.braintreepayments.api.uicomponents.cardfields

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import android.view.accessibility.AccessibilityEvent
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import androidx.core.content.ContextCompat
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.widget.doAfterTextChanged
import com.braintreepayments.api.uicomponents.R

// TODO change class to internal before releasing
@Suppress("TooManyFunctions")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
open class BaseTextInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    internal val editText: EditText
    internal val inputContainer: FrameLayout
    internal val hintLabel: TextView
    private val errorLabel: TextView
    private val borderDrawable: GradientDrawable

    private val cornerRadius: Float
    private val borderWidth: Int
    private val borderFocusedWidth: Int
    private val defaultBorderColor: Int
    private val focusedBorderColor: Int
    private val errorColor: Int
    private val hintRestTextSize: Float
    private val hintFloatTextSize: Float
    private val hintFloatTopMargin: Float

    private val iconWidth: Int
    private val iconHeight: Int
    private val iconMargin: Int
    private val iconCornerRadius: Float
    private val defaultPaddingHorizontal: Int

    private var leadingIconView: ImageView? = null
    private var currentError: CharSequence? = null
    private var isHintFloating: Boolean = false
    private var hintAnimator: AnimatorSet? = null

    private val accessibilityDelegate = object : AccessibilityDelegateCompat() {
        override fun onInitializeAccessibilityNodeInfo(
            host: View,
            info: AccessibilityNodeInfoCompat
        ) {
            super.onInitializeAccessibilityNodeInfo(host, info)
            currentError?.let { info.error = it }
        }
    }

    init {
        orientation = VERTICAL

        LayoutInflater.from(context).inflate(R.layout.base_text_input, this, true)

        inputContainer = findViewById(R.id.input_container)
        hintLabel = findViewById(R.id.hint_label)
        editText = findViewById(R.id.text_input_edit_text)
        errorLabel = findViewById(R.id.error_label)

        cornerRadius = resources.getDimension(R.dimen.card_field_corner_radius)
        borderWidth = resources.getDimensionPixelSize(R.dimen.card_field_border_width)
        borderFocusedWidth = resources.getDimensionPixelSize(R.dimen.card_field_border_focused_width)
        defaultBorderColor = ContextCompat.getColor(context, R.color.card_field_border_default)
        focusedBorderColor = ContextCompat.getColor(context, R.color.card_field_border_focused)
        errorColor = ContextCompat.getColor(context, R.color.card_field_error)
        hintRestTextSize = resources.getDimension(R.dimen.card_field_hint_text_size)
        hintFloatTextSize = resources.getDimension(R.dimen.card_field_hint_float_text_size)
        hintFloatTopMargin = resources.getDimension(R.dimen.card_field_hint_float_top_margin)
        iconWidth = resources.getDimensionPixelSize(R.dimen.card_icon_width)
        iconHeight = resources.getDimensionPixelSize(R.dimen.card_icon_height)
        iconMargin = resources.getDimensionPixelSize(R.dimen.card_icon_margin)
        iconCornerRadius = resources.getDimension(R.dimen.card_icon_corner_radius)
        defaultPaddingHorizontal = resources.getDimensionPixelSize(R.dimen.card_field_padding_horizontal)

        borderDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setCornerRadius(this@BaseTextInputView.cornerRadius)
            setStroke(borderWidth, defaultBorderColor)
            setColor(ContextCompat.getColor(context, R.color.card_field_background))
        }
        inputContainer.background = borderDrawable

        ViewCompat.setAccessibilityDelegate(editText, accessibilityDelegate)

        editText.setOnFocusChangeListener { _, _ ->
            updateBorderState()
            updateHintPosition(animate = true)
        }

        editText.doAfterTextChanged {
            updateHintPosition(animate = true)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        updateHintPosition(animate = false)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        hintAnimator?.cancel()
        hintAnimator = null
    }

    private fun updateBorderState() {
        val hasError = currentError != null
        val hasFocus = editText.hasFocus()

        when {
            hasError -> borderDrawable.setStroke(borderFocusedWidth, errorColor)
            hasFocus -> borderDrawable.setStroke(borderFocusedWidth, focusedBorderColor)
            else -> borderDrawable.setStroke(borderWidth, defaultBorderColor)
        }
        inputContainer.invalidate()
    }

    private fun updateHintPosition(animate: Boolean) {
        val shouldFloat = editText.hasFocus() || !editText.text.isNullOrEmpty()
        if (shouldFloat == isHintFloating) return

        val containerHeight = inputContainer.height
        if (containerHeight == 0) return

        isHintFloating = shouldFloat
        hintAnimator?.cancel()

        val centerY = (containerHeight - hintLabel.height) / 2f
        val targetTranslationY = if (shouldFloat) hintFloatTopMargin - centerY else 0f
        val targetTextSize = if (shouldFloat) hintFloatTextSize else hintRestTextSize

        if (animate) {
            val translationAnim = ObjectAnimator.ofFloat(
                hintLabel, View.TRANSLATION_Y, hintLabel.translationY, targetTranslationY
            )
            val textSizeAnim = ValueAnimator.ofFloat(hintLabel.textSize, targetTextSize).apply {
                addUpdateListener {
                    hintLabel.setTextSize(
                        TypedValue.COMPLEX_UNIT_PX,
                        it.animatedValue as Float
                    )
                }
            }
            hintAnimator = AnimatorSet().apply {
                playTogether(translationAnim, textSizeAnim)
                duration = ANIMATION_DURATION
                start()
            }
        } else {
            hintLabel.translationY = targetTranslationY
            hintLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, targetTextSize)
        }
    }

    fun setHint(hint: CharSequence?) {
        hintLabel.text = hint
        editText.contentDescription = hint
    }

    fun setText(text: CharSequence?) {
        editText.setText(text)
        updateHintPosition(animate = false)
    }

    fun getText(): Editable? = editText.text

    fun setInputType(type: Int) {
        editText.inputType = type
    }

    fun addTextChangedListener(watcher: TextWatcher) {
        editText.addTextChangedListener(watcher)
    }

    fun removeTextChangedListener(watcher: TextWatcher) {
        editText.removeTextChangedListener(watcher)
    }

    fun setError(error: CharSequence?) {
        if (currentError == error) return
        currentError = error
        if (error != null) {
            errorLabel.text = error
            errorLabel.visibility = View.VISIBLE
        } else {
            errorLabel.visibility = View.GONE
        }
        editText.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
        updateBorderState()
    }

    internal fun setCardBrandIcon(@DrawableRes iconRes: Int, contentDescription: String) {
        val iconView = leadingIconView ?: ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(iconWidth, iconHeight).apply {
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                marginStart = defaultPaddingHorizontal
            }
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            scaleType = ImageView.ScaleType.FIT_CENTER
            clipToOutline = true
            outlineProvider = ViewOutlineProvider.BACKGROUND
            background = GradientDrawable().apply {
                setCornerRadius(iconCornerRadius)
                setColor(android.graphics.Color.TRANSPARENT)
            }
            inputContainer.addView(this)
            leadingIconView = this

            val newPaddingStart = defaultPaddingHorizontal + iconWidth + iconMargin
            editText.setPadding(newPaddingStart, editText.paddingTop, editText.paddingEnd, editText.paddingBottom)
            (hintLabel.layoutParams as FrameLayout.LayoutParams).marginStart = newPaddingStart
            hintLabel.requestLayout()
        }
        iconView.setImageResource(iconRes)
        iconView.contentDescription = contentDescription
    }

    companion object {
        private const val ANIMATION_DURATION = 200L
    }
}
