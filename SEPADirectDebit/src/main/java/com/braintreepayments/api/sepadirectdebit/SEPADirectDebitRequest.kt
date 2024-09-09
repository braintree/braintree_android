package com.braintreepayments.api.sepadirectdebit

import com.braintreepayments.api.core.PostalAddress

/**
 * Parameters for creating a SEPA Direct Debit tokenization request.
 *
 * @property accountHolderName The account holder name.
 * @property iban The full IBAN.
 * @property customerId The customer ID.
 * @property mandateType The [SEPADirectDebitMandateType].
 * @property billingAddress The user's billing address.
 * @property merchantAccountId A non-default merchant account to use for tokenization.
 * Optional.
 * @property locale A locale code to use for creating a mandate.
 * @see [Documentation](https://developer.paypal.com/reference/locale-codes/)
 * for possible values. Locale code should be supplied as a BCP-47 formatted locale code.
 */
data class SEPADirectDebitRequest internal constructor(
    var accountHolderName: String? = null,
    var iban: String? = null,
    var customerId: String? = null,
    var mandateType: SEPADirectDebitMandateType = SEPADirectDebitMandateType.ONE_OFF,
    var billingAddress: PostalAddress? = null,
    var merchantAccountId: String? = null,
    var locale: String? = null
)
