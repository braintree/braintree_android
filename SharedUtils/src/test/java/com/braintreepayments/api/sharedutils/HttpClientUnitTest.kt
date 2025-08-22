package com.braintreepayments.api.sharedutils

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HttpClientUnitTest {

    private lateinit var mockOkHttpClient: OkHttpSynchronousHttpClient
    private lateinit var mockScheduler: Scheduler
    private lateinit var mockCallback: NetworkResponseCallback
    private lateinit var sut: HttpClient

    @Before
    fun setUp() {
        mockOkHttpClient = mockk<OkHttpSynchronousHttpClient>()
        mockScheduler = mockk<Scheduler>()
        mockCallback = mockk<NetworkResponseCallback>()
        sut = HttpClient(mockOkHttpClient, mockScheduler)
    }

    @Test
    fun `when sendRequest is called, request is executed on background thread`() {
        val request = mockk<OkHttpRequest>()
        val backgroundSlot = slot<Runnable>()

        every { mockScheduler.runOnBackground(capture(backgroundSlot)) } just Runs

        sut.sendRequest(request, mockCallback)

        verify { mockScheduler.runOnBackground(any()) }
        verify(exactly = 0) { mockOkHttpClient.executeRequest(any()) }
    }

    @Test
    fun `when sendRequest is called with successful response, callback is called on main thread`() {
        val request = mockk<OkHttpRequest>()
        val mockResponse = mockk<HttpResponse>()
        val backgroundSlot = slot<Runnable>()
        val mainSlot = slot<Runnable>()

        every { mockOkHttpClient.executeRequest(request) } returns mockResponse
        every { mockScheduler.runOnBackground(capture(backgroundSlot)) } just Runs
        every { mockScheduler.runOnMain(capture(mainSlot)) } just Runs
        every { mockCallback.onResult(any(), any()) } just Runs

        sut.sendRequest(request, mockCallback)

        verify { mockScheduler.runOnBackground(any()) }
        backgroundSlot.captured.run()

        verify { mockScheduler.runOnMain(any()) }
        mainSlot.captured.run()

        verify { mockCallback.onResult(mockResponse, null) }
    }

    @Test
    fun `when sendRequest is called with exception, callback is called with error on main thread`() {
        val request = mockk<OkHttpRequest>()
        val exception = RuntimeException("Network error")
        val backgroundSlot = slot<Runnable>()
        val mainSlot = slot<Runnable>()

        every { mockOkHttpClient.executeRequest(request) } throws exception
        every { mockScheduler.runOnBackground(capture(backgroundSlot)) } just Runs
        every { mockScheduler.runOnMain(capture(mainSlot)) } just Runs
        every { mockCallback.onResult(any(), any()) } just Runs

        sut.sendRequest(request, mockCallback)

        verify { mockScheduler.runOnBackground(any()) }
        backgroundSlot.captured.run()

        verify { mockScheduler.runOnMain(any()) }
        mainSlot.captured.run()

        verify { mockCallback.onResult(null, exception) }
    }

    @Test
    fun `when sendRequest is called with null callback, no crash occurs`() {
        val request = mockk<OkHttpRequest>()
        val mockResponse = mockk<HttpResponse>()
        val backgroundSlot = slot<Runnable>()

        every { mockOkHttpClient.executeRequest(request) } returns mockResponse
        every { mockScheduler.runOnBackground(capture(backgroundSlot)) } just Runs

        sut.sendRequest(request, null)

        verify { mockScheduler.runOnBackground(any()) }
        backgroundSlot.captured.run()

        verify(exactly = 0) { mockScheduler.runOnMain(any()) }
    }

    @Test
    fun `when sendRequest is called with null callback and exception, no crash occurs`() {
        val request = mockk<OkHttpRequest>()
        val exception = RuntimeException("Network error")
        val backgroundSlot = slot<Runnable>()

        every { mockOkHttpClient.executeRequest(request) } throws exception
        every { mockScheduler.runOnBackground(capture(backgroundSlot)) } just Runs

        sut.sendRequest(request, null)

        verify { mockScheduler.runOnBackground(any()) }
        backgroundSlot.captured.run()

        verify(exactly = 0) { mockScheduler.runOnMain(any()) }
    }

    @Test
    fun `when HttpClient constructor is called without parameters, default instances are created`() {
        val sut = HttpClient()
        assertNotNull(sut)
    }

    @Test
    fun `when successful response, notifyErrorOnMainThread is not called`() {
        val request = mockk<OkHttpRequest>()
        val mockResponse = mockk<HttpResponse>()
        val backgroundSlot = slot<Runnable>()
        val mainSlot = slot<Runnable>()

        every { mockOkHttpClient.executeRequest(request) } returns mockResponse
        every { mockScheduler.runOnBackground(capture(backgroundSlot)) } just Runs
        every { mockScheduler.runOnMain(capture(mainSlot)) } just Runs
        every { mockCallback.onResult(any(), any()) } just Runs

        sut.sendRequest(request, mockCallback)
        backgroundSlot.captured.run()
        mainSlot.captured.run()

        verify(exactly = 1) { mockScheduler.runOnMain(any()) }
        verify { mockCallback.onResult(mockResponse, null) }
    }

    @Test
    fun `when IOException occurs, callback receives error`() {
        val request = mockk<OkHttpRequest>()
        val ioException = java.io.IOException("Network timeout")
        val backgroundSlot = slot<Runnable>()
        val mainSlot = slot<Runnable>()

        every { mockOkHttpClient.executeRequest(request) } throws ioException
        every { mockScheduler.runOnBackground(capture(backgroundSlot)) } just Runs
        every { mockScheduler.runOnMain(capture(mainSlot)) } just Runs
        every { mockCallback.onResult(any(), any()) } just Runs

        sut.sendRequest(request, mockCallback)
        backgroundSlot.captured.run()
        mainSlot.captured.run()

        verify { mockCallback.onResult(null, ioException) }
    }
}
