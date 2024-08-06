package com.braintreepayments.api.paypal.vaultedit

import com.braintreepayments.api.ExperimentalBetaApi
import com.braintreepayments.api.core.PostalAddress

@ExperimentalBetaApi
sealed class PayPalVaultEditResult {

    /**
     * The PayPal vault edit flow completed successfully.
     */
    class Success internal constructor(
        val clientMetadataId: String,
        val payerId: String?,
        val email: String?,
        val firstName: String?,
        val lastName: String?,
        val phone: String?,
        val shippingAddress: PostalAddress?,
        val fundingSourceDescription: String?
    ) : PayPalVaultEditResult()

    /**
     * There was an [error] in the PayPal vault edit flow.
     */
    class Failure internal constructor(val error: Exception) : PayPalVaultEditResult()

    /**
     * The user canceled the PayPal vault edit flow.
     */
    object Cancel : PayPalVaultEditResult()
}
