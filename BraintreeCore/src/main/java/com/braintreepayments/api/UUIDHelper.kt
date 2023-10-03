package com.braintreepayments.api

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import java.util.*

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class UUIDHelper {

    /**
     * @param context Android Context
     * @return A persistent UUID for this application install.
     */
    fun getPersistentUUID(context: Context?): String? {
        return getPersistentUUID(BraintreeSharedPreferences.getInstance(context))
    }

    @VisibleForTesting
    internal fun getPersistentUUID(braintreeSharedPreferences: BraintreeSharedPreferences): String? {
        var uuid = braintreeSharedPreferences.getString(BRAINTREE_UUID_KEY, null)
        if (uuid == null) {
            uuid = formattedUUID
            braintreeSharedPreferences.putString(BRAINTREE_UUID_KEY, uuid)
        }
        return uuid
    }

    val formattedUUID: String
        get() = UUID.randomUUID().toString().replace("-", "")

    fun getInstallationGUID(context: Context?): String {
        return getInstallationGUID(BraintreeSharedPreferences.getInstance(context))
    }

    @VisibleForTesting
    internal fun getInstallationGUID(braintreeSharedPreferences: BraintreeSharedPreferences): String {
        var installationGUID = braintreeSharedPreferences.getString(INSTALL_GUID, null)
        if (installationGUID == null) {
            installationGUID = UUID.randomUUID().toString()
            braintreeSharedPreferences.putString(INSTALL_GUID, installationGUID)
        }
        return installationGUID
    }

    companion object {
        private const val BRAINTREE_UUID_KEY = "braintreeUUID"
        private const val INSTALL_GUID = "InstallationGUID"
    }
}
