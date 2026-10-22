package com.braintreepayments.api.sharedutils

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4ClassRunner::class)
class OkHttpSynchronousHttpClientTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var sut: OkHttpSynchronousHttpClient

    @Before
    fun setUp() {
        val localhostCertificate = HeldCertificate.Builder()
            .addSubjectAlternativeName("localhost")
            .build()

        val serverCertificates = HandshakeCertificates.Builder()
            .heldCertificate(localhostCertificate)
            .build()

        val clientCertificates = HandshakeCertificates.Builder()
            .addTrustedCertificate(localhostCertificate.certificate)
            .build()

        mockWebServer = MockWebServer()
        // HTTP/2 has no chunked transfer encoding, so the chunked-body tests need HTTP/1.1.
        mockWebServer.protocols = listOf(Protocol.HTTP_1_1)
        mockWebServer.useHttps(serverCertificates.sslSocketFactory(), false)
        mockWebServer.start()

        sut = OkHttpSynchronousHttpClient(
            okHttpClient = OkHttpClient.Builder()
                .sslSocketFactory(
                    clientCertificates.sslSocketFactory(),
                    clientCertificates.trustManager
                )
                .build()
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun executeRequest_GET_returnsResponseBody() {
        mockWebServer.enqueue(MockResponse().setBody("hello from server").setResponseCode(200))
        val url = mockWebServer.url("/test").toString()
        val request = OkHttpRequest(url, Method.Get)

        val response = sut.executeRequest(request)

        assertEquals("hello from server", response.body)
    }

    @Test
    fun executeRequest_POST_sendsJsonBodyAndReturnsResponse() {
        mockWebServer.enqueue(MockResponse().setBody("post accepted").setResponseCode(200))
        val url = mockWebServer.url("/submit").toString()
        val jsonBody = """{"key":"value"}"""
        val request = OkHttpRequest(url, Method.Post(jsonBody))

        val response = sut.executeRequest(request)

        assertEquals("post accepted", response.body)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertEquals(jsonBody, recordedRequest.body.readUtf8())
        assertEquals("application/json; charset=utf-8", recordedRequest.getHeader("Content-Type"))
    }

    @Test(expected = IOException::class)
    fun executeRequest_unsuccessfulResponse_throwsIOException() {
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("server error"))
        val url = mockWebServer.url("/error").toString()
        val request = OkHttpRequest(url, Method.Get)

        sut.executeRequest(request)
    }

    @Test
    fun executeRequest_populatesResponseTiming() {
        mockWebServer.enqueue(MockResponse().setBody("timed").setResponseCode(200))
        val url = mockWebServer.url("/timing").toString()
        val request = OkHttpRequest(url, Method.Get)

        val beforeRequest = System.currentTimeMillis()
        val response = sut.executeRequest(request)
        val afterRequest = System.currentTimeMillis()

        assertTrue(response.timing.startTime >= beforeRequest)
        assertTrue(response.timing.endTime <= afterRequest)
        assertTrue(response.timing.endTime >= response.timing.startTime)
    }

    @Test
    fun executeRequestForBytes_returnsBinaryBodyUnchanged() {
        // PNG signature plus bytes that are invalid UTF-8, which a String round-trip would corrupt
        val bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0xFF.toByte(), 0xFE.toByte(), 0x00)
        mockWebServer.enqueue(MockResponse().setBody(Buffer().write(bytes)).setResponseCode(200))

        val result = sut.executeRequestForBytes(bytesRequest("/image"))

        assertArrayEquals(bytes, result)
    }

    @Test
    fun executeRequestForBytes_declaredLengthOverLimit_throwsIOException() {
        mockWebServer.enqueue(MockResponse().setBody(Buffer().write(ByteArray(2048))).setResponseCode(200))

        val error = assertThrows<IOException> { sut.executeRequestForBytes(bytesRequest("/big"), maxBodyBytes = 1024L) }

        assertTrue(error.message, error.message.orEmpty().contains("exceeds 1024 bytes"))
    }

    @Test
    fun executeRequestForBytes_chunkedBodyOverLimit_throwsIOException() {
        // A chunked response carries no Content-Length, so only the streaming bound can reject it.
        mockWebServer.enqueue(MockResponse().setChunkedBody(Buffer().write(ByteArray(4096)), 512))

        val error = assertThrows<IOException> {
            sut.executeRequestForBytes(bytesRequest("/chunked"), maxBodyBytes = 1024L)
        }

        assertTrue(error.message, error.message.orEmpty().contains("exceeds 1024 bytes"))
    }

    @Test
    fun executeRequestForBytes_chunkedBodyWithinLimit_returnsBody() {
        val bytes = ByteArray(4096) { (it % 251).toByte() }
        mockWebServer.enqueue(MockResponse().setChunkedBody(Buffer().write(bytes), 512))

        val result = sut.executeRequestForBytes(bytesRequest("/chunked"), maxBodyBytes = 8192L)

        assertArrayEquals(bytes, result)
    }

    @Test
    fun executeRequestForBytes_unsuccessfulResponse_throwsIOException() {
        mockWebServer.enqueue(MockResponse().setResponseCode(404).setBody("not found"))

        val error = assertThrows<IOException> { sut.executeRequestForBytes(bytesRequest("/missing")) }

        assertTrue(error.message, error.message.orEmpty().contains("404"))
    }

    @Test
    fun executeRequestForBytes_httpsRedirectedToHttp_throwsIOExceptionWithoutCleartextRequest() {
        val plainServer = MockWebServer()
        plainServer.enqueue(MockResponse().setBody("downgraded").setResponseCode(200))
        plainServer.start()
        try {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(302)
                    .setHeader("Location", plainServer.url("/downgraded").toString())
            )

            val error = assertThrows<IOException> { sut.executeRequestForBytes(bytesRequest("/redirect")) }

            // The redirect is never followed, so the cleartext server is never contacted.
            assertTrue(error.message, error.message.orEmpty().contains("302"))
            assertEquals(0, plainServer.requestCount)
        } finally {
            plainServer.shutdown()
        }
    }

    @Test
    fun executeRequestForBytes_httpsRedirectedToHttps_returnsBody() {
        val bytes = byteArrayOf(1, 2, 3)
        mockWebServer.enqueue(
            MockResponse().setResponseCode(302).setHeader("Location", mockWebServer.url("/final").toString())
        )
        mockWebServer.enqueue(MockResponse().setBody(Buffer().write(bytes)).setResponseCode(200))

        val result = sut.executeRequestForBytes(bytesRequest("/redirect"))

        assertArrayEquals(bytes, result)
    }

    @Test
    fun executeRequest_sendsCustomHeaders() {
        mockWebServer.enqueue(MockResponse().setBody("ok").setResponseCode(200))
        val url = mockWebServer.url("/headers").toString()
        val headers = mapOf("X-Custom-Header" to "custom-value", "Authorization" to "Bearer token123")
        val request = OkHttpRequest(url, Method.Get, headers)

        sut.executeRequest(request)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("custom-value", recordedRequest.getHeader("X-Custom-Header"))
        assertEquals("Bearer token123", recordedRequest.getHeader("Authorization"))
    }

    private fun bytesRequest(path: String) = OkHttpRequest(mockWebServer.url(path).toString(), Method.Get)

    private inline fun <reified T : Throwable> assertThrows(block: () -> Unit): T {
        try {
            block()
        } catch (e: Throwable) {
            if (e is T) return e
            throw e
        }
        throw AssertionError("Expected ${T::class.java.simpleName} to be thrown")
    }
}
