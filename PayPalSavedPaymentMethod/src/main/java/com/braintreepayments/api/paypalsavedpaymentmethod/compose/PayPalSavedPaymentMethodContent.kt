package com.braintreepayments.api.paypalsavedpaymentmethod.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalCreditMessagingDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalSavedPaymentMethodDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.showsFundingInstrumentRow
import com.braintreepayments.api.paypalsavedpaymentmethod.state.visibleWith
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodStyleResolver
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodViewStyle

/**
 * Renders the component from its display states: the PayPal logo and label, the funding instrument chip and the
 * Pay Later message. Stateless. Renders nothing when hidden, or when only the logo and label would show and the
 * merchant hid both.
 */
@OptIn(ExperimentalBetaApi::class)
@Composable
internal fun PayPalSavedPaymentMethodContent(
    fundingInstrumentState: PayPalSavedPaymentMethodDisplayState,
    creditMessagingState: PayPalCreditMessagingDisplayState,
    style: PayPalSavedPaymentMethodViewStyle,
    onEditClick: () -> Unit,
    onLearnMoreClick: (url: String) -> Unit,
    modifier: Modifier = Modifier,
    editEnabled: Boolean = true,
    isEditCompleted: Boolean = false
) {
    if (fundingInstrumentState == PayPalSavedPaymentMethodDisplayState.Hidden) return

    val context = LocalContext.current
    val resolvedStyle = remember(style, context) { PayPalSavedPaymentMethodStyleResolver(context, style) }
    val showsBrandMark = resolvedStyle.showPayPalLogo || resolvedStyle.showPayPalLabel
    // Only the brand would show and it's hidden, so no empty card.
    if (!fundingInstrumentState.showsFundingInstrumentRow && !showsBrandMark) return
    val showFundingInstrument = fundingInstrumentState.showsFundingInstrumentRow
    val visibleCreditMessagingState = creditMessagingState.visibleWith(fundingInstrumentState, isEditCompleted)
    val showCreditMessaging = resolvedStyle.showPayPalCreditMessaging &&
        visibleCreditMessagingState != PayPalCreditMessagingDisplayState.Hidden
    val labelGap = resolvedStyle.labelMarginStartDp.dp
    val fundingInstrumentGap = resolvedStyle.fundingInstrumentMarginStartDp.dp

    Card(
        // The height is a minimum, so large text isn't clipped.
        modifier = modifier.heightIn(min = resolvedStyle.heightDp?.dp ?: 0.dp),
        shape = RoundedCornerShape(resolvedStyle.cornerRadiusDp.dp),
        colors = CardDefaults.cardColors(containerColor = Color(resolvedStyle.backgroundColor)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (resolvedStyle.borderWidthDp > 0f) {
            BorderStroke(resolvedStyle.borderWidthDp.dp, Color(resolvedStyle.borderColor))
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = resolvedStyle.horizontalPaddingDp.dp,
                    vertical = resolvedStyle.verticalPaddingDp.dp
                )
        ) {
            val brandMark: @Composable () -> Unit = { PayPalBrandMark(resolvedStyle, labelGap) }
            if (fundingInstrumentState == PayPalSavedPaymentMethodDisplayState.Loading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    brandMark()
                    // Tagged on the wrapper, since the placeholder replaces the semantics of its own modifier.
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .testTag(FUNDING_INSTRUMENT_SHIMMER_TEST_TAG)
                            // The gap only applies when the brand is shown.
                            .padding(start = if (showsBrandMark) fundingInstrumentGap else 0.dp)
                    ) {
                        ShimmerPlaceholder(
                            contentDescription =
                                stringResource(R.string.paypal_saved_payment_method_loading_content_description)
                        )
                    }
                }
            } else {
                SideBySideOrStacked(
                    gap = fundingInstrumentGap,
                    stackedSpacing = dimensionResource(R.dimen.paypal_saved_payment_method_stacked_layout_spacing),
                    brand = brandMark,
                    fundingInstrument = {
                        if (showFundingInstrument) {
                            FundingInstrumentChip(
                                state = fundingInstrumentState,
                                resolvedStyle = resolvedStyle,
                                editEnabled = editEnabled,
                                onEditClick = onEditClick,
                                modifier = Modifier.testTag(FUNDING_INSTRUMENT_CHIP_TEST_TAG)
                            )
                        }
                    }
                )
            }
            if (showCreditMessaging) {
                CreditMessagingRow(
                    state = visibleCreditMessagingState,
                    resolvedStyle = resolvedStyle,
                    onLearnMoreClick = onLearnMoreClick,
                    modifier = Modifier
                        .padding(
                            // Aligned with the label: after the logo if it's shown, else at the leading edge.
                            start = if (resolvedStyle.showPayPalLogo) resolvedStyle.logoWidthDp.dp + labelGap else 0.dp,
                            top = dimensionResource(R.dimen.paypal_saved_payment_method_credit_messaging_top_spacing)
                        )
                        // After the padding, so the bounds start at the message.
                        .testTag(CREDIT_MESSAGING_TEST_TAG)
                )
            }
        }
    }
}

/** The merchant's font, or the default. */
internal val PayPalSavedPaymentMethodStyleResolver.fontFamily: FontFamily
    get() = fontResId?.let { FontFamily(Font(it)) } ?: FontFamily.Default

// Tags to find the parts in tests.
internal const val BRAND_MARK_TEST_TAG = "paypal_saved_payment_method_brand_mark"
internal const val FUNDING_INSTRUMENT_SHIMMER_TEST_TAG = "paypal_saved_payment_method_funding_instrument_shimmer"
internal const val FUNDING_INSTRUMENT_CHIP_TEST_TAG = "paypal_saved_payment_method_funding_instrument_chip"
internal const val CREDIT_MESSAGING_TEST_TAG = "paypal_saved_payment_method_credit_messaging"
