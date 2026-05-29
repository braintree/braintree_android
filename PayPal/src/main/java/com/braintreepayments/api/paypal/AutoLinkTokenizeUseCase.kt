package com.braintreepayments.api.paypal

import com.braintreepayments.api.paypal.PayPalPaymentIntent.Companion.fromString
import org.json.JSONObject

/**
 * Tokenizes a billing agreement directly with BTGW using a stored BA token,
 * bypassing the normal URL-return flow.
 *
 * In the auto-link scenario, the SDK does not have a return URL (the App Link failed).
 * Instead, the BA token is sent as a standalone `billing_agreement_token` field in the
 * `paypal_account` payload — the same field the Web SDK uses.
 *
 * @param internalPayPalClient used to call [PayPalInternalClient.tokenize]
 */
internal class AutoLinkTokenizeUseCase(
    private val internalPayPalClient: PayPalInternalClient
) {

    /**
     * Builds a [PayPalAccount] from the stored [PendingPaymentStore.PendingSession]
     * and tokenizes it via BTGW.
     *
     * @param session the captured session data from the original app switch
     * @return a [PayPalAccountNonce] on successful tokenization
     * @throws Exception on BTGW errors (e.g. 422 BILLING_AGREEMENT_NOT_APPROVED)
     */
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
