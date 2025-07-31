package com.braintreepayments.api.sharedutils

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.content.pm.Signature
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
    @Suppress("SwallowedException", "ReturnCount")
    fun isSignatureValid(context: Context, packageName: String, base64EncodedSignature: String): Boolean {
        val packageManager = context.packageManager
        val signatures: Array<Signature>?
        try {
            signatures = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
        } catch (e: NameNotFoundException) {
            return false
        }
        if (signatures == null || signatures.isEmpty()) {
            return false
        }
        for (signature in signatures) {
            val currentSignature: String
            try {
                val md = MessageDigest.getInstance("SHA-256")
                val encodedCert = certificateHelper.getEncodedCertificate(signature.toByteArray())
                currentSignature = Base64.encodeToString(md.digest(encodedCert), Base64.DEFAULT)
            } catch (e: NoSuchAlgorithmException) {
                return false
            } catch (e: CertificateException) {
                return false
            }
            val validated = base64EncodedSignature == currentSignature
            if (!validated) {
                return false
            }
        }
        return true
    }
}
