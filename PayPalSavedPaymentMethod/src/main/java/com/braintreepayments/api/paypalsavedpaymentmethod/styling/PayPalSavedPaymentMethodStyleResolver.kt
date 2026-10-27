package com.braintreepayments.api.paypalsavedpaymentmethod.styling

import android.content.Context
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.FontRes
import androidx.core.content.ContextCompat
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypalsavedpaymentmethod.R

/** Resolves the merchant's style against the SDK defaults, in dp and sp. Negative sizes resolve to 0. */
@OptIn(ExperimentalBetaApi::class)
internal class PayPalSavedPaymentMethodStyleResolver(context: Context, style: PayPalSavedPaymentMethodViewStyle) {

    private val resources = context.resources
    private val baseFontSizeSp: Float? = style.componentAppearance?.baseFontSizeSp

    val showPayPalLogo: Boolean = style.showPayPalLogo
    val showPayPalLabel: Boolean = style.showPayPalLabel
    val showPayPalCreditMessaging: Boolean = style.showPayPalCreditMessaging

    @ColorInt
    val backgroundColor: Int = style.componentAppearance?.backgroundColor
        ?: context.colorInt(R.color.paypal_saved_payment_method_style_default_background)

    @ColorInt
    val textColor: Int = style.componentAppearance?.textColor
        ?: context.colorInt(R.color.paypal_saved_payment_method_style_default_text_color)

    @FontRes
    val fontResId: Int? = style.componentAppearance?.fontResId

    val heightDp: Float? = style.container?.heightDp?.coerceAtLeast(0f)
    val horizontalPaddingDp: Float =
        dp(style.container?.horizontalPaddingDp, R.dimen.paypal_saved_payment_method_style_default_horizontal_padding)
    val verticalPaddingDp: Float =
        dp(style.container?.verticalPaddingDp, R.dimen.paypal_saved_payment_method_style_default_vertical_padding)
    val cornerRadiusDp: Float =
        dp(style.container?.cornerRadiusDp, R.dimen.paypal_saved_payment_method_style_default_corner_radius)

    @ColorInt
    val borderColor: Int = style.container?.borderColor
        ?: context.colorInt(R.color.paypal_saved_payment_method_style_default_border_color)
    val borderWidthDp: Float =
        dp(style.container?.borderWidthDp, R.dimen.paypal_saved_payment_method_style_default_border_width)

    val logoWidthDp: Float =
        dp(style.container?.logo?.widthDp, R.dimen.paypal_saved_payment_method_style_default_logo_width)

    val labelFontSizeSp: Float =
        sp(style.container?.label?.fontSizeSp, R.dimen.paypal_saved_payment_method_style_default_label_font_size)
    val labelMarginStartDp: Float =
        dp(style.container?.label?.marginStartDp, R.dimen.paypal_saved_payment_method_style_default_label_margin_start)

    val fundingInstrumentTextFontSizeSp: Float = sp(
        style.container?.fundingInstrument?.textFontSizeSp,
        R.dimen.paypal_saved_payment_method_style_default_funding_instrument_text_font_size
    )
    val editIconSizeDp: Float = dp(
        style.container?.fundingInstrument?.editIconSizeDp,
        R.dimen.paypal_saved_payment_method_style_default_edit_icon_size
    )
    val fundingInstrumentMarginStartDp: Float = dp(
        style.container?.fundingInstrument?.marginStartDp,
        R.dimen.paypal_saved_payment_method_style_default_funding_instrument_margin_start
    )

    val creditMessagingFontSizeSp: Float = sp(
        style.container?.creditMessaging?.fontSizeSp,
        R.dimen.paypal_saved_payment_method_style_default_credit_messaging_font_size
    )

    @ColorInt
    val creditMessagingLinkColor: Int? = style.container?.creditMessaging?.linkColor

    private fun dp(value: Float?, @DimenRes defaultRes: Int): Float =
        (value ?: resources.dimenDp(defaultRes)).coerceAtLeast(0f)

    private fun sp(value: Float?, @DimenRes defaultRes: Int): Float =
        (value ?: baseFontSizeSp ?: resources.dimenSp(defaultRes)).coerceAtLeast(0f)
}

@ColorInt
private fun Context.colorInt(@ColorRes colorRes: Int): Int = ContextCompat.getColor(this, colorRes)
