package com.braintreepayments.api.threedsecure

/**
 * Button types that can be customized in 3D Secure 2 flows.
 */
enum class ThreeDSecureV2ButtonType {
    BUTTON_TYPE_VERIFY,
    BUTTON_TYPE_CONTINUE,
    BUTTON_TYPE_NEXT,
    BUTTON_TYPE_CANCEL,
    BUTTON_TYPE_RESEND
}
