package com.braintreepayments.api.threedsecure

internal object ThreeDSecureAnalytics {

    // Conversion Events
    const val VERIFY_STARTED = "3ds:verify:started"
    const val VERIFY_SUCCEEDED = "3ds:verify:succeeded"
    const val VERIFY_FAILED = "3ds:verify:failed"
    // cardinal sdk returns a cancellation result
    const val VERIFY_CANCELED = "3ds:verify:canceled"

    // Lookup Events
    const val LOOKUP_SUCCEEDED = "3ds:verify:lookup:succeeded"
    const val LOOKUP_FAILED = "3ds:verify:lookup:failed"
    const val CHALLENGE_REQUIRED = "3ds:verify:lookup:challenge-required"

    // JWT Events
    const val JWT_AUTH_SUCCEEDED = "3ds:verify:authenticate-jwt:succeeded"
    const val JWT_AUTH_FAILED = "3ds:verify:authenticate-jwt:failed"
}
