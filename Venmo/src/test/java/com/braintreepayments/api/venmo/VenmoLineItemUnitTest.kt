package com.braintreepayments.api.venmo

import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VenmoLineItemUnitTest {

    @Test
    @Throws(JSONException::class)
    fun `creates JSONObject from VenmoLineItem and preserves all keys and value correctly`() {
        val item = VenmoLineItem(
            kind = VenmoLineItemKind.DEBIT,
            name = "An Item",
            quantity = 1,
            unitAmount = "2",
            description = "A new item",
            productCode = "abc-123",
            unitTaxAmount = "1.50",
            url = "http://example.com"
        )

        val json = item.toJson()

        assertEquals("DEBIT", json.getString("type"))
        assertEquals("An Item", json.getString("name"))
        assertEquals("1", json.getString("quantity"))
        assertEquals("2", json.getString("unitAmount"))
        assertEquals("A new item", json.getString("description"))
        assertEquals("abc-123", json.getString("productCode"))
        assertEquals("1.50", json.getString("unitTaxAmount"))
        assertEquals("http://example.com", json.getString("url"))
    }
}
