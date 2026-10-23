package com.braintreepayments.api.paypalsavedpaymentmethod.styling

import android.content.Context
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.FontRes
import androidx.core.content.ContextCompat
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypalsavedpaymentmethod.R

/**
 * Resolves a merchant-supplied [PayPalSavedPaymentMethodViewStyle] against the SDK defaults, so the XML and Compose
 * renderers read concrete values instead of checking for null. SDK defaults live in `res/values/colors.xml` and
 * `res/values/dimens.xml` (`paypal_saved_payment_method_style_default_*`).
 *
 * Sizes and spacing are never negative: a negative merchant value resolves to 0. Element font sizes fall back to the
 * element's own value, then [PayPalSavedPaymentMethodAppearance.baseFontSizeSp] if the merchant set it, then the
 * element's SDK default.
 *
 * [heightDp], [fontResId] and [creditMessagingLinkColor] stay null when unset: null height wraps the content, a null
 * font uses the system font, and a null link color shows the link bold and underlined in the text color.
 *
 * All sizes are in dp or sp, which don't depend on screen density or font scale, so a resolver can be kept across
 * configuration changes. Renderers convert them to px when drawing.
 */
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
