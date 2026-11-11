package com.braintreepayments.api.paypalsavedpaymentmethod.xml

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.TouchDelegate
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePaddingRelative
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalCreditMessagingDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalSavedPaymentMethodDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.showsFundingInstrumentRow
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodStyleResolver
import com.braintreepayments.api.paypalsavedpaymentmethod.state.visibleWith

/**
 * Renders the component from its display states: the PayPal logo and label, the funding instrument chip and the
 * Pay Later message. Stateless. Renders nothing when hidden, or when only the logo and label would show and the
 * merchant hid both.
 */
internal class PayPalSavedPaymentMethodContentView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    var onEditClick: () -> Unit = {}
    var onLearnMoreClick: (url: String) -> Unit = {}

    private val brandView: View
    private val logoView: ImageView
    private val labelView: TextView
    private val fundingInstrumentShimmer: ShimmerPlaceholderView
    private val fundingInstrumentChip: FundingInstrumentChipView
    private val creditMessagingView: PayPalCreditMessagingView

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.paypal_saved_payment_method_view, this)
        brandView = findViewById(R.id.paypal_saved_payment_method_brand)
        logoView = findViewById(R.id.paypal_saved_payment_method_logo)
        labelView = findViewById(R.id.paypal_saved_payment_method_label)
        fundingInstrumentShimmer = findViewById(R.id.paypal_saved_payment_method_funding_instrument_shimmer)
        fundingInstrumentChip = findViewById(R.id.paypal_saved_payment_method_funding_instrument_chip)
        creditMessagingView = findViewById(R.id.paypal_saved_payment_method_credit_messaging)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        updateChipTouchDelegate()
    }

    // An editable chip gets a 48dp touch target, like Compose's minimumInteractiveComponentSize, without changing how
    // the chip looks. Kept inside this view, since a touch outside it never arrives.
    private fun updateChipTouchDelegate() {
        if (!fundingInstrumentChip.isVisible || !fundingInstrumentChip.isClickable) {
            touchDelegate = null
            return
        }
        val bounds = Rect(0, 0, fundingInstrumentChip.width, fundingInstrumentChip.height)
        offsetDescendantRectToMyCoords(fundingInstrumentChip, bounds)
        val minSize = MIN_TOUCH_TARGET_DP.dpToPx(resources)
        bounds.inset(
            -((minSize - bounds.width() + 1) / 2).coerceAtLeast(0),
            -((minSize - bounds.height() + 1) / 2).coerceAtLeast(0)
        )
        bounds.intersect(0, 0, width, height)
        touchDelegate = TouchDelegate(bounds, fundingInstrumentChip)
    }

    @Suppress("LongParameterList")
    fun render(
        fundingInstrumentState: PayPalSavedPaymentMethodDisplayState,
        creditMessagingState: PayPalCreditMessagingDisplayState,
        resolvedStyle: PayPalSavedPaymentMethodStyleResolver,
        editEnabled: Boolean,
        isEditCompleted: Boolean
    ) {
        val showsBrand = resolvedStyle.showPayPalLogo || resolvedStyle.showPayPalLabel
        // Nothing to draw when only the brand would show and it's hidden.
        isVisible = fundingInstrumentState != PayPalSavedPaymentMethodDisplayState.Hidden &&
            (fundingInstrumentState.showsFundingInstrumentRow || showsBrand)
        if (!isVisible) return
        applyStyle(resolvedStyle)

        fundingInstrumentShimmer.isVisible = fundingInstrumentState == PayPalSavedPaymentMethodDisplayState.Loading
        fundingInstrumentChip.isVisible = fundingInstrumentState.showsFundingInstrumentRow &&
            fundingInstrumentState != PayPalSavedPaymentMethodDisplayState.Loading
        if (fundingInstrumentChip.isVisible) {
            fundingInstrumentChip.bind(fundingInstrumentState, resolvedStyle, editEnabled) { onEditClick() }
        }

        val visibleCreditMessagingState = if (resolvedStyle.showPayPalCreditMessaging) {
            creditMessagingState.visibleWith(fundingInstrumentState, isEditCompleted)
        } else {
            PayPalCreditMessagingDisplayState.Hidden
        }
        creditMessagingView.bind(visibleCreditMessagingState, resolvedStyle) { onLearnMoreClick(it) }
    }

    private fun applyStyle(resolvedStyle: PayPalSavedPaymentMethodStyleResolver) {
        background = GradientDrawable().apply {
            cornerRadius = resolvedStyle.cornerRadiusDp.dpToPx(resources).toFloat()
            setColor(resolvedStyle.backgroundColor)
            val borderWidth = resolvedStyle.borderWidthDp.dpToPx(resources)
            if (borderWidth > 0) setStroke(borderWidth, resolvedStyle.borderColor)
        }
        val horizontalPadding = resolvedStyle.horizontalPaddingDp.dpToPx(resources)
        val verticalPadding = resolvedStyle.verticalPaddingDp.dpToPx(resources)
        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        // The height is a minimum, so large text isn't clipped.
        minimumHeight = resolvedStyle.heightDp?.dpToPx(resources) ?: 0

        val logoSize = resolvedStyle.logoWidthDp.dpToPx(resources)
        val labelGap = resolvedStyle.labelMarginStartDp.dpToPx(resources)
        brandView.isVisible = resolvedStyle.showPayPalLogo || resolvedStyle.showPayPalLabel
        logoView.isVisible = resolvedStyle.showPayPalLogo
        logoView.updateLayoutParams {
            width = logoSize
            height = logoSize
        }
        // The label already says "PayPal".
        logoView.contentDescription = if (resolvedStyle.showPayPalLabel) {
            null
        } else {
            context.getString(R.string.paypal_saved_payment_method_label)
        }

        labelView.isVisible = resolvedStyle.showPayPalLabel
        labelView.applyTextStyle(resolvedStyle, resolvedStyle.labelFontSizeSp, weight = TextWeight.MEDIUM)
        // The gap sits between the logo and the label.
        labelView.updatePaddingRelative(start = if (resolvedStyle.showPayPalLogo) labelGap else 0)

        val fundingInstrumentGap = resolvedStyle.fundingInstrumentMarginStartDp.dpToPx(resources)
        fundingInstrumentShimmer.updateLayoutParams<MarginLayoutParams> { marginStart = fundingInstrumentGap }
        fundingInstrumentChip.updateLayoutParams<MarginLayoutParams> { marginStart = fundingInstrumentGap }
        // Aligned with the label: after the logo if it's shown, else at the leading edge.
        creditMessagingView.updateLayoutParams<MarginLayoutParams> {
            marginStart = if (resolvedStyle.showPayPalLogo) logoSize + labelGap else 0
        }
    }

    private companion object {
        const val MIN_TOUCH_TARGET_DP = 48f
    }
}
