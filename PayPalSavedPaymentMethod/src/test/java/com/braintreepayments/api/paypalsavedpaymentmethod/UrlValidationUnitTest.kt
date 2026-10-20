package com.braintreepayments.api.paypalsavedpaymentmethod

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UrlValidationUnitTest {

    @Test
    fun `asHttpsUrlOrNull keeps an https url`() {
        assertEquals("https://paypal.com/logo.png", "https://paypal.com/logo.png".asHttpsUrlOrNull())
    }

    @Test
    fun `asHttpsUrlOrNull matches the scheme case-insensitively`() {
        assertEquals("HTTPS://paypal.com", "HTTPS://paypal.com".asHttpsUrlOrNull())
    }

    @Test
    fun `asHttpsUrlOrNull returns null for an http url`() {
        assertNull("http://paypal.com".asHttpsUrlOrNull())
    }

    @Test
    fun `asHttpsUrlOrNull returns null for a javascript url`() {
        assertNull("javascript:alert(1)".asHttpsUrlOrNull())
    }

    @Test
    fun `asHttpsUrlOrNull returns null for an https url without a host`() {
        assertNull("https:paypal.com".asHttpsUrlOrNull())
    }

    @Test
    fun `asHttpsUrlOrNull returns null for a relative url`() {
        assertNull("/logo.png".asHttpsUrlOrNull())
    }

    @Test
    fun `asHttpsUrlOrNull returns null for an empty string`() {
        assertNull("".asHttpsUrlOrNull())
    }

    @Test
    fun `asHttpsUrlOrNull returns null for null`() {
        assertNull(null.asHttpsUrlOrNull())
    }
}
