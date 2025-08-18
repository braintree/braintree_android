package com.braintreepayments.api.paypal

import org.json.JSONObject
import kotlin.test.assertEquals
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector

@RunWith(RobolectricTestParameterInjector::class)
class PayPalLineItemUnitTest {

    @Test
    fun `toJson sets keys and values`() {
        val item = PayPalLineItem(
            kind = PayPalLineItemKind.DEBIT,
            name = "An Item",
            quantity = "1",
            unitAmount = "2"
        )
        item.description = "A new item"
        item.imageUrl = "http://example.com/image.jpg"
        item.productCode = "abc-123"
        item.unitTaxAmount = "1.50"
        item.upcType = PayPalLineItemUpcType.UPC_TYPE_2
        item.upcCode = "upc-code"
        item.url = "http://example.com"

        val json: JSONObject = item.toJson()

        assertEquals("debit", json.getString("kind"))
        assertEquals("An Item", json.getString("name"))
        assertEquals("1", json.getString("quantity"))
        assertEquals("2", json.getString("unit_amount"))
        assertEquals("A new item", json.getString("description"))
        assertEquals("abc-123", json.getString("product_code"))
        assertEquals("1.50", json.getString("unit_tax_amount"))
        assertEquals("http://example.com", json.getString("url"))
        assertEquals("http://example.com/image.jpg", json.getString("image_url"))
        assertEquals("UPC-2", json.getString("upc_type"))
        assertEquals("upc-code", json.getString("upc_code"))
    }
}
