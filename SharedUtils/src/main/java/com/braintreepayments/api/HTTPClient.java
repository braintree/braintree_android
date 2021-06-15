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

class HTTPClient {

    @IntDef({ NO_RETRY, RETRY_MAX_3_TIMES })
    @Retention(RetentionPolicy.SOURCE)
    @interface RetryStrategy {
    }

    static final int NO_RETRY = 0;
    static final int RETRY_MAX_3_TIMES = 1;

    static final int MAX_RETRY_ATTEMPTS = 3;

    private final Scheduler scheduler;
    private final SynchronousHTTPClient syncHTTPClient;

    private final Map<URL, Integer> retryCountMap;

    protected HTTPClient(SSLSocketFactory socketFactory, HTTPResponseParser httpResponseParser) {
        this(new SynchronousHTTPClient(socketFactory, httpResponseParser), new ThreadScheduler());
    }

    @VisibleForTesting
    HTTPClient(SynchronousHTTPClient syncHTTPClient, Scheduler scheduler) {
        this.syncHTTPClient = syncHTTPClient;
        this.scheduler = scheduler;
        this.retryCountMap = new HashMap<>();
    }

    String sendRequest(HTTPRequest request) throws Exception {
        return syncHTTPClient.request(request);
    }

    void sendRequest(HTTPRequest request, HTTPResponseCallback callback) {
        sendRequest(request, HTTPClient.NO_RETRY, callback);
    }

    void sendRequest(HTTPRequest request, @RetryStrategy int retryStrategy, HTTPResponseCallback callback) {
        scheduleRequest(request, retryStrategy, callback);
    }

    private void scheduleRequest(final HTTPRequest request, @RetryStrategy final int retryStrategy, final HTTPResponseCallback callback) {
        resetRetryCount(request);

        scheduler.runOnBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    String responseBody = syncHTTPClient.request(request);
                    notifySuccessOnMainThread(callback, responseBody);
                } catch (Exception e) {
                    switch (retryStrategy) {
                        case HTTPClient.NO_RETRY:
                            notifyErrorOnMainThread(callback, e);
                            break;
                        case HTTPClient.RETRY_MAX_3_TIMES:
                            retryGet(request, retryStrategy, callback);
                            break;
                    }
                }
            }
        });
    }

    private void retryGet(final HTTPRequest request, @RetryStrategy final int retryStrategy, final HTTPResponseCallback callback) {
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
                HTTPClientException retryLimitException = new HTTPClientException(message);
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

    private void resetRetryCount(HTTPRequest request) {
        URL url = null;
        try {
            url = request.getURL();
        } catch (MalformedURLException | URISyntaxException ignore) {}

        if (url != null) {
            retryCountMap.remove(url);
        }
    }

    private void notifySuccessOnMainThread(final HTTPResponseCallback callback, final String responseBody) {
        if (callback != null) {
            scheduler.runOnMain(new Runnable() {
                @Override
                public void run() {
                    callback.onResult(responseBody, null);
                }
            });
        }
    }

    private void notifyErrorOnMainThread(final HTTPResponseCallback callback, final Exception e) {
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
