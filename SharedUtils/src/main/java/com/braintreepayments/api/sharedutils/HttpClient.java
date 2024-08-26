package com.braintreepayments.api.sharedutils;

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import javax.net.ssl.SSLSocketFactory;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class HttpClient {

    // NOTE: a single thread pool makes the ThreadScheduler behave like a serial dispatch queue
    private static final int THREAD_POOL_SIZE = 1;

    private final Scheduler scheduler;
    private final SynchronousHttpClient syncHttpClient;

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public HttpClient(SSLSocketFactory socketFactory, HttpResponseParser httpResponseParser) {
        this(new SynchronousHttpClient(socketFactory, httpResponseParser), new ThreadScheduler(THREAD_POOL_SIZE));
    }

    @VisibleForTesting
    HttpClient(SynchronousHttpClient syncHttpClient, Scheduler scheduler) {
        this.syncHttpClient = syncHttpClient;
        this.scheduler = scheduler;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public HttpResponse sendRequest(HttpRequest request) throws Exception {
        return syncHttpClient.request(request);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void sendRequest(HttpRequest request, NetworkResponseCallback callback) {
        scheduleRequest(request, callback);
    }

    private void scheduleRequest(final HttpRequest request, final NetworkResponseCallback callback) {
        scheduler.runOnBackground(() -> {
            try {
                HttpResponse httpResponse = syncHttpClient.request(request);
                notifySuccessOnMainThread(callback, httpResponse);
            } catch (Exception e) {
                notifyErrorOnMainThread(callback, e);
            }
        });
    }

    private void notifySuccessOnMainThread(final NetworkResponseCallback callback,
                                           final HttpResponse response) {
        if (callback != null) {
            scheduler.runOnMain(() -> callback.onResult(response, null));
        }
    }

    private void notifyErrorOnMainThread(final NetworkResponseCallback callback, final Exception e) {
        if (callback != null) {
            scheduler.runOnMain(() -> callback.onResult(null, e));
        }
    }
}
