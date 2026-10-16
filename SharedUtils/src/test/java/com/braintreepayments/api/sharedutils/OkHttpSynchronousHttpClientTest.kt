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
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertFailsWith

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

    @Test
    fun `when response is successful, executeRequestForBytes sends a GET and returns the body bytes unchanged`() {
        // PNG signature plus bytes that are invalid UTF-8, which a String round-trip would corrupt
        val bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0xFF.toByte(), 0xFE.toByte(), 0x00)
        givenBytesResponse(isSuccessful = true, body = bytes.toResponseBody())

        val result = sut.executeRequestForBytes(OkHttpRequest(URL, Method.Get))

        assertArrayEquals(bytes, result)
        val slot = slot<Request>()
        verify { okHttpClient.newCall(capture(slot)) }
        assertEquals("$URL/", slot.captured.url.toString())
        assertEquals("GET", slot.captured.method)
    }

    @Test
    fun `when response is unsuccessful or has no body, executeRequestForBytes throws IOException`() {
        givenBytesResponse(isSuccessful = false, body = byteArrayOf(1).toResponseBody())
        assertFailsWith<IOException> { sut.executeRequestForBytes(OkHttpRequest(URL, Method.Get)) }

        givenBytesResponse(isSuccessful = true, body = null)
        assertFailsWith<IOException> { sut.executeRequestForBytes(OkHttpRequest(URL, Method.Get)) }
    }

    @Test
    fun `when declared body size exceeds maxBodyBytes, executeRequestForBytes throws without reading the body`() {
        givenBytesResponse(isSuccessful = true, body = responseBody)
        every { responseBody.contentLength() } returns 2048L

        assertFailsWith<IOException> {
            sut.executeRequestForBytes(OkHttpRequest(URL, Method.Get), maxBodyBytes = 1024L)
        }
        verify(exactly = 0) { responseBody.bytes() }
        verify(exactly = 0) { responseBody.source() }
    }

    @Test
    fun `when declared body size is within maxBodyBytes, executeRequestForBytes returns the body`() {
        val bytes = ByteArray(1024) { 7 }
        givenBytesResponse(isSuccessful = true, body = bytes.toResponseBody())

        val result = sut.executeRequestForBytes(OkHttpRequest(URL, Method.Get), maxBodyBytes = 1024L)

        assertArrayEquals(bytes, result)
    }

    @Test
    fun `when Content-Length is not declared and actual body is within maxBodyBytes, returns the body`() {
        val bytes = ByteArray(1024) { 7 }
        givenBytesResponse(isSuccessful = true, body = responseBody)
        every { responseBody.contentLength() } returns -1L
        every { responseBody.source() } returns Buffer().write(bytes)

        val result = sut.executeRequestForBytes(OkHttpRequest(URL, Method.Get), maxBodyBytes = 1024L)

        assertArrayEquals(bytes, result)
    }

    @Test
    fun `when Content-Length is not declared but actual body exceeds maxBodyBytes, throws without buffering it all`() {
        val bytes = ByteArray(2048) { 7 }
        givenBytesResponse(isSuccessful = true, body = responseBody)
        every { responseBody.contentLength() } returns -1L
        every { responseBody.source() } returns Buffer().write(bytes)

        assertFailsWith<IOException> {
            sut.executeRequestForBytes(OkHttpRequest(URL, Method.Get), maxBodyBytes = 1024L)
        }
    }

    @Test
    fun `when an https request is redirected to http, executeRequestForBytes throws IOException`() {
        givenBytesResponse(isSuccessful = true, body = byteArrayOf(1).toResponseBody(), finalUrl = "http://example.com")

        assertFailsWith<IOException> { sut.executeRequestForBytes(OkHttpRequest(URL, Method.Get)) }
    }

    @Test
    fun `when maxBodyBytes is Long MAX_VALUE, executeRequestForBytes returns the body`() {
        val bytes = ByteArray(1024) { 7 }
        givenBytesResponse(isSuccessful = true, body = responseBody)
        every { responseBody.contentLength() } returns -1L
        every { responseBody.source() } returns Buffer().write(bytes)

        val result = sut.executeRequestForBytes(OkHttpRequest(URL, Method.Get), maxBodyBytes = Long.MAX_VALUE)

        assertArrayEquals(bytes, result)
    }

    private fun givenBytesResponse(isSuccessful: Boolean, body: ResponseBody?, finalUrl: String = URL) {
        every { okHttpClient.newCall(any()) } returns call
        every { call.execute() } returns response
        every { response.isSuccessful } returns isSuccessful
        every { response.body } returns body
        // The request OkHttp ended on after following any redirects.
        every { response.request } returns Request.Builder().url(finalUrl).build()
    }

    companion object {
        private const val URL = "https://example.com"
    }
}
