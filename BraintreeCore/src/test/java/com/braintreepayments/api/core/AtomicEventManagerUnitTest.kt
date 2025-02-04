package com.braintreepayments.api.core;

import com.braintreepayments.api.core.atomicevent.AtomicEventManager
import io.mockk.*
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AtomicEventManagerUnitTest {

    private lateinit var httpClient: BraintreeHttpClient
    private lateinit var atomicEventManager: AtomicEventManager

    @Before
    fun setUp() {
        httpClient = mockk(relaxed = true)
        atomicEventManager = AtomicEventManager(httpClient)
    }

    @Test
    fun performStartAtomicEventsUpload() {
        val payloadSlot = slot<String>()

        every {
            httpClient.post(
                path = any(),
                data = capture(payloadSlot),
                configuration = any(),
                authorization = any(),
                callback = any()
            )

        } returns Unit

        atomicEventManager.performStartAtomicEventsUpload()

        verify {
            httpClient.post(
                path = eq("https://www.msmaster.qa.paypal.com/xoplatform/logger/api/ae"),
                data = any(),
                configuration = any(),
                authorization = any(),
                callback = any()
            )
        }

        val jsonArray = JSONArray(payloadSlot.captured)
        assertEquals(1, jsonArray.length())
    }

    @Test
    fun performEndAtomicEventUpload() {
        val payloadSlot = slot<String>()

        every {
            httpClient.post(
                path = any(),
                data = capture(payloadSlot),
                configuration = any(),
                authorization = any(),
                callback = any()
            )
        } returns Unit

        atomicEventManager.performEndAtomicEventUpload()

        verify {

            httpClient.post(
                path = eq("https://www.msmaster.qa.paypal.com/xoplatform/logger/api/ae"),
                data = any(),
                configuration = any(),
                authorization = any(),
                callback = any()
            )
        }

        val jsonArray = JSONArray(payloadSlot.captured)
        assertEquals(1, jsonArray.length())
    }

    @Test
    fun performCancelEndAtomicEventUpload() {
        val payloadSlot = slot<String>()

        every {
            httpClient.post(
                path = any(),
                data = capture(payloadSlot),
                configuration = any(),
                authorization = any(),
                callback = any()
            )
        } returns Unit

        atomicEventManager.performCancelEndAtomicEventUpload()

        verify {
            httpClient.post(
                path = eq("https://www.msmaster.qa.paypal.com/xoplatform/logger/api/ae"),
                data = any(),
                configuration = any(),
                authorization = any(),
                callback = any()
            )
        }

        val jsonArray = JSONArray(payloadSlot.captured)
        assertEquals(1, jsonArray.length())
    }
}