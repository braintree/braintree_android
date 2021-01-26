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

public class HttpClient {

    @IntDef({ NO_RETRY, RETRY_MAX_3_TIMES })
    @Retention(RetentionPolicy.SOURCE)
    public @interface RetryStrategy {
    }

    public static final int NO_RETRY = 0;
    public static final int RETRY_MAX_3_TIMES = 1;

    public static final int MAX_RETRY_ATTEMPTS = 3;

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

    public String sendRequest(HttpRequest request) throws Exception {
        return syncHttpClient.request(request);
    }

    public void sendRequest(HttpRequest request, HttpResponseCallback callback) {
        sendRequest(request, HttpClient.NO_RETRY, callback);
    }

    public void sendRequest(HttpRequest request, @RetryStrategy int retryStrategy, HttpResponseCallback callback) {
        scheduleRequest(request, retryStrategy, callback);
    }

    private void scheduleRequest(final HttpRequest request, @RetryStrategy final int retryStrategy, final HttpResponseCallback callback) {
        resetRetryCount(request);

        scheduler.runOnBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    String responseBody = syncHttpClient.request(request);
                    notifySuccessOnMainThread(callback, responseBody);
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

    private void retryGet(final HttpRequest request, @RetryStrategy final int retryStrategy, final HttpResponseCallback callback) {
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

    private void notifySuccessOnMainThread(final HttpResponseCallback callback, final String responseBody) {
        if (callback != null) {
            scheduler.runOnMain(new Runnable() {
                @Override
                public void run() {
                    callback.success(responseBody);
                }
            });
        }
    }

    private void notifyErrorOnMainThread(final HttpResponseCallback callback, final Exception e) {
        if (callback != null) {
            scheduler.runOnMain(new Runnable() {
                @Override
                public void run() {
                    callback.failure(e);
                }
            });
        }
    }
}
