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
                R.styleable.PayPalSavedPaymentMethodView_paypalSavedPaymentMethodShowLogo,
                defaults.showPayPalLogo
            ),
            showPayPalLabel = typedArray.getBoolean(
                R.styleable.PayPalSavedPaymentMethodView_paypalSavedPaymentMethodShowLabel,
                defaults.showPayPalLabel
            ),
            showPayPalCreditMessaging = typedArray.getBoolean(
                R.styleable.PayPalSavedPaymentMethodView_paypalSavedPaymentMethodShowCreditMessaging,
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
private fun parseComponentAppearance(typedArray: TypedArray, resources: Resources): PayPalSavedPaymentMethodAppearance =
    PayPalSavedPaymentMethodAppearance(
        backgroundColor = typedArray.colorOrNull(
            R.styleable.PayPalSavedPaymentMethodView_paypalSavedPaymentMethodBackgroundColor
        ),
        textColor = typedArray.colorOrNull(
            R.styleable.PayPalSavedPaymentMethodView_paypalSavedPaymentMethodTextColor
        ),
        baseFontSizeSp = typedArray.spOrNull(
            R.styleable.PayPalSavedPaymentMethodView_paypalSavedPaymentMethodFontSize,
            resources
        ),
        fontResId = typedArray.resourceIdOrNull(
            R.styleable.PayPalSavedPaymentMethodView_paypalSavedPaymentMethodFont
        )
    )

@OptIn(ExperimentalBetaApi::class)
private fun parseContainerStyle(typedArray: TypedArray, resources: Resources): PayPalSavedPaymentMethodContainerStyle =
    PayPalSavedPaymentMethodContainerStyle(
        heightDp = typedArray.dpOrNull(
            R.styleable.PayPalSavedPaymentMethodView_paypalSavedPaymentMethodHeight,
            resources
        ),
        horizontalPaddingDp = typedArray.dpOrNull(
            R.styleable.PayPalSavedPaymentMethodView_paypalSavedPaymentMethodHorizontalPadding,
            resources
        ),
        verticalPaddingDp = typedArray.dpOrNull(
            R.styleable.PayPalSavedPaymentMethodView_paypalSavedPaymentMethodVerticalPadding,
            resources
        ),
        cornerRadiusDp = typedArray.dpOrNull(
            R.styleable.PayPalSavedPaymentMethodView_paypalSavedPaymentMethodCornerRadius,
            resources
        ),
        borderColor = typedArray.colorOrNull(
            R.styleable.PayPalSavedPaymentMethodView_paypalSavedPaymentMethodBorderColor
        ),
        borderWidthDp = typedArray.dpOrNull(
            R.styleable.PayPalSavedPaymentMethodView_paypalSavedPaymentMethodBorderWidth,
            resources
        ),
        logo = PayPalLogoStyle(
            widthDp = typedArray.dpOrNull(
                R.styleable.PayPalSavedPaymentMethodView_paypalSavedPaymentMethodLogoWidth,
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
            R.styleable.PayPalSavedPaymentMethodView_paypalSavedPaymentMethodLabelFontSize,
            resources
        ),
        marginStartDp = typedArray.dpOrNull(
            R.styleable.PayPalSavedPaymentMethodView_paypalSavedPaymentMethodLabelMarginStart,
            resources
        )
    )

@OptIn(ExperimentalBetaApi::class)
private fun parseFundingInstrumentStyle(
    typedArray: TypedArray,
    resources: Resources
): PayPalSavedPaymentMethodFundingInstrumentStyle =
    PayPalSavedPaymentMethodFundingInstrumentStyle(
        textFontSizeSp = typedArray.spOrNull(
            R.styleable.PayPalSavedPaymentMethodView_paypalSavedPaymentMethodFundingInstrumentFontSize,
            resources
        ),
        editIconSizeDp = typedArray.dpOrNull(
            R.styleable.PayPalSavedPaymentMethodView_paypalSavedPaymentMethodEditIconSize,
            resources
        ),
        marginStartDp = typedArray.dpOrNull(
            R.styleable.PayPalSavedPaymentMethodView_paypalSavedPaymentMethodFundingInstrumentMarginStart,
            resources
        )
    )

@OptIn(ExperimentalBetaApi::class)
private fun parseCreditMessagingStyle(
    typedArray: TypedArray,
    resources: Resources
): PayPalSavedPaymentMethodCreditMessagingStyle =
    PayPalSavedPaymentMethodCreditMessagingStyle(
        fontSizeSp = typedArray.spOrNull(
            R.styleable.PayPalSavedPaymentMethodView_paypalSavedPaymentMethodCreditMessagingFontSize,
            resources
        ),
        linkColor = typedArray.colorOrNull(
            R.styleable.PayPalSavedPaymentMethodView_paypalSavedPaymentMethodCreditMessagingLinkColor
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
