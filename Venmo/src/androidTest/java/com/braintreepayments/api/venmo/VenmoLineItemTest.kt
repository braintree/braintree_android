package com.braintreepayments.api.venmo

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class VenmoLineItemTest {

    @Test
    fun toJson_includesRequiredFields() {
        val item = VenmoLineItem(
            kind = VenmoLineItemKind.DEBIT,
            name = "Widget",
            quantity = 2,
            unitAmount = "10.00"
        )

        val json = item.toJson()

        assertEquals("DEBIT", json.getString("type"))
        assertEquals("Widget", json.getString("name"))
        assertEquals(2, json.getInt("quantity"))
        assertEquals("10.00", json.getString("unitAmount"))
    }

    @Test
    fun toJson_withAllOptionalFields_includesAllFields() {
        val item = VenmoLineItem(
            kind = VenmoLineItemKind.CREDIT,
            name = "Gadget",
            quantity = 1,
            unitAmount = "25.00",
            description = "A gadget",
            productCode = "SKU-123",
            unitTaxAmount = "2.00",
            url = "https://example.com/gadget"
        )

        val json = item.toJson()

        assertEquals("CREDIT", json.getString("type"))
        assertEquals("Gadget", json.getString("name"))
        assertEquals(1, json.getInt("quantity"))
        assertEquals("25.00", json.getString("unitAmount"))
        assertEquals("A gadget", json.getString("description"))
        assertEquals("SKU-123", json.getString("productCode"))
        assertEquals("2.00", json.getString("unitTaxAmount"))
        assertEquals("https://example.com/gadget", json.getString("url"))
    }

    @Test
    fun toJson_withNullOptionalFields_omitsNullFields() {
        val item = VenmoLineItem(
            kind = VenmoLineItemKind.DEBIT,
            name = "Item",
            quantity = 1,
            unitAmount = "5.00"
        )

        val json = item.toJson()

        assertFalse(json.has("description"))
        assertFalse(json.has("productCode"))
        assertFalse(json.has("unitTaxAmount"))
        assertFalse(json.has("url"))
    }

    @Test
    fun constructor_setsPropertiesCorrectly() {
        val item = VenmoLineItem(
            kind = VenmoLineItemKind.DEBIT,
            name = "Test Item",
            quantity = 3,
            unitAmount = "15.00",
            description = "Test description"
        )

        assertEquals(VenmoLineItemKind.DEBIT, item.kind)
        assertEquals("Test Item", item.name)
        assertEquals(3, item.quantity)
        assertEquals("15.00", item.unitAmount)
        assertEquals("Test description", item.description)
        assertNull(item.productCode)
        assertNull(item.unitTaxAmount)
        assertNull(item.url)
    }
}
