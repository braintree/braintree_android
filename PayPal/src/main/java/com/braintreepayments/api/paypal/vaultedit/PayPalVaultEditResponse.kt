package com.braintreepayments.api.paypal.vaultedit

import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.core.ExperimentalBetaApi

@ExperimentalBetaApi
sealed class PayPalVaultEditResponse {

    /**
     * The PayPal vault edit flow completed successfully.
     *
     * @property riskCorrelationId This ID is used to link subsequent retry attempts if payment is declined
     */
    class ReadyToLaunch internal constructor(
        val riskCorrelationId: String,
        val response: EditFIAgreementSetup,
        internal var browserSwitchOptions: BrowserSwitchOptions? = null
    ) : PayPalVaultEditResponse()

    /**
     * There was an [error] in the PayPal vault edit flow.
     */
    class Failure internal constructor(
        val error: Exception,
        val riskCorrelationId: String? = null,
    ) : PayPalVaultEditResponse()

    /**
     * The user canceled the PayPal vault edit flow.
     */
    class Cancel internal constructor(
        val riskCorrelationIdZ: String
    ) : PayPalVaultEditResponse()
}

data class EditFIAgreementSetup(
    val tokenId: String,
    val approvalURL: String,
    val paypalApprovalUrl: String?
)

sealed class PayPalVaultEditResult {
    class Success(val riskCorrelationId: String) : PayPalVaultEditResult()
    class Failure(val error: Exception) : PayPalVaultEditResult()
    object Cancel : PayPalVaultEditResult()
}
