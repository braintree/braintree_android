package com.braintreepayments.api.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RestrictTo
import com.braintreepayments.api.sharedutils.AppHelper
import com.braintreepayments.api.sharedutils.SignatureVerifier

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DeviceInspector(
    private val appHelper: AppHelper = AppHelper(),
    private val signatureVerifier: SignatureVerifier = SignatureVerifier(),
) {

    internal fun getDeviceMetadata(
        context: Context?,
        configuration: Configuration?,
        sessionId: String?,
        integration: IntegrationType?
    ): DeviceMetadata {
        return DeviceMetadata(
            appId = context?.packageName,
            appName = getAppName(context),
            clientSDKVersion = BuildConfig.VERSION_NAME,
            clientOs = getAPIVersion(),
            component = "braintreeclientsdk",
            deviceManufacturer = Build.MANUFACTURER,
            deviceModel = Build.MODEL,
            dropInSDKVersion = dropInVersion,
            environment = configuration?.environment,
            eventSource = "mobile-native",
            integrationType = integration,
            isSimulator = isDeviceEmulator,
            merchantAppVersion = getAppVersion(context),
            merchantId = configuration?.merchantId,
            platform = "Android",
            sessionId = sessionId
        )
    }

    // Analytics payload no longer sends appInstalled info.
    // Leaving logic for upcoming PaymentReady API implementation.
    /**
     * @param context A context to access the installed packages.
     * @return boolean depending on if the Venmo app is installed, and has a valid signature.
     */
    fun isVenmoAppSwitchAvailable(context: Context?): Boolean {
        val isVenmoIntentAvailable = appHelper.isIntentAvailable(context, venmoIntent)
        val isVenmoSignatureValid = signatureVerifier.isSignatureValid(
            context, VENMO_APP_PACKAGE, VENMO_BASE_64_ENCODED_SIGNATURE
        )
        return isVenmoIntentAvailable && isVenmoSignatureValid
    }

    fun isPayPalInstalled(context: Context?): Boolean {
        return appHelper.isAppInstalled(context, PAYPAL_APP_PACKAGE)
    }

    fun isPayPalBetaInstalled(context: Context?): Boolean {
        return appHelper.isAppInstalled(context, PAYPAL_APP_PACKAGE_BETA)
    }

    fun isVenmoInstalled(context: Context?): Boolean {
        return appHelper.isAppInstalled(context, VENMO_APP_PACKAGE)
    }

    private val isDeviceEmulator: Boolean
        get() = Build.BRAND.startsWith("generic") &&
            Build.DEVICE.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.HARDWARE.contains("goldfish") ||
            Build.HARDWARE.contains("ranchu") ||
            Build.MODEL.contains("google_sdk") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for x86") ||
            Build.MANUFACTURER.contains("Genymotion") ||
            Build.PRODUCT.contains("sdk_google") ||
            Build.PRODUCT.contains("google_sdk") ||
            Build.PRODUCT.contains("sdk") ||
            Build.PRODUCT.contains("sdk_x86") ||
            Build.PRODUCT.contains("vbox86p") ||
            Build.PRODUCT.contains("emulator") ||
            Build.PRODUCT.contains("simulator")

    private fun getAppName(context: Context?): String =
        getApplicationInfo(context)?.let { appInfo ->
                context?.packageManager?.getApplicationLabel(appInfo).toString()
        } ?: "ApplicationNameUnknown"

    @Suppress("SwallowedException")
    private fun getApplicationInfo(context: Context?) =
        try {
            context?.packageManager?.getApplicationInfo(context.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }

    private fun getAppVersion(context: Context?): String = getPackageInfo(context) ?: "VersionUnknown"

    private fun getPackageInfo(context: Context?) =
        context?.let {
            try {
                val packageInfo = it.packageManager.getPackageInfo(it.packageName, 0)
                packageInfo?.versionName
            } catch (ignored: PackageManager.NameNotFoundException) { null }
        }

    private fun getAPIVersion(): String {
        val sdkInt = Build.VERSION.SDK_INT
        return "Android API $sdkInt"
    }

    /**
     * Gets the current Drop-in version or null.
     *
     * @return string representation of the current Drop-in version, or null if
     * Drop-in is unavailable
     */
    private val dropInVersion
        get() = getDropInVersion()

    companion object {
        private const val PAYPAL_APP_PACKAGE = "com.paypal.android.p2pmobile"
        private const val PAYPAL_APP_PACKAGE_BETA = "com.paypal.android.p2pmobile.pp_beta"
        private const val VENMO_APP_PACKAGE = "com.venmo"
        private const val VENMO_APP_SWITCH_ACTIVITY = "controller.SetupMerchantActivity"

        const val VENMO_BASE_64_ENCODED_SIGNATURE = "x34mMawEUcCG8l95riWCOK+kAJYejVmdt44l6tzcyUc=\n"
        private val venmoIntent: Intent
            get() = Intent().setComponent(
                ComponentName(
                    VENMO_APP_PACKAGE,
                    "$VENMO_APP_PACKAGE.$VENMO_APP_SWITCH_ACTIVITY"
                )
            )
        internal fun getDropInVersion(): String? {
            try {
                val dropInBuildConfigClass = Class.forName("com.braintreepayments.api.dropin.BuildConfig")
                val versionNameField = dropInBuildConfigClass.getField("VERSION_NAME")
                versionNameField.isAccessible = true
                return versionNameField[String::class] as String?
            } catch (ignored: ClassNotFoundException) {
            } catch (ignored: NoSuchFieldException) {
            } catch (ignored: IllegalAccessException) {
            }
            return null
        }
    }
}
