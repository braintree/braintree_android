package com.braintreepayments.api.sharedutils;

import androidx.annotation.RestrictTo;

/**
 * Exception thrown when a 426 HTTP_UPGRADE_REQUIRED response is encountered. Indicates that the
 * API used or current SDK version is deprecated and must be updated.
 */
public class UpgradeRequiredException extends Exception {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    UpgradeRequiredException(String message) {
        super(message);
    }
}
