package com.braintreepayments.api.core

import android.content.Context
import androidx.annotation.RestrictTo
import com.braintreepayments.api.sharedutils.BraintreeSharedPreferences
import java.util.*

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class UUIDHelper {

    val formattedUUID: String
        get() = UUID.randomUUID().toString().replace("-", "")

    fun getInstallationGUID(context: Context?): String {
        return getInstallationGUID(BraintreeSharedPreferences.getInstance(context))
    }

    internal fun getInstallationGUID(braintreeSharedPreferences: BraintreeSharedPreferences): String {
        var installationGUID = braintreeSharedPreferences.getString(INSTALL_GUID, null)
        if (installationGUID == null) {
            installationGUID = UUID.randomUUID().toString()
            braintreeSharedPreferences.putString(INSTALL_GUID, installationGUID)
        }
        return installationGUID
    }

    companion object {
        private const val INSTALL_GUID = "InstallationGUID"
    }
}
