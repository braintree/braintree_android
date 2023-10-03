package com.braintreepayments.api

import androidx.annotation.RestrictTo
import androidx.annotation.StringDef
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object IntegrationType {
    const val CUSTOM = "custom"
    const val DROP_IN = "dropin"

    @Retention(RetentionPolicy.SOURCE)
    @StringDef(CUSTOM, DROP_IN)
    internal annotation class Integration
}
