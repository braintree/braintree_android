package com.braintreepayments.api.paypalsavedpaymentmethod.styling

import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.util.AttributeSet
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypalsavedpaymentmethod.R

/**
 * Parses [attrs] into a [PayPalSavedPaymentMethodViewStyle]. Unset attrs stay null, so they resolve to SDK defaults.
 */
@OptIn(ExperimentalBetaApi::class)
internal fun styleFromAttrs(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
): PayPalSavedPaymentMethodViewStyle {
    val typedArray = context.obtainStyledAttributes(attrs, R.styleable.PayPalSavedPaymentMethodView, defStyleAttr, 0)
    return try {
        val resources = context.resources
        // Unset toggles fall back to the style class's own defaults, so the two can't drift.
        val defaults = PayPalSavedPaymentMethodViewStyle()
        PayPalSavedPaymentMethodViewStyle(
            showPayPalLogo = typedArray.getBoolean(
                R.styleable.PayPalSavedPaymentMethodView_paypal_saved_payment_method_show_logo,
                defaults.showPayPalLogo
            ),
            showPayPalLabel = typedArray.getBoolean(
                R.styleable.PayPalSavedPaymentMethodView_paypal_saved_payment_method_show_label,
                defaults.showPayPalLabel
            ),
            showPayPalCreditMessaging = typedArray.getBoolean(
                R.styleable.PayPalSavedPaymentMethodView_paypal_saved_payment_method_show_credit_messaging,
                defaults.showPayPalCreditMessaging
            ),
            componentAppearance = parseComponentAppearance(typedArray, resources),
            container = parseContainerStyle(typedArray, resources)
        )
    } finally {
        typedArray.recycle()
    }
}

@OptIn(ExperimentalBetaApi::class)
private fun parseComponentAppearance(typedArray: TypedArray, resources: Resources): ComponentAppearance =
    ComponentAppearance(
        backgroundColor = typedArray.colorOrNull(
            R.styleable.PayPalSavedPaymentMethodView_paypal_saved_payment_method_background_color
        ),
        textColor = typedArray.colorOrNull(
            R.styleable.PayPalSavedPaymentMethodView_paypal_saved_payment_method_text_color
        ),
        baseFontSizeSp = typedArray.spOrNull(
            R.styleable.PayPalSavedPaymentMethodView_paypal_saved_payment_method_font_size,
            resources
        ),
        fontResId = typedArray.resourceIdOrNull(
            R.styleable.PayPalSavedPaymentMethodView_paypal_saved_payment_method_font
        )
    )

@OptIn(ExperimentalBetaApi::class)
private fun parseContainerStyle(typedArray: TypedArray, resources: Resources): ContainerStyle =
    ContainerStyle(
        heightDp = typedArray.dpOrNull(
            R.styleable.PayPalSavedPaymentMethodView_paypal_saved_payment_method_height,
            resources
        ),
        horizontalPaddingDp = typedArray.dpOrNull(
            R.styleable.PayPalSavedPaymentMethodView_paypal_saved_payment_method_horizontal_padding,
            resources
        ),
        verticalPaddingDp = typedArray.dpOrNull(
            R.styleable.PayPalSavedPaymentMethodView_paypal_saved_payment_method_vertical_padding,
            resources
        ),
        cornerRadiusDp = typedArray.dpOrNull(
            R.styleable.PayPalSavedPaymentMethodView_paypal_saved_payment_method_corner_radius,
            resources
        ),
        borderColor = typedArray.colorOrNull(
            R.styleable.PayPalSavedPaymentMethodView_paypal_saved_payment_method_border_color
        ),
        borderWidthDp = typedArray.dpOrNull(
            R.styleable.PayPalSavedPaymentMethodView_paypal_saved_payment_method_border_width,
            resources
        ),
        logo = PayPalLogoStyle(
            widthDp = typedArray.dpOrNull(
                R.styleable.PayPalSavedPaymentMethodView_paypal_saved_payment_method_logo_width,
                resources
            )
        ),
        label = parseLabelStyle(typedArray, resources),
        fundingInstrument = parseFundingInstrumentStyle(typedArray, resources),
        creditMessaging = parseCreditMessagingStyle(typedArray, resources)
    )

@OptIn(ExperimentalBetaApi::class)
private fun parseLabelStyle(typedArray: TypedArray, resources: Resources): PayPalLabelStyle =
    PayPalLabelStyle(
        fontSizeSp = typedArray.spOrNull(
            R.styleable.PayPalSavedPaymentMethodView_paypal_saved_payment_method_label_font_size,
            resources
        ),
        marginStartDp = typedArray.dpOrNull(
            R.styleable.PayPalSavedPaymentMethodView_paypal_saved_payment_method_label_margin_start,
            resources
        )
    )

@OptIn(ExperimentalBetaApi::class)
private fun parseFundingInstrumentStyle(typedArray: TypedArray, resources: Resources): FundingInstrumentStyle =
    FundingInstrumentStyle(
        textFontSizeSp = typedArray.spOrNull(
            R.styleable.PayPalSavedPaymentMethodView_paypal_saved_payment_method_funding_instrument_font_size,
            resources
        ),
        editIconSizeDp = typedArray.dpOrNull(
            R.styleable.PayPalSavedPaymentMethodView_paypal_saved_payment_method_edit_icon_size,
            resources
        ),
        marginStartDp = typedArray.dpOrNull(
            R.styleable.PayPalSavedPaymentMethodView_paypal_saved_payment_method_funding_instrument_margin_start,
            resources
        )
    )

@OptIn(ExperimentalBetaApi::class)
private fun parseCreditMessagingStyle(typedArray: TypedArray, resources: Resources): CreditMessagingStyle =
    CreditMessagingStyle(
        fontSizeSp = typedArray.spOrNull(
            R.styleable.PayPalSavedPaymentMethodView_paypal_saved_payment_method_credit_messaging_font_size,
            resources
        ),
        linkColor = typedArray.colorOrNull(
            R.styleable.PayPalSavedPaymentMethodView_paypal_saved_payment_method_credit_messaging_link_color
        )
    )

private fun TypedArray.colorOrNull(index: Int): Int? =
    if (hasValue(index)) getColor(index, 0) else null

private fun TypedArray.resourceIdOrNull(index: Int): Int? =
    if (hasValue(index)) getResourceId(index, 0) else null

private fun TypedArray.dpOrNull(index: Int, resources: Resources): Float? =
    if (hasValue(index)) resources.pxToDp(getDimension(index, 0f)) else null

private fun TypedArray.spOrNull(index: Int, resources: Resources): Float? =
    if (hasValue(index)) resources.pxToSp(getDimension(index, 0f)) else null
