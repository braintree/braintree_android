package com.braintreepayments.api.paypal.vaultedit

import com.braintreepayments.api.core.ExperimentalBetaApi

@ExperimentalBetaApi
sealed class PayPalVaultEditResult {

    /**
     * The PayPal vault edit flow completed successfully.
     *
     * @property riskCorrelationId This ID is used to link subsequent retry attempts if payment is declined
     */
    class Success internal constructor(
        val riskCorrelationId: String,
    ) : PayPalVaultEditResult()

//    {
//        "agreementSetup": {
//        "tokenId": "BA-3178271115069990W",
//        "approvalUrl": "https://www.sandbox.paypal.com/agreements/approve?ba_token=BA-3178271115069990W&platform=android",
//        "paypalAppApprovalUrl": null
//    }
//    }

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
