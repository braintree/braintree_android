package com.braintreepayments.api.venmo

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class VenmoAccountTest {

    @Test
    fun buildJSON_includesNonceInVenmoAccountObject() {
        val account = VenmoAccount("fake-venmo-nonce")

        val json = account.buildJSON()

        val venmoAccountJson = json.getJSONObject("venmoAccount")
        assertEquals("fake-venmo-nonce", venmoAccountJson.getString("nonce"))
    }

    @Test
    fun buildJSON_includesMetadataObject() {
        val account = VenmoAccount("fake-venmo-nonce")

        val json = account.buildJSON()

        assertNotNull(json.getJSONObject("_meta"))
    }

    @Test
    fun apiPath_returnsVenmoAccountsPath() {
        val account = VenmoAccount("fake-venmo-nonce")

        assertEquals("venmo_accounts", account.apiPath)
    }
}
