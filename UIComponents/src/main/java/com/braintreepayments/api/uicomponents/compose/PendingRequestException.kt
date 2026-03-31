package com.braintreepayments.api.uicomponents.compose

import androidx.annotation.RestrictTo
import com.braintreepayments.api.core.BraintreeException

/**
 * Error class thrown when there's an issue fetching the pending request to complete the flow.
 */
class PendingRequestException @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) constructor(
    message: String? = "Unable to recover pending request. Cannot complete flow."
) : BraintreeException(message)
