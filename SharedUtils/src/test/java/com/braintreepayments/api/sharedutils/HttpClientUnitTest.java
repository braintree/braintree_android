package com.braintreepayments.api.sharedutils;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

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

        NetworkResponseCallback callback = mock(NetworkResponseCallback.class);
        sut.sendRequest(httpRequest, callback);

        verifyNoInteractions(syncHttpClient);
        threadScheduler.flushBackgroundThread();

        verify(syncHttpClient).request(httpRequest);
    }

    @Test
    public void sendRequest_whenBaseHttpClientThrowsException_notifiesErrorViaCallbackOnMainThread()
            throws Exception {
        HttpClient sut = new HttpClient(syncHttpClient, threadScheduler);

        Exception exception = new Exception("error");
        when(syncHttpClient.request(httpRequest)).thenThrow(exception);

        NetworkResponseCallback callback = mock(NetworkResponseCallback.class);
        sut.sendRequest(httpRequest, callback);

        threadScheduler.flushBackgroundThread();
        verify(callback, never()).onResult(null, exception);

        threadScheduler.flushMainThread();
        verify(callback).onResult(null, exception);
    }

    @Test
    public void sendRequest_onBaseHttpClientRequestSuccess_notifiesSuccessViaCallbackOnMainThread()
            throws Exception {
        HttpClient sut = new HttpClient(syncHttpClient, threadScheduler);
        HttpResponse response = new HttpResponse("response body", new HttpResponseTiming(123, 456));

        when(syncHttpClient.request(httpRequest)).thenReturn(response);

        NetworkResponseCallback callback = mock(NetworkResponseCallback.class);
        sut.sendRequest(httpRequest, callback);

        threadScheduler.flushBackgroundThread();
        verify(callback, never()).onResult(response, null);

        threadScheduler.flushMainThread();
        verify(callback).onResult(response, null);
    }

    @Test
    public void sendRequest_whenCallbackIsNull_doesNotNotifySuccess() throws Exception {
        HttpClient sut = new HttpClient(syncHttpClient, threadScheduler);
        HttpResponse response = new HttpResponse("response body", new HttpResponseTiming(123, 456));

        when(syncHttpClient.request(httpRequest)).thenReturn(response);
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
    public void sendRequestSynchronous_sendsHttpRequest() throws Exception {
        HttpClient sut = new HttpClient(syncHttpClient, threadScheduler);
        HttpResponse response = new HttpResponse("response body", new HttpResponseTiming(123, 456));

        when(syncHttpClient.request(httpRequest)).thenReturn(response);

        String result = sut.sendRequest(httpRequest);
        assertEquals("response body", result);
    }
}
