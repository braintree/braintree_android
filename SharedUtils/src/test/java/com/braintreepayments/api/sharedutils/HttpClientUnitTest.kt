package com.braintreepayments.api.sharedutils

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class HttpClientUnitTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockOkHttpClient: OkHttpSynchronousHttpClient
    private lateinit var sut: HttpClient


    @Before
    fun setUp() {
        mockOkHttpClient = mockk<OkHttpSynchronousHttpClient>()
        sut = HttpClient(mockOkHttpClient, testDispatcher)
    }

    @Test
    fun `sendRequest is a suspend function that executes in a coroutine`() =
        runTest(testDispatcher) {
        val request = mockk<OkHttpRequest>()
        val mockResponse = mockk<HttpResponse>()

        every { mockOkHttpClient.executeRequest(request) } returns mockResponse

        // if sendRequest is not a suspend function it wouldn't be able to run within runTest coroutine
        val result = sut.sendRequest(request)

        assertEquals(mockResponse, result)
        verify { mockOkHttpClient.executeRequest(request) }
    }

    @Test
    fun `when sendRequest is called with successful response, returns response`() =
        runTest(testDispatcher) {
        val request = mockk<OkHttpRequest>()
        val mockResponse = mockk<HttpResponse>()

        every { mockOkHttpClient.executeRequest(request) } returns mockResponse

        val result = sut.sendRequest(request)

        assertEquals(mockResponse, result)
        verify { mockOkHttpClient.executeRequest(request) }
    }

    @Test
    fun `when sendRequest is called with exception, throws exception`() =
        runTest(testDispatcher) {
        val request = mockk<OkHttpRequest>()
        val exception = RuntimeException("Network error")

        every { mockOkHttpClient.executeRequest(request) } throws exception

        val thrownException = assertFailsWith<RuntimeException> {
            sut.sendRequest(request)
        }

        assertEquals("Network error", thrownException.message)
        verify { mockOkHttpClient.executeRequest(request) }
    }

    @Test
    fun `when IOException occurs, throws IOException`() =
        runTest(testDispatcher) {
        val request = mockk<OkHttpRequest>()
        val ioException = java.io.IOException("Network timeout")

        every { mockOkHttpClient.executeRequest(request) } throws ioException

        val thrownException = assertFailsWith<java.io.IOException> {
            sut.sendRequest(request)
        }

        assertEquals("Network timeout", thrownException.message)
        verify { mockOkHttpClient.executeRequest(request) }
    }

    @Test
    fun `when HttpClient constructor is called without parameters, default instances are created`() {
        val sut = HttpClient()
        assertNotNull(sut)
    }
}
