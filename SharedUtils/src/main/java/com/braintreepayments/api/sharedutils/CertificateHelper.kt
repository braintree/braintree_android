package com.braintreepayments.api.sharedutils

import androidx.annotation.RestrictTo
import java.io.ByteArrayInputStream
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CertificateHelper {
    @Throws(CertificateException::class)
    fun getEncodedCertificate(signatureBytes: ByteArray): ByteArray {
        val certStream = ByteArrayInputStream(signatureBytes)
        val certFactory = CertificateFactory.getInstance("X509")
        val x509Cert = certFactory.generateCertificate(certStream) as X509Certificate
        return x509Cert.encoded
    }
}
