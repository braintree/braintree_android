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
        const val BILLING_ADDRESS_STREET_ADDRESS = "addressLine1"
        const val BILLING_ADDRESS_EXTENDED_ADDRESS = "addressLine2"
        const val BILLING_ADDRESS_LOCALITY = "adminArea2"
        const val BILLING_ADDRESS_REGION = "adminArea1"
        const val BILLING_ADDRESS_POSTAL_CODE = "postalCode"
        const val BILLING_ADDRESS_COUNTRY_CODE_ALPHA_2 = "countryCode"
        const val CUSTOMER_ACTION_COMPLETE = "customerActionComplete"
        const val DEVICE_DATA_COLLECTION = "deviceDataCollection"
        const val DF_REFERENCE_ID = "dfReferenceId"
        const val CHALLENGE = "challenge"
        const val CARDINAL_AUTHENTICATION_JWT = "cardinalAuthenticationJWT"
    }

    /**
     * The GraphQL `__typename` values for the `nextAction` union's members.
     */
    object TypeNames {
        const val THREE_D_SECURE_AUTHENTICATION = "PaymentActionThreeDSecureAuthentication"
        const val THREE_D_SECURE_CHALLENGE = "PaymentActionThreeDSecureChallenge"
    }

    /**
     * The `paymentAction` selection set shared by every GraphQL operation that returns a
     * `PaymentAction` — `setPaymentActionPaymentMethod`, `getPaymentAction`, and
     * `signalCustomerActionComplete`. This is the schema-defined response shape, uniform
     * regardless of payment method, so it's safe to share rather than duplicate per operation.
     *
     * `nextAction` only requests fields for the union members this SDK currently supports; others
     * (redirect, QR code, etc.) parse safely as [PaymentActionNextAction.Unrecognized] until added.
     */
    const val PAYMENT_ACTION_SELECTION_SET = """
        id
        status
        nextAction {
            __typename
            ... on PaymentActionThreeDSecureAuthentication {
                bin
            }
            ... on PaymentActionThreeDSecureChallenge {
                acsUrl
                acsRenderingType
                challengePayload
            }
        }
    """
}
