package com.braintreepayments.api.paypalsavedpaymentmethod.xml

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import com.braintreepayments.api.paypalsavedpaymentmethod.accessibilityLabel
import com.braintreepayments.api.paypalsavedpaymentmethod.iconRes
import com.braintreepayments.api.paypalsavedpaymentmethod.imageloader.ImageLoader
import com.braintreepayments.api.paypalsavedpaymentmethod.isEditable
import com.braintreepayments.api.paypalsavedpaymentmethod.maskedLastDigitsOrNull
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethodDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodStyleResolver

/**
 * The funding instrument chip: the card art or generic icon, the label or masked last digits (or the buyer's email),
 * and the edit icon. The whole chip is the edit target, so it's larger than the edit icon alone. A buyer who can't
 * edit gets a display-only chip. Screen readers read the chip as one element, for example "Visa, ending in 1 2 3 4".
 */
internal class FundingInstrumentChipView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val iconView: ImageView
    private val textView: TextView
    private val editIconView: ImageView
    private val imageLoader = ImageLoader.getInstance()
    private var isEditable = false
    private var boundIcon: Pair<String?, Int?>? = null
    private val editActionLabel = context.getString(R.string.paypal_saved_payment_method_edit_action_label)

    // The theme's touch feedback, clipped to the chip's rounded corners.
    private val clickFeedback = TypedValue().let { value ->
        if (context.theme.resolveAttribute(android.R.attr.selectableItemBackground, value, true)) {
            ContextCompat.getDrawable(context, value.resourceId)
        } else {
            null
        }
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        LayoutInflater.from(context).inflate(R.layout.paypal_saved_payment_method_funding_instrument_chip, this)
        iconView = findViewById(R.id.paypal_saved_payment_method_funding_instrument_icon)
        textView = findViewById(R.id.paypal_saved_payment_method_funding_instrument_text)
        editIconView = findViewById(R.id.paypal_saved_payment_method_edit_icon)

        background = GradientDrawable().apply {
            cornerRadius =
                resources.getDimension(R.dimen.paypal_saved_payment_method_funding_instrument_chip_corner_radius)
            setColor(
                ContextCompat.getColor(context, R.color.paypal_saved_payment_method_funding_instrument_chip_background)
            )
        }
        clipToOutline = true
        val horizontalPadding = resources.getDimensionPixelSize(
            R.dimen.paypal_saved_payment_method_funding_instrument_chip_horizontal_padding
        )
        val verticalPadding = resources.getDimensionPixelSize(
            R.dimen.paypal_saved_payment_method_funding_instrument_chip_vertical_padding
        )
        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)

        ViewCompat.setAccessibilityDelegate(this, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                if (isEditable) info.className = Button::class.java.name
            }
        })
    }

    /** Shows [state], which must be a state with a funding instrument row. */
    fun bind(
        state: PayPalSavedPaymentMethodDisplayState,
        resolvedStyle: PayPalSavedPaymentMethodStyleResolver,
        editEnabled: Boolean,
        onEditClick: () -> Unit
    ) {
        isEditable = state.isEditable
        contentDescription = state.accessibilityLabel { label, spokenDigits ->
            context.getString(R.string.paypal_saved_payment_method_funding_instrument_ending_in, label, spokenDigits)
        }
        if (isEditable) {
            setOnClickListener { onEditClick() }
            ViewCompat.replaceAccessibilityAction(this, AccessibilityActionCompat.ACTION_CLICK, editActionLabel, null)
        } else {
            setOnClickListener(null)
            ViewCompat.removeAccessibilityAction(this, AccessibilityNodeInfoCompat.ACTION_CLICK)
        }
        isClickable = isEditable
        isEnabled = editEnabled
        foreground = if (isEditable) clickFeedback else null

        textView.applyTextStyle(resolvedStyle, resolvedStyle.fundingInstrumentTextFontSizeSp)
        when (state) {
            is PayPalSavedPaymentMethodDisplayState.Content -> {
                val paymentMethod = state.paymentMethod
                textView.text = paymentMethod.maskedLastDigitsOrNull()?.let {
                    context.getString(
                        R.string.paypal_saved_payment_method_label_funding_instrument_card_masked_number,
                        it
                    )
                } ?: paymentMethod.label
                bindIcon(paymentMethod.imageUrl, paymentMethod.type.iconRes())
            }
            is PayPalSavedPaymentMethodDisplayState.NoFundingInstrument -> {
                textView.text = state.payerEmail
                bindIcon(null, null)
            }
            else -> Unit
        }

        editIconView.isVisible = isEditable
        val editIconSize = resolvedStyle.editIconSizeDp.dpToPx(resources)
        editIconView.updateLayoutParams {
            width = editIconSize
            height = editIconSize
        }
    }

    // Card art from PayPal, blank while it loads, with the generic card or bank icon if it can't be loaded.
    private fun bindIcon(url: String?, fallbackIcon: Int?) {
        // Re-rendering with the same icon keeps the current image instead of reloading it.
        if (boundIcon == url to fallbackIcon) return
        boundIcon = url to fallbackIcon
        if (url == null && fallbackIcon == null) {
            imageLoader.cancel(iconView)
            iconView.setImageDrawable(null)
            iconView.isVisible = false
            return
        }
        iconView.isVisible = true
        imageLoader.load(url, iconView, loadingPlaceholder = null, errorPlaceholder = fallbackIcon)
    }
}
