package com.braintreepayments.demo

import androidx.annotation.ColorInt
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalLabelStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalLogoStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodAppearance
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodContainerStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodCreditMessagingStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodFundingInstrumentStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodViewStyle

/**
 * The values of `PayPalSavedPaymentMethodViewStyle`, flattened so each one can be edited on its own. A `null` value
 * is left unset on the style, so the SDK applies its own default.
 */
data class PayPalSavedPaymentMethodStyleConfig(
    val showPayPalLogo: Boolean = true,
    val showPayPalLabel: Boolean = true,
    val showPayPalCreditMessaging: Boolean = true,
    @ColorInt val backgroundColor: Int? = null,
    @ColorInt val textColor: Int? = null,
    val baseFontSizeSp: Float? = null,
    val heightDp: Float? = null,
    val horizontalPaddingDp: Float? = null,
    val verticalPaddingDp: Float? = null,
    val cornerRadiusDp: Float? = null,
    @ColorInt val borderColor: Int? = null,
    val borderWidthDp: Float? = null,
    val logoWidthDp: Float? = null,
    val labelFontSizeSp: Float? = null,
    val labelMarginStartDp: Float? = null,
    val fundingInstrumentTextFontSizeSp: Float? = null,
    val editIconSizeDp: Float? = null,
    val fundingInstrumentMarginStartDp: Float? = null,
    val creditMessagingFontSizeSp: Float? = null,
    @ColorInt val creditMessagingLinkColor: Int? = null
) {

    @OptIn(ExperimentalBetaApi::class)
    fun toStyle() = PayPalSavedPaymentMethodViewStyle(
        showPayPalLogo = showPayPalLogo,
        showPayPalLabel = showPayPalLabel,
        showPayPalCreditMessaging = showPayPalCreditMessaging,
        componentAppearance = PayPalSavedPaymentMethodAppearance(
            backgroundColor = backgroundColor,
            textColor = textColor,
            baseFontSizeSp = baseFontSizeSp
        ),
        container = PayPalSavedPaymentMethodContainerStyle(
            heightDp = heightDp,
            horizontalPaddingDp = horizontalPaddingDp,
            verticalPaddingDp = verticalPaddingDp,
            cornerRadiusDp = cornerRadiusDp,
            borderColor = borderColor,
            borderWidthDp = borderWidthDp,
            logo = PayPalLogoStyle(widthDp = logoWidthDp),
            label = PayPalLabelStyle(fontSizeSp = labelFontSizeSp, marginStartDp = labelMarginStartDp),
            fundingInstrument = PayPalSavedPaymentMethodFundingInstrumentStyle(
                textFontSizeSp = fundingInstrumentTextFontSizeSp,
                editIconSizeDp = editIconSizeDp,
                marginStartDp = fundingInstrumentMarginStartDp
            ),
            creditMessaging = PayPalSavedPaymentMethodCreditMessagingStyle(
                fontSizeSp = creditMessagingFontSizeSp,
                linkColor = creditMessagingLinkColor
            )
        )
    )
}
