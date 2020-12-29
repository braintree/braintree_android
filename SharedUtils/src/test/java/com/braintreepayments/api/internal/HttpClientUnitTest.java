package com.braintreepayments.api.internal;

import com.braintreepayments.api.interfaces.HttpResponseCallback;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class HttpClientUnitTest {

    private SynchronousHttpClient syncHttpClient;

    private HttpRequest httpRequest;
    private MockThreadScheduler threadScheduler;

    @Before
    public void beforeEach() {
        syncHttpClient = mock(SynchronousHttpClient.class);
        threadScheduler = spy(new MockThreadScheduler());

        httpRequest = new HttpRequest().path("https://example.com");
    }

    @Test
    public void sendRequest_sendsRequestOnBackgroundThread() throws Exception {
        HttpClient sut = new HttpClient(syncHttpClient, threadScheduler);

        HttpResponseCallback callback = mock(HttpResponseCallback.class);
        sut.sendRequest(httpRequest, callback);

        verifyZeroInteractions(syncHttpClient);
        threadScheduler.flushBackgroundThread();

        verify(syncHttpClient).request(httpRequest);
    }

    @Test
    public void sendRequest_whenBaseHttpClientThrowsException_notifiesErrorViaCallbackOnMainThread() throws Exception {
        HttpClient sut = new HttpClient(syncHttpClient, threadScheduler);

        Exception exception = new Exception("error");
        when(syncHttpClient.request(httpRequest)).thenThrow(exception);

        HttpResponseCallback callback = mock(HttpResponseCallback.class);
        sut.sendRequest(httpRequest, callback);

        threadScheduler.flushBackgroundThread();
        verify(callback, never()).failure(exception);

        threadScheduler.flushMainThread();
        verify(callback).failure(exception);
    }

    @Test
    public void sendRequest_onBaseHttpClientRequestSuccess_notifiesSuccessViaCallbackOnMainThread() throws Exception {
        HttpClient sut = new HttpClient(syncHttpClient, threadScheduler);

        when(syncHttpClient.request(httpRequest)).thenReturn("response body");

        HttpResponseCallback callback = mock(HttpResponseCallback.class);
        sut.sendRequest(httpRequest, callback);

        threadScheduler.flushBackgroundThread();
        verify(callback, never()).success("response body");

        threadScheduler.flushMainThread();
        verify(callback).success("response body");
    }

    @Test
    public void sendRequest_whenCallbackIsNull_doesNotNotifySuccess() throws Exception {
        HttpClient sut = new HttpClient(syncHttpClient, threadScheduler);

        when(syncHttpClient.request(httpRequest)).thenReturn("response body");
        sut.sendRequest(httpRequest, null);

        threadScheduler.flushBackgroundThread();
        verify(threadScheduler, never()).runOnMain(any(Runnable.class));
    }

    @Test
    public void sendRequest_whenCallbackIsNull_doesNotNotifyError() throws Exception {
        HttpClient sut = new HttpClient(syncHttpClient, threadScheduler);

        Exception exception = new Exception("error");
        when(syncHttpClient.request(httpRequest)).thenThrow(exception);

        sut.sendRequest(httpRequest, null);

        threadScheduler.flushBackgroundThread();
        verify(threadScheduler, never()).runOnMain(any(Runnable.class));
    }

    @Test
    public void sendRequest_whenRetryMax3TimesEnabled_retriesRequest3Times() throws Exception {
        HttpClient sut = new HttpClient(syncHttpClient, threadScheduler);

        Exception exception = new Exception("error");
        when(syncHttpClient.request(httpRequest)).thenThrow(exception);

        HttpResponseCallback callback = mock(HttpResponseCallback.class);
        sut.sendRequest(httpRequest, HttpClient.RETRY_MAX_3_TIMES, callback);

        threadScheduler.flushBackgroundThread();
        verify(syncHttpClient, times(3)).request(httpRequest);
    }

    @Test
    public void sendRequest_whenRetryMax3TimesEnabled_notifiesMaxRetriesLimitExceededOnForegroundThread() throws Exception {
        HttpClient sut = new HttpClient(syncHttpClient, threadScheduler);

        Exception exception = new Exception("error");
        when(syncHttpClient.request(httpRequest)).thenThrow(exception);

        HttpResponseCallback callback = mock(HttpResponseCallback.class);
        sut.sendRequest(httpRequest, HttpClient.RETRY_MAX_3_TIMES, callback);

        threadScheduler.flushBackgroundThread();
        verify(callback, never()).failure(any(Exception.class));

        threadScheduler.flushMainThread();

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(callback).failure(captor.capture());

        HttpClientException httpClientException = (HttpClientException) captor.getValue();
        String expectedMessage = "Retry limit has been exceeded. Try again later.";
        assertEquals(expectedMessage, httpClientException.getMessage());
    }

    @Test
    public void sendRequest_whenRetryMax3TimesEnabled_futureRequestsAreAllowed() throws Exception {
        HttpClient sut = new HttpClient(syncHttpClient, threadScheduler);

        Exception exception = new Exception("error");
        when(syncHttpClient.request(httpRequest)).thenThrow(exception);

        HttpResponseCallback callback = mock(HttpResponseCallback.class);
        sut.sendRequest(httpRequest, HttpClient.RETRY_MAX_3_TIMES, callback);

        threadScheduler.flushBackgroundThread();

        reset(syncHttpClient);
        when(syncHttpClient.request(httpRequest))
                .thenThrow(exception)
                .thenReturn("response body");
        sut.sendRequest(httpRequest, HttpClient.RETRY_MAX_3_TIMES, callback);

        threadScheduler.flushBackgroundThread();
        threadScheduler.flushMainThread();

        verify(callback).success("response body");
    }

    @Test
    public void sendRequestSynchronous_sendsHttpRequest() throws Exception {
        HttpClient sut = new HttpClient(syncHttpClient, threadScheduler);

        when(syncHttpClient.request(httpRequest)).thenReturn("response body");

        String result = sut.sendRequest(httpRequest);
        assertEquals("response body", result);
    }
}
