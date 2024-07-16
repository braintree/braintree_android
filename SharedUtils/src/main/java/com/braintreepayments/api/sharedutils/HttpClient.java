package com.braintreepayments.api.sharedutils;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class HttpClient {

    @IntDef({NO_RETRY, RETRY_MAX_3_TIMES})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @interface RetryStrategy {
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final int NO_RETRY = 0;
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final int RETRY_MAX_3_TIMES = 1;

    static final int MAX_RETRY_ATTEMPTS = 3;

    private final Scheduler scheduler;
    private final SynchronousHttpClient syncHttpClient;

    private final Map<URL, Integer> retryCountMap;

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public HttpClient(SSLSocketFactory socketFactory, HttpResponseParser httpResponseParser) {
        this(new SynchronousHttpClient(socketFactory, httpResponseParser), new ThreadScheduler());
    }

    @VisibleForTesting
    HttpClient(SynchronousHttpClient syncHttpClient, Scheduler scheduler) {
        this.syncHttpClient = syncHttpClient;
        this.scheduler = scheduler;
        this.retryCountMap = new HashMap<>();
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public String sendRequest(HttpRequest request) throws Exception {
        return syncHttpClient.request(request).getBody();
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void sendRequest(HttpRequest request, NetworkResponseCallback callback) {
        sendRequest(request, HttpClient.NO_RETRY, callback);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void sendRequest(HttpRequest request, @RetryStrategy int retryStrategy,
                            NetworkResponseCallback callback) {
        scheduleRequest(request, retryStrategy, callback);
    }

    private void scheduleRequest(final HttpRequest request, @RetryStrategy final int retryStrategy,
                                 final NetworkResponseCallback callback) {
        resetRetryCount(request);

        scheduler.runOnBackground(() -> {
            try {
                HttpResponse httpResponse = syncHttpClient.request(request);
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
        });
    }

    private void retryGet(final HttpRequest request, @RetryStrategy final int retryStrategy,
                          final NetworkResponseCallback callback) {
        URL url = null;
        try {
            url = request.getURL();
        } catch (MalformedURLException | URISyntaxException ignore) {
        }

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
        } catch (MalformedURLException | URISyntaxException ignore) {
        }

        if (url != null) {
            retryCountMap.remove(url);
        }
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
