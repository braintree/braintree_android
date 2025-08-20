package com.braintreepayments.api.sharedutils

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.GzipSink
import okio.buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class GzipRequestInterceptorUnitTest {

    private lateinit var interceptor: GzipRequestInterceptor
    private lateinit var chain: Interceptor.Chain
    private lateinit var request: Request

    @Before
    fun setUp() {
        interceptor = GzipRequestInterceptor()
        chain = mockk(relaxed = true)
        request = Request.Builder()
            .url("https://example.com")
            .build()

        every { chain.request() } returns request
    }

    @Test
    fun `when response has gzip Content-Encoding, decompresses response body`() {
        val originalContent = "This is test content that will be gzipped"
        val gzippedContent = createGzippedContent(originalContent)

        val originalResponse = createResponse(
            body = gzippedContent.toResponseBody("text/plain".toMediaType()),
            headers = Headers.Builder()
                .add("Content-Encoding", "gzip")
                .add("Content-Length", "123")
                .build()
        )

        every { chain.proceed(request) } returns originalResponse

        val result = interceptor.intercept(chain)

        assertEquals(originalContent, result.body?.string())
        assertNull(result.header("Content-Encoding"))
        assertNull(result.header("Content-Length"))
    }

    @Test
    fun `when response has gzip Content-Encoding with mixed case, decompresses response body`() {
        val originalContent = "Mixed case gzip test"
        val gzippedContent = createGzippedContent(originalContent)

        val originalResponse = createResponse(
            body = gzippedContent.toResponseBody("text/plain".toMediaType()),
            headers = Headers.Builder()
                .add("Content-Encoding", "GZIP")
                .build()
        )

        every { chain.proceed(request) } returns originalResponse

        val result = interceptor.intercept(chain)

        assertEquals(originalContent, result.body?.string())
        assertNull(result.header("Content-Encoding"))
    }

    @Test
    fun `when response has no gzip Content-Encoding, returns original response unchanged`() {
        val originalContent = "This content is not gzipped"
        val originalResponse = createResponse(
            body = originalContent.toResponseBody("text/plain".toMediaType()),
            headers = Headers.Builder()
                .add("Content-Type", "text/plain")
                .add("Content-Length", "100")
                .build()
        )

        every { chain.proceed(request) } returns originalResponse

        val result = interceptor.intercept(chain)

        assertEquals(originalContent, result.body?.string())
        assertEquals("text/plain", result.header("Content-Type"))
        assertEquals("100", result.header("Content-Length"))
    }

    @Test
    fun `when response has different Content-Encoding, returns original response unchanged`() {
        val originalContent = "This content uses different encoding"
        val originalResponse = createResponse(
            body = originalContent.toResponseBody("text/plain".toMediaType()),
            headers = Headers.Builder()
                .add("Content-Encoding", "deflate")
                .add("Content-Length", "150")
                .build()
        )

        every { chain.proceed(request) } returns originalResponse

        val result = interceptor.intercept(chain)

        assertEquals(originalContent, result.body?.string())
        assertEquals("deflate", result.header("Content-Encoding"))
        assertEquals("150", result.header("Content-Length"))
    }

    @Test
    fun `when response has no Content-Encoding header, returns original response unchanged`() {
        val originalContent = "No encoding header"
        val originalResponse = createResponse(
            body = originalContent.toResponseBody("application/json".toMediaType()),
            headers = Headers.Builder()
                .add("Content-Type", "application/json")
                .build()
        )

        every { chain.proceed(request) } returns originalResponse

        val result = interceptor.intercept(chain)

        assertEquals(originalContent, result.body?.string())
        assertEquals("application/json", result.header("Content-Type"))
    }

    @Test
    fun `when gzipped response has null body, handles gracefully`() {
        val originalResponse = createResponse(
            body = null,
            headers = Headers.Builder()
                .add("Content-Encoding", "gzip")
                .build()
        )

        every { chain.proceed(request) } returns originalResponse

        val result = interceptor.intercept(chain)

        assertNull(result.body)
        assertNull(result.header("Content-Encoding"))
    }

    @Test
    fun `when gzipped response body preserves content type, sets content length to -1`() {
        val originalContent = "Content type preservation test"
        val gzippedContent = createGzippedContent(originalContent)
        val contentType = "application/json".toMediaType()

        val originalResponse = createResponse(
            body = gzippedContent.toResponseBody(contentType),
            headers = Headers.Builder()
                .add("Content-Encoding", "gzip")
                .add("Content-Length", "456")
                .build()
        )

        every { chain.proceed(request) } returns originalResponse

        val result = interceptor.intercept(chain)

        assertEquals(originalContent, result.body?.string())
        assertEquals(contentType, result.body?.contentType())
        assertEquals(-1L, result.body?.contentLength())
        assertNull(result.header("Content-Length"))
    }

    @Test
    fun `when response has multiple headers, only removes Content-Encoding and Content-Length`() {
        val originalContent = "Multiple headers test"
        val gzippedContent = createGzippedContent(originalContent)

        val originalResponse = createResponse(
            body = gzippedContent.toResponseBody("text/plain".toMediaType()),
            headers = Headers.Builder()
                .add("Content-Encoding", "gzip")
                .add("Content-Length", "789")
                .add("Content-Type", "text/plain")
                .add("Cache-Control", "no-cache")
                .add("Custom-Header", "custom-value")
                .build()
        )

        every { chain.proceed(request) } returns originalResponse

        val result = interceptor.intercept(chain)

        assertEquals(originalContent, result.body?.string())
        assertNull(result.header("Content-Encoding"))
        assertNull(result.header("Content-Length"))
        assertEquals("text/plain", result.header("Content-Type"))
        assertEquals("no-cache", result.header("Cache-Control"))
        assertEquals("custom-value", result.header("Custom-Header"))
    }

    @Test
    fun `intercept calls chain proceed with original request`() {
        val originalResponse = createResponse(
            body = "test".toResponseBody("text/plain".toMediaType()),
            headers = Headers.Builder().build()
        )

        every { chain.proceed(request) } returns originalResponse

        interceptor.intercept(chain)

        verify { chain.proceed(request) }
    }

    private fun createGzippedContent(content: String): ByteArray {
        val buffer = Buffer()
        val gzipSink = GzipSink(buffer).buffer()
        gzipSink.writeUtf8(content)
        gzipSink.close()
        return buffer.readByteArray()
    }

    private fun createResponse(
        body: ResponseBody?,
        headers: Headers
    ): Response {
        return Response.Builder()
            .request(request)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .headers(headers)
            .body(body)
            .build()
    }
}
