package com.braintreepayments.api.core

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class PostalAddressParserTest {

    @Test(timeout = 1000)
    fun fromJson_withNullInput_returnsEmptyPostalAddress() {
        val result = PostalAddressParser.fromJson(null)
        assertNotNull(result)
        assertTrue(result.isEmpty)
    }

    @Test(timeout = 1000)
    fun fromJson_withStandardKeys_parsesAllFields() {
        val json = JSONObject().apply {
            put("recipientName", "John Doe")
            put("street1", "123 Main St")
            put("street2", "Apt 4")
            put("city", "San Jose")
            put("state", "CA")
            put("postalCode", "95131")
            put("country", "US")
        }

        val result = PostalAddressParser.fromJson(json)

        assertEquals("John Doe", result.recipientName)
        assertEquals("123 Main St", result.streetAddress)
        assertEquals("Apt 4", result.extendedAddress)
        assertEquals("San Jose", result.locality)
        assertEquals("CA", result.region)
        assertEquals("95131", result.postalCode)
        assertEquals("US", result.countryCodeAlpha2)
    }

    @Test(timeout = 1000)
    fun fromJson_withAlternateKeys_parsesCorrectly() {
        val json = JSONObject().apply {
            put("line1", "456 Oak Ave")
            put("line2", "Suite 200")
            put("city", "Austin")
            put("state", "TX")
            put("postalCode", "78701")
            put("countryCode", "US")
        }

        val result = PostalAddressParser.fromJson(json)

        assertEquals("456 Oak Ave", result.streetAddress)
        assertEquals("Suite 200", result.extendedAddress)
        assertEquals("Austin", result.locality)
        assertEquals("TX", result.region)
        assertEquals("78701", result.postalCode)
        assertEquals("US", result.countryCodeAlpha2)
    }

    @Test(timeout = 1000)
    fun fromJson_withVenmoGqlKeys_parsesCorrectly() {
        val json = JSONObject().apply {
            put("fullName", "Jane Smith")
            put("addressLine1", "789 Elm St")
            put("addressLine2", "Floor 3")
            put("adminArea2", "Portland")
            put("adminArea1", "OR")
            put("postalCode", "97201")
            put("countryCode", "US")
        }

        val result = PostalAddressParser.fromJson(json)

        assertEquals("Jane Smith", result.recipientName)
        assertEquals("789 Elm St", result.streetAddress)
        assertEquals("Floor 3", result.extendedAddress)
        assertEquals("Portland", result.locality)
        assertEquals("OR", result.region)
        assertEquals("97201", result.postalCode)
        assertEquals("US", result.countryCodeAlpha2)
    }

    @Test(timeout = 1000)
    fun fromJson_withUserAddressFormat_parsesCorrectly() {
        val json = JSONObject().apply {
            put("name", "Bob Johnson")
            put("phoneNumber", "555-1234")
            put("address1", "321 Pine Rd")
            put("address2", "Building A")
            put("address3", "Wing B")
            put("address4", "")
            put("address5", "")
            put("locality", "Denver")
            put("administrativeArea", "CO")
            put("postalCode", "80202")
            put("countryCode", "US")
            put("sortingCode", "SORT1")
        }

        val result = PostalAddressParser.fromJson(json)

        assertEquals("Bob Johnson", result.recipientName)
        assertEquals("555-1234", result.phoneNumber)
        assertEquals("321 Pine Rd", result.streetAddress)
        assertEquals("Denver", result.locality)
        assertEquals("CO", result.region)
        assertEquals("80202", result.postalCode)
        assertEquals("US", result.countryCodeAlpha2)
        assertEquals("SORT1", result.sortingCode)
    }

    @Test(timeout = 1000)
    fun fromJson_withUserAddressFormat_formatsExtendedAddress() {
        val json = JSONObject().apply {
            put("name", "Test User")
            put("address1", "100 Main St")
            put("address2", "Line 2")
            put("address3", "Line 3")
            put("address4", "Line 4")
            put("address5", "Line 5")
        }

        val result = PostalAddressParser.fromJson(json)

        val extendedAddress = requireNotNull(result.extendedAddress)
        assertTrue(extendedAddress.contains("Line 2"))
        assertTrue(extendedAddress.contains("Line 3"))
        assertTrue(extendedAddress.contains("Line 4"))
        assertTrue(extendedAddress.contains("Line 5"))
    }

    @Test(timeout = 1000)
    fun fromJson_withEmptyJson_returnsPostalAddressWithNullFields() {
        val result = PostalAddressParser.fromJson(JSONObject())

        assertNull(result.recipientName)
        assertNull(result.streetAddress)
        assertNull(result.extendedAddress)
        assertNull(result.locality)
        assertNull(result.region)
        assertNull(result.postalCode)
        assertNull(result.countryCodeAlpha2)
    }

    @Test(timeout = 1000)
    fun fromJson_standardKeysTakePrecedenceOverAlternateKeys() {
        val json = JSONObject().apply {
            put("street1", "Primary Street")
            put("line1", "Alternate Street")
            put("country", "US")
            put("countryCode", "CA")
        }

        val result = PostalAddressParser.fromJson(json)

        assertEquals("Primary Street", result.streetAddress)
        assertEquals("US", result.countryCodeAlpha2)
    }
}