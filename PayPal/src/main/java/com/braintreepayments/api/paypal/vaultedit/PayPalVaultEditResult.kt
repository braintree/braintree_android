package com.braintreepayments.api.paypal.vaultedit

import com.braintreepayments.api.core.ExperimentalBetaApi

@ExperimentalBetaApi
sealed class PayPalVaultEditResult {

    /**
     * The PayPal vault edit flow completed successfully.
     *
     * @property riskCorrelationId This ID is used to link subsequent retry attempts if payment is declined
     */
    class ReadyToLaunch internal constructor(
        val riskCorrelationId: String,
        val response: EditFIAgreementSetup
    ) : PayPalVaultEditResult()

    /**
     * There was an [error] in the PayPal vault edit flow.
     */
    class Failure internal constructor(
        val error: Exception,
        val riskCorrelationId: String? = null,
    ) : PayPalVaultEditResult()

    /**
     * The user canceled the PayPal vault edit flow.
     */
    class Cancel internal constructor(
        val riskCorrelationIdZ: String
    ) : PayPalVaultEditResult()
}

data class EditFIAgreementSetup(
    val tokenId: String,
    val approvalURL: String,
    val paypalApprovalUrl: String?
)
