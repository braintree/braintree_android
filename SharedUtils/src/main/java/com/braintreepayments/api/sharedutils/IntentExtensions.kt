package com.braintreepayments.api.sharedutils

import android.content.Intent
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.RestrictTo
import java.io.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object IntentExtensions {
    /** Although the newer getParcelableExtra(key, T::class.java) was introduced in Tiramisu (API 33),
    there seems to be an issue that throws NPE. See: https://issuetracker.google.com/issues/240585930#comment6
     Suggestion is to use the older API for Tiramisu (API 33) instead.
    */
    inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
        SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> getParcelableExtra(key, T::class.java)
        else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
    }

    inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
        SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> getParcelable(key, T::class.java)
        else -> @Suppress("DEPRECATION") getParcelable(key) as? T
    }

    inline fun <reified T : Serializable> Intent.serializable(key: String): T? = when {
        SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> getSerializableExtra(key, T::class.java)
        else -> @Suppress("DEPRECATION") getSerializableExtra(key) as? T
    }

    inline fun <reified T : Serializable> Bundle.serializable(key: String): T? = when {
        SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> getSerializable(key, T::class.java)
        else -> @Suppress("DEPRECATION") getSerializable(key) as? T
    }
}
