package com.braintreepayments.api.paypalsavedpaymentmethod

import android.content.Intent
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class IntentExtensionsUnitTest {

    private val appLinkReturnUrl = uri(scheme = "https", host = "merchant.example.com")

    @Test
    fun `the App Link return URL is cleared, matching scheme and host in any case`() {
        val intent = intentWith(uri(scheme = "HTTPS", host = "Merchant.Example.com"))

        intent.clearPayPalReturnUrl(appLinkReturnUrl, DEEP_LINK_SCHEME)

        verify { intent.data = null }
    }

    @Test
    fun `the deep link fallback return URL is cleared by its scheme`() {
        val intent = intentWith(uri(scheme = DEEP_LINK_SCHEME, host = "x"))

        intent.clearPayPalReturnUrl(appLinkReturnUrl, DEEP_LINK_SCHEME)

        verify { intent.data = null }
    }

    @Test
    fun `the merchant's own links and an intent without data are left alone`() {
        val merchantLink = intentWith(uri(scheme = "https", host = "other.example.com"))
        val noData = intentWith(null)

        merchantLink.clearPayPalReturnUrl(appLinkReturnUrl, null)
        noData.clearPayPalReturnUrl(appLinkReturnUrl, null)

        verify(exactly = 0) { merchantLink.data = any() }
        verify(exactly = 0) { noData.data = any() }
    }

    private fun uri(scheme: String, host: String): Uri = mockk {
        every { this@mockk.scheme } returns scheme
        every { this@mockk.host } returns host
    }

    private fun intentWith(data: Uri?): Intent = mockk(relaxed = true) {
        every { this@mockk.data } returns data
    }

    private companion object {
        const val DEEP_LINK_SCHEME = "com.merchant.braintree"
    }
}
