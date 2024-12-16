package com.braintreepayments.api.sharedutils

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito

class HttpClientUnitTest {
    private lateinit var syncHttpClient: SynchronousHttpClient
    private lateinit var httpRequest: HttpRequest
    private lateinit var threadScheduler: MockThreadScheduler

    private lateinit var sut: HttpClient

    @Before
    fun beforeEach() {
        syncHttpClient = Mockito.mock(SynchronousHttpClient::class.java)
        threadScheduler = Mockito.spy(MockThreadScheduler())
        httpRequest = HttpRequest().path("https://example.com")

        sut = HttpClient(syncHttpClient, threadScheduler)
    }

    @Test
    @Throws(Exception::class)
    fun sendRequest_sendsRequestOnBackgroundThread() {
        val callback = Mockito.mock(NetworkResponseCallback::class.java)
        sut.sendRequest(httpRequest, callback, HttpClient.RetryStrategy.NO_RETRY)

        Mockito.verifyNoInteractions(syncHttpClient)
        threadScheduler.flushBackgroundThread()

        Mockito.verify(syncHttpClient)?.request(httpRequest)
    }

    @Test
    @Throws(Exception::class)
    fun sendRequest_whenBaseHttpClientThrowsException_notifiesErrorViaCallbackOnMainThread() {
        val exception = Exception("error")
        Mockito.`when`(syncHttpClient.request(httpRequest)).thenThrow(exception)

        val callback = Mockito.mock(NetworkResponseCallback::class.java)
        sut.sendRequest(httpRequest, callback, HttpClient.RetryStrategy.NO_RETRY)

        threadScheduler.flushBackgroundThread()
        Mockito.verify(callback, Mockito.never()).onResult(null, exception)

        threadScheduler.flushMainThread()
        Mockito.verify(callback).onResult(null, exception)
    }

    @Test
    @Throws(Exception::class)
    fun sendRequest_onBaseHttpClientRequestSuccess_notifiesSuccessViaCallbackOnMainThread() {
        val response = HttpResponse("response body", HttpResponseTiming(123, 456))

        Mockito.`when`(syncHttpClient.request(httpRequest)).thenReturn(response)

        val callback = Mockito.mock(NetworkResponseCallback::class.java)
        sut.sendRequest(httpRequest, callback, HttpClient.RetryStrategy.NO_RETRY)

        threadScheduler.flushBackgroundThread()
        Mockito.verify(callback, Mockito.never()).onResult(response, null)

        threadScheduler.flushMainThread()
        Mockito.verify(callback).onResult(response, null)
    }

    @Test
    @Throws(Exception::class)
    fun sendRequest_whenCallbackIsNull_doesNotNotifySuccess() {
        val response = HttpResponse("response body", HttpResponseTiming(123, 456))

        Mockito.`when`(syncHttpClient.request(httpRequest)).thenReturn(response)
        sut.sendRequest(httpRequest, null, HttpClient.RetryStrategy.NO_RETRY)

        threadScheduler.flushBackgroundThread()
        Mockito.verify(threadScheduler, Mockito.never())?.runOnMain(
            ArgumentMatchers.any(Runnable::class.java)
        )
    }

    @Test
    @Throws(Exception::class)
    fun sendRequest_whenCallbackIsNull_doesNotNotifyError() {
        val exception = Exception("error")
        Mockito.`when`(syncHttpClient.request(httpRequest)).thenThrow(exception)

        sut.sendRequest(httpRequest, null, HttpClient.RetryStrategy.NO_RETRY)

        threadScheduler.flushBackgroundThread()
        Mockito.verify(threadScheduler, Mockito.never())?.runOnMain(
            ArgumentMatchers.any(
                Runnable::class.java
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun sendRequest_whenRetryMax3TimesEnabled_retriesRequest3Times() {
        val exception = Exception("error")
        Mockito.`when`(syncHttpClient.request(httpRequest)).thenThrow(exception)

        val callback = Mockito.mock(NetworkResponseCallback::class.java)
        sut.sendRequest(httpRequest, callback, HttpClient.RetryStrategy.RETRY_MAX_3_TIMES)

        threadScheduler.flushBackgroundThread()
        Mockito.verify(syncHttpClient, Mockito.times(3))?.request(httpRequest)
    }

    @Test
    @Throws(Exception::class)
    fun sendRequest_whenRetryMax3TimesEnabled_notifiesMaxRetriesLimitExceededOnForegroundThread() {
        val exception = Exception("error")
        Mockito.`when`(syncHttpClient.request(httpRequest)).thenThrow(exception)

        val callback = Mockito.mock(NetworkResponseCallback::class.java)
        sut.sendRequest(httpRequest, callback, HttpClient.RetryStrategy.RETRY_MAX_3_TIMES)

        threadScheduler.flushBackgroundThread()
        Mockito.verify(callback, Mockito.never()).onResult(
            ArgumentMatchers.isNull(),
            ArgumentMatchers.any(Exception::class.java)
        )

        threadScheduler.flushMainThread()

        val captor = ArgumentCaptor.forClass(Exception::class.java)
        Mockito.verify(callback).onResult(ArgumentMatchers.isNull(), captor.capture())

        val httpClientException = captor.value as HttpClientException
        val expectedMessage = "Retry limit has been exceeded. Try again later."
        Assert.assertEquals(expectedMessage, httpClientException.message)
    }

    @Test
    @Throws(Exception::class)
    fun sendRequest_whenRetryMax3TimesEnabled_futureRequestsAreAllowed() {
        val response = HttpResponse("response body", HttpResponseTiming(123, 456))

        val exception = Exception("error")
        Mockito.`when`(syncHttpClient.request(httpRequest)).thenThrow(exception)

        val callback = Mockito.mock(NetworkResponseCallback::class.java)
        sut.sendRequest(httpRequest, callback, HttpClient.RetryStrategy.RETRY_MAX_3_TIMES)

        threadScheduler.flushBackgroundThread()

        Mockito.reset(syncHttpClient)
        Mockito.`when`(syncHttpClient.request(httpRequest))
            .thenThrow(exception)
            .thenReturn(response)
        sut.sendRequest(httpRequest, callback, HttpClient.RetryStrategy.RETRY_MAX_3_TIMES)

        threadScheduler.flushBackgroundThread()
        threadScheduler.flushMainThread()

        Mockito.verify(callback).onResult(response, null)
    }

    @Test
    @Throws(Exception::class)
    fun sendRequestSynchronous_sendsHttpRequest() {
        val response = HttpResponse("response body", HttpResponseTiming(123, 456))

        Mockito.`when`(syncHttpClient.request(httpRequest)).thenReturn(response)

        val result = sut.sendRequest(httpRequest)
        Assert.assertEquals("response body", result)
    }
}
