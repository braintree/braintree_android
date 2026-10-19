@file:OptIn(ExperimentalBetaApi::class)
@file:Suppress("UnusedPrivateMember")

package com.braintreepayments.api.paypalsavedpaymentmethod.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalCreditMessagingContent
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalCreditMessagingDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethod
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethodDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethodType
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.ComponentAppearance
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.ContainerStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.CreditMessagingStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodViewStyle

private val card = PayPalSavedPaymentMethodDisplayState.Content(
    PayPalSavedPaymentMethod(
        type = PayPalSavedPaymentMethodType.CARD,
        label = "Visa",
        imageUrl = null,
        lastDigits = "1234",
        subtype = null
    )
)

private val payPalCredit = PayPalSavedPaymentMethodDisplayState.Content(
    PayPalSavedPaymentMethod(
        type = PayPalSavedPaymentMethodType.PAYPAL_CREDIT,
        label = "PayPal Credit",
        imageUrl = null,
        lastDigits = "0000",
        subtype = null
    )
)

private val payLaterMessage = PayPalCreditMessagingDisplayState.Content(
    PayPalCreditMessagingContent(
        message = "Pay in 4 interest-free payments of \$13.75.",
        learnMoreText = "Learn more",
        learnMoreUrl = "https://www.paypal.com"
    )
)

private const val PREVIEW_WIDTH_DP = 320

/**
 * A bank with last digits, shown masked, and one without, whose label is shown instead and is too long for the row.
 */
private class BankProvider : PreviewParameterProvider<PayPalSavedPaymentMethodDisplayState> {
    override val values = sequenceOf("6789", null).map { lastDigits ->
        PayPalSavedPaymentMethodDisplayState.Content(
            PayPalSavedPaymentMethod(
                type = PayPalSavedPaymentMethodType.BANK,
                label = if (lastDigits != null) "Chase" else "JPMorgan Chase Bank Total Checking Account",
                imageUrl = null,
                lastDigits = lastDigits,
                subtype = null
            )
        )
    }
}

/** A one-line message, and one that wraps onto three lines. */
private class PayLaterMessageProvider : PreviewParameterProvider<PayPalCreditMessagingDisplayState> {
    override val values = sequenceOf(
        payLaterMessage,
        PayPalCreditMessagingDisplayState.Content(
            PayPalCreditMessagingContent(
                message = "Pay in 4 interest-free payments of \$13.75 or pay monthly over 6, 12 or 24 months " +
                    "with PayPal Pay Later, subject to approval.",
                learnMoreText = "Learn more",
                learnMoreUrl = "https://www.paypal.com"
            )
        )
    )
}

/** A short email, and one too long for the row so the truncation can be checked. */
private class PayerEmailProvider : PreviewParameterProvider<String> {
    override val values = sequenceOf(
        "buyer@example.com",
        "firstname.middlename.lastname.longaddress@example.com"
    )
}

private val customStyle = PayPalSavedPaymentMethodViewStyle(
    showPayPalLogo = false,
    componentAppearance = ComponentAppearance(
        backgroundColor = 0xFFF5F7FA.toInt(),
        textColor = 0xFF1A1A1A.toInt()
    ),
    container = ContainerStyle(
        horizontalPaddingDp = 16f,
        cornerRadiusDp = 12f,
        borderColor = 0xFFC4C4C4.toInt(),
        borderWidthDp = 1f,
        creditMessaging = CreditMessagingStyle(linkColor = 0xFF0070E0.toInt())
    )
)

@Preview(name = "Loading", showBackground = true)
@Composable
private fun PreviewLoading() {
    PayPalSavedPaymentMethodContent(
        fundingInstrumentState = PayPalSavedPaymentMethodDisplayState.Loading,
        creditMessagingState = PayPalCreditMessagingDisplayState.Loading,
        style = PayPalSavedPaymentMethodViewStyle(),
        onEditClick = {},
        onLearnMoreClick = {}
    )
}

@Preview(name = "Card with Pay Later message", showBackground = true, widthDp = PREVIEW_WIDTH_DP)
@Composable
private fun PreviewCardWithMessage(
    @PreviewParameter(PayLaterMessageProvider::class) creditMessagingState: PayPalCreditMessagingDisplayState
) {
    PayPalSavedPaymentMethodContent(
        fundingInstrumentState = card,
        creditMessagingState = creditMessagingState,
        style = PayPalSavedPaymentMethodViewStyle(),
        onEditClick = {},
        onLearnMoreClick = {}
    )
}

@Preview(name = "Card, Pay Later message loading", showBackground = true)
@Composable
private fun PreviewCardWithMessageLoading() {
    PayPalSavedPaymentMethodContent(
        fundingInstrumentState = card,
        creditMessagingState = PayPalCreditMessagingDisplayState.Loading,
        style = PayPalSavedPaymentMethodViewStyle(),
        onEditClick = {},
        onLearnMoreClick = {}
    )
}

@Preview(name = "Bank account, no Pay Later message", showBackground = true, widthDp = PREVIEW_WIDTH_DP)
@Composable
private fun PreviewBank(@PreviewParameter(BankProvider::class) bank: PayPalSavedPaymentMethodDisplayState) {
    PayPalSavedPaymentMethodContent(
        fundingInstrumentState = bank,
        creditMessagingState = PayPalCreditMessagingDisplayState.Hidden,
        style = PayPalSavedPaymentMethodViewStyle(),
        onEditClick = {},
        onLearnMoreClick = {}
    )
}

@Preview(name = "PayPal Credit", showBackground = true)
@Composable
private fun PreviewPayPalCredit() {
    PayPalSavedPaymentMethodContent(
        fundingInstrumentState = payPalCredit,
        creditMessagingState = PayPalCreditMessagingDisplayState.Hidden,
        style = PayPalSavedPaymentMethodViewStyle(),
        onEditClick = {},
        onLearnMoreClick = {}
    )
}

@Preview(name = "Email, editable, with Pay Later message", showBackground = true, widthDp = PREVIEW_WIDTH_DP)
@Composable
private fun PreviewEmailEditable(@PreviewParameter(PayerEmailProvider::class) payerEmail: String) {
    PayPalSavedPaymentMethodContent(
        fundingInstrumentState = PayPalSavedPaymentMethodDisplayState.NoFundingInstrument(
            payerEmail = payerEmail,
            isEditable = true
        ),
        creditMessagingState = payLaterMessage,
        style = PayPalSavedPaymentMethodViewStyle(),
        onEditClick = {},
        onLearnMoreClick = {}
    )
}

@Preview(
    name = "Email, not editable (Pay Later message hidden)",
    showBackground = true,
    widthDp = PREVIEW_WIDTH_DP
)
@Composable
private fun PreviewEmailNotEditable(@PreviewParameter(PayerEmailProvider::class) payerEmail: String) {
    PayPalSavedPaymentMethodContent(
        fundingInstrumentState = PayPalSavedPaymentMethodDisplayState.NoFundingInstrument(
            payerEmail = payerEmail,
            isEditable = false
        ),
        creditMessagingState = payLaterMessage,
        style = PayPalSavedPaymentMethodViewStyle(),
        onEditClick = {},
        onLearnMoreClick = {}
    )
}

@Preview(name = "Offline or error, logo only", showBackground = true)
@Composable
private fun PreviewLogoOnly() {
    PayPalSavedPaymentMethodContent(
        fundingInstrumentState = PayPalSavedPaymentMethodDisplayState.NoNetwork,
        creditMessagingState = payLaterMessage,
        style = PayPalSavedPaymentMethodViewStyle(),
        onEditClick = {},
        onLearnMoreClick = {}
    )
}

@Preview(name = "After an edit, refreshing the funding instrument", showBackground = true)
@Composable
private fun PreviewRefreshingAfterEdit() {
    PayPalSavedPaymentMethodContent(
        fundingInstrumentState = PayPalSavedPaymentMethodDisplayState.Loading,
        creditMessagingState = payLaterMessage,
        style = PayPalSavedPaymentMethodViewStyle(),
        onEditClick = {},
        onLearnMoreClick = {},
        isEditCompleted = true
    )
}

@Preview(name = "Custom style, no logo", showBackground = true)
@Composable
private fun PreviewCustomStyle() {
    PayPalSavedPaymentMethodContent(
        fundingInstrumentState = card,
        creditMessagingState = payLaterMessage,
        style = customStyle,
        onEditClick = {},
        onLearnMoreClick = {}
    )
}
