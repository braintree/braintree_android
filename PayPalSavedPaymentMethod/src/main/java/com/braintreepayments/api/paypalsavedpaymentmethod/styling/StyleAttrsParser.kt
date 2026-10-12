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
        PayPalSavedPaymentMethodViewStyle(
            showPayPalLogo = typedArray.getBoolean(R.styleable.PayPalSavedPaymentMethodView_showPayPalLogo, true),
            showPayPalLabel = typedArray.getBoolean(R.styleable.PayPalSavedPaymentMethodView_showPayPalLabel, true),
            showPayPalCreditMessaging = typedArray.getBoolean(
                R.styleable.PayPalSavedPaymentMethodView_showPayPalCreditMessaging,
                true
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
        backgroundColor = typedArray.colorOrNull(R.styleable.PayPalSavedPaymentMethodView_componentBackgroundColor),
        textColor = typedArray.colorOrNull(R.styleable.PayPalSavedPaymentMethodView_componentTextColor),
        baseFontSizeSp = typedArray.spOrNull(
            R.styleable.PayPalSavedPaymentMethodView_componentBaseFontSizeSp,
            resources
        ),
        fontResId = typedArray.resourceIdOrNull(R.styleable.PayPalSavedPaymentMethodView_componentFontResId)
    )

@OptIn(ExperimentalBetaApi::class)
private fun parseContainerStyle(typedArray: TypedArray, resources: Resources): ContainerStyle =
    ContainerStyle(
        heightDp = typedArray.dpOrNull(R.styleable.PayPalSavedPaymentMethodView_containerHeightDp, resources),
        horizontalPaddingDp = typedArray.dpOrNull(
            R.styleable.PayPalSavedPaymentMethodView_containerHorizontalPaddingDp,
            resources
        ),
        verticalPaddingDp = typedArray.dpOrNull(
            R.styleable.PayPalSavedPaymentMethodView_containerVerticalPaddingDp,
            resources
        ),
        cornerRadiusDp = typedArray.dpOrNull(
            R.styleable.PayPalSavedPaymentMethodView_containerCornerRadiusDp,
            resources
        ),
        borderColor = typedArray.colorOrNull(R.styleable.PayPalSavedPaymentMethodView_containerBorderColor),
        borderWidthDp = typedArray.dpOrNull(R.styleable.PayPalSavedPaymentMethodView_containerBorderWidthDp, resources),
        logo = PayPalLogoStyle(
            widthDp = typedArray.dpOrNull(R.styleable.PayPalSavedPaymentMethodView_payPalLogoWidthDp, resources)
        ),
        label = PayPalLabelStyle(
            fontSizeSp = typedArray.spOrNull(R.styleable.PayPalSavedPaymentMethodView_payPalLabelFontSizeSp, resources),
            marginStartDp = typedArray.dpOrNull(
                R.styleable.PayPalSavedPaymentMethodView_payPalLabelMarginStartDp,
                resources
            )
        ),
        fundingInstrument = FundingInstrumentStyle(
            textFontSizeSp = typedArray.spOrNull(
                R.styleable.PayPalSavedPaymentMethodView_fundingInstrumentTextFontSizeSp,
                resources
            ),
            editIconSizeDp = typedArray.dpOrNull(
                R.styleable.PayPalSavedPaymentMethodView_fundingInstrumentEditIconSizeDp,
                resources
            ),
            marginStartDp = typedArray.dpOrNull(
                R.styleable.PayPalSavedPaymentMethodView_fundingInstrumentMarginStartDp,
                resources
            )
        ),
        creditMessaging = CreditMessagingStyle(
            fontSizeSp = typedArray.spOrNull(
                R.styleable.PayPalSavedPaymentMethodView_creditMessagingFontSizeSp,
                resources
            ),
            linkColor = typedArray.colorOrNull(R.styleable.PayPalSavedPaymentMethodView_creditMessagingLinkColor)
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
