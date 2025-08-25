package com.braintreepayments.api.visacheckout

import android.os.Parcel
import kotlinx.parcelize.parcelableCreator
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.Assert.assertEquals

@RunWith(RobolectricTestRunner::class)
class VisaCheckoutUserDataUnitTest {
    private lateinit var sampleUserData: JSONObject

    @Before
    @Throws
    fun setup() {
        sampleUserData = JSONObject()
            .put("userFirstName", "userFirstName")
            .put("userLastName", "userLastName")
            .put("userFullName", "userFullName")
            .put("userName", "userName")
            .put("userEmail", "userEmail")
    }

    @Test
    fun `creates VisaCheckoutUserData from valid JSON and returns a populated object`() {
        val visaCheckoutUserData = VisaCheckoutUserData.fromJson(sampleUserData)

        assertEquals("userFirstName", visaCheckoutUserData.userFirstName)
        assertEquals("userLastName", visaCheckoutUserData.userLastName)
        assertEquals("userFullName", visaCheckoutUserData.userFullName)
        assertEquals("userName", visaCheckoutUserData.username)
        assertEquals("userEmail", visaCheckoutUserData.userEmail)
    }

    @Test
    fun `creates VisaCheckoutUserData from null JSON and returns an empty object`() {
        val visaCheckoutUserData = VisaCheckoutUserData.fromJson(null)

        assertEquals("", visaCheckoutUserData.userFirstName)
        assertEquals("", visaCheckoutUserData.userLastName)
        assertEquals("", visaCheckoutUserData.userFullName)
        assertEquals("", visaCheckoutUserData.username)
        assertEquals("", visaCheckoutUserData.userEmail)
    }

    @Test
    fun `creates VisaCheckoutUserData from JSON and parcels it correctly`() {
        val visaCheckoutUserData = VisaCheckoutUserData.fromJson(sampleUserData)

        val parcel = Parcel.obtain().apply {
            visaCheckoutUserData.writeToParcel(this, 0)
            setDataPosition(0)
        }

        val actual = parcelableCreator<VisaCheckoutUserData>().createFromParcel(parcel)

        assertEquals( visaCheckoutUserData.userFirstName, actual.userFirstName)
        assertEquals( visaCheckoutUserData.userLastName, actual.userLastName)
        assertEquals(visaCheckoutUserData.userFullName, actual.userFullName)
        assertEquals(visaCheckoutUserData.username, actual.username)
        assertEquals( visaCheckoutUserData.userEmail, actual.userEmail)
    }
}
