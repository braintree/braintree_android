package com.braintreepayments.api.sharedutils

import android.content.pm.PackageManager
import android.util.Base64
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest

@RunWith(AndroidJUnit4ClassRunner::class)
class SignatureVerifierTest {

    private lateinit var sut: SignatureVerifier

    @Before
    fun setUp() {
        sut = SignatureVerifier()
    }

    @Suppress("deprecation")
    @Test
    fun isSignatureValid_withRealAppSignature_returnsTrue() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageName = context.packageName

        val packageInfo = context.packageManager
            .getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
        val signature = requireNotNull(packageInfo.signatures).first()
        val encodedCert = CertificateHelper().getEncodedCertificate(signature.toByteArray())
        val md = MessageDigest.getInstance("SHA-256")
        val expectedSignature = Base64.encodeToString(md.digest(encodedCert), Base64.DEFAULT)

        assertTrue(sut.isSignatureValid(context, packageName, expectedSignature))
    }

    @Test
    fun isSignatureValid_withWrongSignature_returnsFalse() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageName = context.packageName

        assertFalse(sut.isSignatureValid(context, packageName, "wrong-signature"))
    }

    @Test
    fun isSignatureValid_withNonExistentPackage_returnsFalse() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        assertFalse(
            sut.isSignatureValid(
                context,
                "com.nonexistent.package.that.does.not.exist",
                "any-signature"
            )
        )
    }
}
