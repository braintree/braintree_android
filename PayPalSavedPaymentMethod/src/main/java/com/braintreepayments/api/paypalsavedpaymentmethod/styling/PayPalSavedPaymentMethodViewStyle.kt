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
 * @param showPayPalLogo whether the PayPal logo is displayed. Default: true.
 * @param showPayPalLabel whether the "PayPal" text label is displayed. Default: true.
 * @param showPayPalCreditMessaging whether eligible Pay Later / credit messaging is displayed. Default: true.
 * @param componentAppearance shared colors and typography used across the component. Null uses the SDK default
 * theme values.
 * @param container dimensions, spacing, shape, border, and internal layout of the component. Null uses the SDK
 * default container styling.
 */
@ExperimentalBetaApi
class PayPalSavedPaymentMethodViewStyle @JvmOverloads constructor(
    internal val showPayPalLogo: Boolean = true,
    internal val showPayPalLabel: Boolean = true,
    internal val showPayPalCreditMessaging: Boolean = true,
    internal val componentAppearance: ComponentAppearance? = null,
    internal val container: ContainerStyle? = null
)

/**
 * Shared colors and typography used across the PayPal Saved Payment Method component.
 *
 * @param backgroundColor background color of the entire component, including its internal padding area. Null uses
 * the SDK default.
 * @param textColor text color used for the PayPal label, funding instrument text, and credit messaging. Null uses
 * the SDK default.
 * @param baseFontSizeSp base font size, in sp, used for a text element whose own font size is not provided. If this
 * is also null, the SDK default font size for that element is used.
 * @param fontResId font used for text across the component. Null uses the SDK default font.
 */
@ExperimentalBetaApi
class ComponentAppearance @JvmOverloads constructor(
    @ColorInt internal val backgroundColor: Int? = null,
    @ColorInt internal val textColor: Int? = null,
    internal val baseFontSizeSp: Float? = null,
    @FontRes internal val fontResId: Int? = null
)

/**
 * Dimensions, spacing, shape, border, and internal layout of the PayPal Saved Payment Method component.
 *
 * @param heightDp minimum height of the container, in dp. Null wraps the content. The container is never smaller than
 * its content, so 0 or less behaves like null.
 * @param horizontalPaddingDp horizontal padding between the container edges and its content, in dp. Null uses the
 * SDK default.
 * @param verticalPaddingDp vertical padding between the container edges and its content, in dp. Null uses the SDK
 * default.
 * @param cornerRadiusDp corner radius of the container, in dp. Null uses the SDK default.
 * @param borderColor color of the border around the container. Null uses the SDK default.
 * @param borderWidthDp width of the border around the container, in dp. Null uses the SDK default.
 * @param logo sizing of the PayPal logo. Null uses the SDK default logo styling.
 * @param label typography and positioning of the "PayPal" text label. Null uses the SDK default label styling.
 * @param fundingInstrument styling and positioning of the saved payment method and its edit affordance. Null uses
 * the SDK default funding instrument styling.
 * @param creditMessaging styling of the Pay Later / credit messaging. Null uses the SDK default credit messaging
 * styling.
 */
@ExperimentalBetaApi
class ContainerStyle @JvmOverloads constructor(
    internal val heightDp: Float? = null,
    internal val horizontalPaddingDp: Float? = null,
    internal val verticalPaddingDp: Float? = null,
    internal val cornerRadiusDp: Float? = null,
    @ColorInt internal val borderColor: Int? = null,
    internal val borderWidthDp: Float? = null,
    internal val logo: PayPalLogoStyle? = null,
    internal val label: PayPalLabelStyle? = null,
    internal val fundingInstrument: FundingInstrumentStyle? = null,
    internal val creditMessaging: CreditMessagingStyle? = null
)

/**
 * Sizing of the PayPal logo.
 *
 * @param widthDp width of the PayPal logo, in dp. Null uses the SDK default.
 */
@ExperimentalBetaApi
class PayPalLogoStyle @JvmOverloads constructor(
    internal val widthDp: Float? = null
)

/**
 * Typography and positioning of the "PayPal" text label.
 *
 * @param fontSizeSp font size of the "PayPal" label, in sp. Null uses [ComponentAppearance.baseFontSizeSp], then the
 * SDK default.
 * @param marginStartDp start margin of the label relative to the preceding PayPal logo, in dp. Null uses the SDK
 * default.
 */
@ExperimentalBetaApi
class PayPalLabelStyle @JvmOverloads constructor(
    internal val fontSizeSp: Float? = null,
    internal val marginStartDp: Float? = null
)

/**
 * Styling and positioning of the saved payment method and its edit affordance.
 *
 * @param textFontSizeSp font size of the saved payment method text, in sp. Null uses
 * [ComponentAppearance.baseFontSizeSp], then the SDK default.
 * @param editIconSizeDp size of the edit icon for the saved payment method, in dp. Null uses the SDK default.
 * @param marginStartDp start margin of the funding instrument relative to the preceding "PayPal" label, in dp. Null
 * uses the SDK default.
 */
@ExperimentalBetaApi
class FundingInstrumentStyle @JvmOverloads constructor(
    internal val textFontSizeSp: Float? = null,
    internal val editIconSizeDp: Float? = null,
    internal val marginStartDp: Float? = null
)

/**
 * Styling of the Pay Later / credit messaging.
 *
 * @param fontSizeSp font size of the credit messaging text, in sp. Null uses [ComponentAppearance.baseFontSizeSp],
 * then the SDK default.
 * @param linkColor color of the "Learn more" link within credit messaging. Null shows the link bold and underlined in
 * the text color.
 */
@ExperimentalBetaApi
class CreditMessagingStyle @JvmOverloads constructor(
    internal val fontSizeSp: Float? = null,
    @ColorInt internal val linkColor: Int? = null
)
