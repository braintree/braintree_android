package com.braintreepayments.api.sharedutils

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.util.Base64
import androidx.annotation.RestrictTo
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateException

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SignatureVerifier(
    private val certificateHelper: CertificateHelper = CertificateHelper()
) {

    /**
     * Check if an app has the correct, matching, signature. Used to prevent malicious apps from
     * impersonating other apps.
     *
     * @param context                Android Context
     * @param packageName            the package name of the app to verify.
     * @param base64EncodedSignature the base64 encoded signature to verify.
     * @return true is signature is valid or signature verification has been disabled.
     */
    @Suppress("ReturnCount")
    fun isSignatureValid(
        context: Context,
        packageName: String,
        base64EncodedSignature: String
    ): Boolean {
        val signatures = try {
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
        } catch (_: NameNotFoundException) {
            return false
        }

        if (signatures == null || signatures.isEmpty()) return false

        for (signature in signatures) {
            val currentSignature = try {
                val md = MessageDigest.getInstance("SHA-256")
                val encodedCert = certificateHelper.getEncodedCertificate(signature.toByteArray())
                Base64.encodeToString(md.digest(encodedCert), Base64.DEFAULT)
            } catch (_: NoSuchAlgorithmException) {
                return false
            } catch (_: CertificateException) {
                return false
            }
            if (base64EncodedSignature != currentSignature) return false
        }
        return true
    }
}
