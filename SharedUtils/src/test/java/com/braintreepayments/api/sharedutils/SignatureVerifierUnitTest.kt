package com.braintreepayments.api.sharedutils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.util.Base64
import io.mockk.every
import io.mockk.mockk
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.MessageDigest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class SignatureVerifierUnitTest {

    private lateinit var context: Context
    private lateinit var packageManager: PackageManager
    private lateinit var packageInfo: PackageInfo
    private lateinit var certificateHelper: CertificateHelper

    private lateinit var sut: SignatureVerifier

    @BeforeTest
    fun beforeEach() {
        context = mockk(relaxed = true)
        packageManager = mockk(relaxed = true)
        packageInfo = mockk(relaxed = true)
        certificateHelper = mockk(relaxed = true)

        val signature = createMockSignature("example-signature")
        val signatures = arrayOf(signature)
        packageInfo.signatures = signatures

        every { certificateHelper.getEncodedCertificate("example-signature".toByteArray()) } returns "example-signature".toByteArray()
        every { packageManager.getPackageInfo(eq("com.example"), eq(PackageManager.GET_SIGNATURES)) } returns packageInfo
        every { context.packageManager } returns packageManager

        sut = SignatureVerifier(certificateHelper)
    }

    @Test
    fun `isSignatureValid when encoded signatures match returns true`() {
        val base64EncodedSignature = base64EncodedSHA256("example-signature")
        assertTrue(sut.isSignatureValid(context, "com.example", base64EncodedSignature))
    }

    @Test
    fun `isSignatureValid when encoded signatures do not match returns false`() {
        val base64EncodedSignature = base64EncodedSHA256("different-signature")
        assertFalse(sut.isSignatureValid(context, "com.example", base64EncodedSignature))
    }

    @Test
    fun `isSignatureValid when additional signatures do not match returns false`() {
        val signatures = arrayOf(
            createMockSignature("example-signature1"),
            createMockSignature("example-signature2")
        )
        packageInfo.signatures = signatures
        every { certificateHelper.getEncodedCertificate("example-signature1".toByteArray()) } returns "example-signature1".toByteArray()
        every { certificateHelper.getEncodedCertificate("example-signature2".toByteArray()) } returns "example-signature2".toByteArray()

        val base64EncodedSignature = base64EncodedSHA256("example-signature1")
        assertFalse(sut.isSignatureValid(context, "com.example", base64EncodedSignature))
    }

    private fun base64EncodedSHA256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(input.toByteArray())
        return Base64.encodeToString(md.digest(), Base64.DEFAULT)
    }

    private fun createMockSignature(signatureContent: String): Signature {
        val signature = mockk<Signature>(relaxed = true)
        val bytes = signatureContent.toByteArray()
        every { signature.toByteArray() } returns bytes
        return signature
    }
}