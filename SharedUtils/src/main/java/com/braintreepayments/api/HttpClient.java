package com.braintreepayments.api;

import androidx.annotation.IntDef;
import androidx.annotation.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;

class HttpClient {

    @IntDef({ NO_RETRY, RETRY_MAX_3_TIMES })
    @Retention(RetentionPolicy.SOURCE)
    @interface RetryStrategy {
    }

    static final int NO_RETRY = 0;
    static final int RETRY_MAX_3_TIMES = 1;

    static final int MAX_RETRY_ATTEMPTS = 3;

    private final Scheduler scheduler;
    private final SynchronousHttpClient syncHttpClient;

    private final Map<URL, Integer> retryCountMap;

    protected HttpClient(SSLSocketFactory socketFactory, HttpResponseParser httpResponseParser) {
        this(new SynchronousHttpClient(socketFactory, httpResponseParser), new ThreadScheduler());
    }

    @VisibleForTesting
    HttpClient(SynchronousHttpClient syncHttpClient, Scheduler scheduler) {
        this.syncHttpClient = syncHttpClient;
        this.scheduler = scheduler;
        this.retryCountMap = new HashMap<>();
    }

    String sendRequest(HttpRequest request) throws Exception {
        return syncHttpClient.request(request).getBody();
    }

    void sendRequest(HttpRequest request, BTHttpResponseCallback callback) {
        sendRequest(request, HttpClient.NO_RETRY, callback);
    }

    void sendRequest(HttpRequest request, @RetryStrategy int retryStrategy, BTHttpResponseCallback callback) {
        scheduleRequest(request, retryStrategy, callback);
    }

    private void scheduleRequest(final HttpRequest request, @RetryStrategy final int retryStrategy, final BTHttpResponseCallback callback) {
        resetRetryCount(request);

        scheduler.runOnBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    BTHttpResponse httpResponse = syncHttpClient.request(request);
                    notifySuccessOnMainThread(callback, httpResponse);
                } catch (Exception e) {
                    switch (retryStrategy) {
                        case HttpClient.NO_RETRY:
                            notifyErrorOnMainThread(callback, e);
                            break;
                        case HttpClient.RETRY_MAX_3_TIMES:
                            retryGet(request, retryStrategy, callback);
                            break;
                    }
                }
            }
        });
    }

    private void retryGet(final HttpRequest request, @RetryStrategy final int retryStrategy, final BTHttpResponseCallback callback) {
        URL url = null;
        try {
            url = request.getURL();
        } catch (MalformedURLException | URISyntaxException ignore) {}

        if (url != null) {
            int retryCount = getNumRetriesSoFar(url);
            boolean shouldRetry = ((retryCount + 1) < MAX_RETRY_ATTEMPTS);
            if (shouldRetry) {
                scheduleRequest(request, retryStrategy, callback);
                retryCountMap.put(url, retryCount + 1);
            } else {
                String message = "Retry limit has been exceeded. Try again later.";
                HttpClientException retryLimitException = new HttpClientException(message);
                notifyErrorOnMainThread(callback, retryLimitException);
            }
        }
    }

    private int getNumRetriesSoFar(URL url) {
        Integer retryCount = retryCountMap.get(url);
        if (retryCount == null) {
            return 0;
        }
        return retryCount;
    }

    private void resetRetryCount(HttpRequest request) {
        URL url = null;
        try {
            url = request.getURL();
        } catch (MalformedURLException | URISyntaxException ignore) {}

        if (url != null) {
            retryCountMap.remove(url);
        }
    }

    private void notifySuccessOnMainThread(final BTHttpResponseCallback callback, final BTHttpResponse response) {
        if (callback != null) {
            scheduler.runOnMain(new Runnable() {
                @Override
                public void run() {
                    callback.onResult(response, null);
                }
            });
        }
    }

    private void notifyErrorOnMainThread(final BTHttpResponseCallback callback, final Exception e) {
        if (callback != null) {
            scheduler.runOnMain(new Runnable() {
                @Override
                public void run() {
                    callback.onResult(null, e);
                }
            });
        }
    }
}
