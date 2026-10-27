package com.braintreepayments.api.paypalsavedpaymentmethod.styling

import androidx.annotation.ColorInt
import androidx.annotation.FontRes
import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * Style and visibility configuration for the PayPal Saved Payment Method component.
 *
 * All styling properties default to `null`, meaning the SDK-provided default is used. Sizes and spacing below 0 are
 * treated as 0.
 *
 * @property showPayPalLogo whether the PayPal logo is displayed. Default: true.
 * @property showPayPalLabel whether the "PayPal" text label is displayed. Default: true.
 * @property showPayPalCreditMessaging whether eligible Pay Later / credit messaging is displayed. Default: true.
 * @property componentAppearance shared colors and typography used across the component. Null uses the SDK default
 * theme values.
 * @property container dimensions, spacing, shape, border, and internal layout of the component. Null uses the SDK
 * default container styling.
 */
@ExperimentalBetaApi
class PayPalSavedPaymentMethodViewStyle @JvmOverloads constructor(
    val showPayPalLogo: Boolean = true,
    val showPayPalLabel: Boolean = true,
    val showPayPalCreditMessaging: Boolean = true,
    val componentAppearance: PayPalSavedPaymentMethodAppearance? = null,
    val container: PayPalSavedPaymentMethodContainerStyle? = null
)

/**
 * Shared colors and typography used across the PayPal Saved Payment Method component.
 *
 * [backgroundColor] and [textColor] fall back to their SDK defaults independently (white background, dark text). When
 * overriding one, set the other as well so the text keeps enough contrast, for example light text on a dark
 * background.
 *
 * @property backgroundColor background color of the entire component, including its internal padding area. Null uses
 * the SDK default.
 * @property textColor text color used for the PayPal label, funding instrument text, and credit messaging. Null uses
 * the SDK default.
 * @property baseFontSizeSp base font size, in sp, used for a text element whose own font size is not provided. If this
 * is also null, the SDK default font size for that element is used.
 * @property fontResId font used for text across the component. Null uses the system font.
 */
@ExperimentalBetaApi
class PayPalSavedPaymentMethodAppearance @JvmOverloads constructor(
    @ColorInt val backgroundColor: Int? = null,
    @ColorInt val textColor: Int? = null,
    val baseFontSizeSp: Float? = null,
    @FontRes val fontResId: Int? = null
)

/**
 * Dimensions, spacing, shape, border, and internal layout of the PayPal Saved Payment Method component.
 *
 * @property heightDp minimum height of the container, in dp. Null wraps the content. The container is never smaller
 * than its content, so 0 or less behaves like null.
 * @property horizontalPaddingDp horizontal padding between the container edges and its content, in dp. Null uses the
 * SDK default.
 * @property verticalPaddingDp vertical padding between the container edges and its content, in dp. Null uses the SDK
 * default.
 * @property cornerRadiusDp corner radius of the container, in dp. Null uses the SDK default.
 * @property borderColor color of the border around the container. Null uses the SDK default.
 * @property borderWidthDp width of the border around the container, in dp. Null uses the SDK default.
 * @property logo sizing of the PayPal logo. Null uses the SDK default logo styling.
 * @property label typography and positioning of the "PayPal" text label. Null uses the SDK default label styling.
 * @property fundingInstrument styling and positioning of the saved payment method and its edit affordance. Null uses
 * the SDK default funding instrument styling.
 * @property creditMessaging styling of the Pay Later / credit messaging. Null uses the SDK default credit messaging
 * styling.
 */
@ExperimentalBetaApi
class PayPalSavedPaymentMethodContainerStyle @JvmOverloads constructor(
    val heightDp: Float? = null,
    val horizontalPaddingDp: Float? = null,
    val verticalPaddingDp: Float? = null,
    val cornerRadiusDp: Float? = null,
    @ColorInt val borderColor: Int? = null,
    val borderWidthDp: Float? = null,
    val logo: PayPalLogoStyle? = null,
    val label: PayPalLabelStyle? = null,
    val fundingInstrument: PayPalSavedPaymentMethodFundingInstrumentStyle? = null,
    val creditMessaging: PayPalSavedPaymentMethodCreditMessagingStyle? = null
)

/**
 * Sizing of the PayPal logo.
 *
 * @property widthDp side of the square box the PayPal logo is drawn in, in dp; the logo scales to fit and keeps its
 * aspect ratio. Null uses the SDK default.
 */
@ExperimentalBetaApi
class PayPalLogoStyle @JvmOverloads constructor(
    val widthDp: Float? = null
)

/**
 * Typography and positioning of the "PayPal" text label.
 *
 * @property fontSizeSp font size of the "PayPal" label, in sp. Null uses
 * [PayPalSavedPaymentMethodAppearance.baseFontSizeSp], then the SDK default.
 * @property marginStartDp start margin of the label relative to the preceding PayPal logo, in dp. Null uses the SDK
 * default.
 */
@ExperimentalBetaApi
class PayPalLabelStyle @JvmOverloads constructor(
    val fontSizeSp: Float? = null,
    val marginStartDp: Float? = null
)

/**
 * Styling and positioning of the saved payment method and its edit affordance.
 *
 * @property textFontSizeSp font size of the saved payment method text, in sp. Null uses
 * [PayPalSavedPaymentMethodAppearance.baseFontSizeSp], then the SDK default.
 * @property editIconSizeDp size of the edit icon for the saved payment method, in dp. Null uses the SDK default.
 * @property marginStartDp start margin of the funding instrument relative to the preceding "PayPal" label, in dp. Null
 * uses the SDK default.
 */
@ExperimentalBetaApi
class PayPalSavedPaymentMethodFundingInstrumentStyle @JvmOverloads constructor(
    val textFontSizeSp: Float? = null,
    val editIconSizeDp: Float? = null,
    val marginStartDp: Float? = null
)

/**
 * Styling of the Pay Later / credit messaging.
 *
 * @property fontSizeSp font size of the credit messaging text, in sp. Null uses
 * [PayPalSavedPaymentMethodAppearance.baseFontSizeSp], then the SDK default.
 * @property linkColor color of the "Learn more" link within credit messaging. Null shows the link bold and underlined
 * in the text color.
 */
@ExperimentalBetaApi
class PayPalSavedPaymentMethodCreditMessagingStyle @JvmOverloads constructor(
    val fontSizeSp: Float? = null,
    @ColorInt val linkColor: Int? = null
)
