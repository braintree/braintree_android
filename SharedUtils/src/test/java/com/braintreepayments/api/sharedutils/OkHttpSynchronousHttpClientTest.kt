package com.braintreepayments.api.sharedutils

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import okhttp3.Call
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class OkHttpSynchronousHttpClientTest {

    private lateinit var okHttpClient: OkHttpClient
    private lateinit var certificatePinner: CertificatePinner
    private lateinit var sut: OkHttpSynchronousHttpClient

    private lateinit var call: Call
    private lateinit var response: Response
    private lateinit var responseBody: ResponseBody

    @Before
    fun setUp() {
        okHttpClient = mockk(relaxed = true)
        certificatePinner = mockk(relaxed = true)

        sut = OkHttpSynchronousHttpClient(
            okHttpClient = okHttpClient
        )

        call = mockk(relaxed = true)
        response = mockk(relaxed = true)
        responseBody = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `when request is GET and response is successful, executeRequest returns HttpResponse`() {
        val url = "https://example.com"
        val okHttpRequest = OkHttpRequest(url, Method.Get)
        every { okHttpClient.newCall(any()) } returns call
        every { call.execute() } returns response
        every { response.isSuccessful } returns true
        every { response.body } returns responseBody
        every { responseBody.string() } returns "response body"

        val httpResponse = sut.executeRequest(okHttpRequest)
        assertEquals("response body", httpResponse.body)
        assertTrue(httpResponse.timing.endTime >= httpResponse.timing.startTime)
    }

    @Test
    fun `when request is POST and response is successful, executeRequest returns HttpResponse`() {
        val url = "https://example.com"
        val okHttpRequest = OkHttpRequest(url, Method.Post("{\"key\":\"value\"}"))
        every { okHttpClient.newCall(any()) } returns call
        every { call.execute() } returns response
        every { response.isSuccessful } returns true
        every { response.body } returns responseBody
        every { responseBody.string() } returns "post response"

        val httpResponse = sut.executeRequest(okHttpRequest)
        assertEquals("post response", httpResponse.body)
    }

    @Test(expected = IOException::class)
    fun `when response is unsuccessful, executeRequest throws IOException`() {
        val url = "https://example.com"
        val okHttpRequest = OkHttpRequest(url, Method.Get)
        every { okHttpClient.newCall(any()) } returns call
        every { call.execute() } returns response
        every { response.isSuccessful } returns false

        sut.executeRequest(okHttpRequest)
    }

    @Test
    fun `when headers are provided, executeRequest sets headers on OkHttp Request`() {
        val url = "https://example.com"
        val headers = mapOf("Authorization" to "Bearer token", "Custom" to "Value")
        val okHttpRequest = OkHttpRequest(url, Method.Get, headers)
        every { okHttpClient.newCall(any()) } returns call
        every { call.execute() } returns response
        every { response.isSuccessful } returns true
        every { response.body } returns responseBody
        every { responseBody.string() } returns "header response"

        sut.executeRequest(okHttpRequest)
        val slot = slot<Request>()
        verify { okHttpClient.newCall(capture(slot)) }
        val capturedRequest = slot.captured
        assertEquals("Bearer token", capturedRequest.header("Authorization"))
        assertEquals("Value", capturedRequest.header("Custom"))
    }
}
