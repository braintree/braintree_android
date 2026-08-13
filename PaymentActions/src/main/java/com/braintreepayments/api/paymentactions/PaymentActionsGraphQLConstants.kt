package com.braintreepayments.api.paymentactions

internal object PaymentActionsGraphQLConstants {

    object Keys {
        const val PAYMENT_METHOD_DETAILS = "paymentMethodDetails"
        const val CREDIT_CARD = "creditCard"
        const val CREDIT_CARD_NUMBER = "number"
        const val CREDIT_CARD_EXPIRATION_MONTH = "expirationMonth"
        const val CREDIT_CARD_EXPIRATION_YEAR = "expirationYear"
        const val CREDIT_CARD_CVV = "cvv"
        const val CREDIT_CARD_CARDHOLDER_NAME = "cardholderName"
        const val CREDIT_CARD_BILLING_ADDRESS = "billingAddress"
        const val BILLING_ADDRESS_STREET_ADDRESS = "streetAddress"
        const val BILLING_ADDRESS_EXTENDED_ADDRESS = "extendedAddress"
        const val BILLING_ADDRESS_LOCALITY = "locality"
        const val BILLING_ADDRESS_REGION = "region"
        const val BILLING_ADDRESS_POSTAL_CODE = "postalCode"
        const val BILLING_ADDRESS_COUNTRY_CODE_ALPHA_2 = "countryCodeAlpha2"
    }
}
