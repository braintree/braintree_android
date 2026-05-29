package com.braintreepayments.api.paypal

import com.braintreepayments.api.paypal.PayPalPaymentIntent.Companion.fromString
import org.json.JSONObject

internal class AutoLinkTokenizeUseCase(
    private val internalPayPalClient: PayPalInternalClient
) {

    suspend operator fun invoke(session: PendingPaymentStore.PendingSession): PayPalAccountNonce {
        val account = PayPalAccount(
            clientMetadataId = session.clientMetadataId,
            urlResponseData = buildAutoLinkResponseData(session),
            intent = session.intent?.let { fromString(it) },
            merchantAccountId = session.merchantAccountId,
            paymentType = session.paymentType
        )
        return internalPayPalClient.tokenize(account)
    }

    private fun buildAutoLinkResponseData(session: PendingPaymentStore.PendingSession): JSONObject {
        return JSONObject().apply {
            put("billing_agreement_token", session.baToken)
            put("response_type", "web")
        }
    }
}
