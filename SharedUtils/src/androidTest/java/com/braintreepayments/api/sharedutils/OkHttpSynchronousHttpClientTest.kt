package com.braintreepayments.api.sharedutils

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.After
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
}
