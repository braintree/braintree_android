package com.braintreepayments.api.googlepay

import android.os.Parcel
import kotlinx.parcelize.parcelableCreator
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class GooglePayDisplayItemTest {

    @Test
    fun `constructor sets properties`() {
        val sut = GooglePayDisplayItem(
            label = "item-label",
            type = GooglePayDisplayItemType.LINE_ITEM,
            price = "10.00",
            status = GooglePayDisplayItemStatus.PENDING
        )

        assertEquals("item-label", sut.label)
        assertEquals(GooglePayDisplayItemType.LINE_ITEM, sut.type)
        assertEquals("10.00", sut.price)
        assertEquals(GooglePayDisplayItemStatus.PENDING, sut.status)
    }

    @Test
    fun `constructor uses default status when not provided`() {
        val sut = GooglePayDisplayItem(
            label = "item-label",
            type = GooglePayDisplayItemType.LINE_ITEM,
            price = "10.00"
        )

        assertEquals("item-label", sut.label)
        assertEquals(GooglePayDisplayItemType.LINE_ITEM, sut.type)
        assertEquals("10.00", sut.price)
        assertEquals(GooglePayDisplayItemStatus.FINAL, sut.status)
    }

    @Test
    fun `toJson serializes all properties`() {
        val sut = GooglePayDisplayItem(
            label = "item-label",
            type = GooglePayDisplayItemType.LINE_ITEM,
            price = "10.00",
            status = GooglePayDisplayItemStatus.PENDING
        )
        val expectedJson = JSONObject()
            .put("label", "item-label")
            .put("type", "LINE_ITEM")
            .put("price", "10.00")
            .put("status", "PENDING")

        JSONAssert.assertEquals(expectedJson, sut.toJson(), true)
    }

    @Test
    fun `parcels correctly`() {
        val displayItem = GooglePayDisplayItem(
            label = "item-label",
            type = GooglePayDisplayItemType.LINE_ITEM,
            price = "10.00",
            status = GooglePayDisplayItemStatus.PENDING
        )

        val parcel = Parcel.obtain()
        displayItem.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val parceled = parcelableCreator<GooglePayDisplayItem>().createFromParcel(parcel)

        assertEquals("item-label", parceled.label)
        assertEquals(GooglePayDisplayItemType.LINE_ITEM, parceled.type)
        assertEquals("10.00", parceled.price)
        assertEquals(GooglePayDisplayItemStatus.PENDING, parceled.status)
    }
}
