package com.paypal.android.networking.processing;

import com.paypal.android.sdk.onetouch.core.base.Constants;
import com.paypal.android.sdk.onetouch.core.base.ContextInspector;
import com.paypal.android.sdk.onetouch.core.base.CoreEnvironment;
import com.paypal.android.networking.EnvironmentManager;
import com.paypal.android.networking.events.LibraryError;
import com.paypal.android.networking.events.RequestError;
import com.paypal.android.networking.events.ThrowableEvent;
import com.paypal.android.networking.http.OkHttpClientFactory;
import com.paypal.android.networking.http.Tlsv1_0UnavailableException;
import com.paypal.android.networking.request.ServerRequest;
import com.paypal.android.networking.request.ServerRequestEnvironment;

import android.text.TextUtils;
import android.util.Log;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Processor handles all non-mock interactions on serverRequests.
 */
public class NetworkRequestProcessor extends AbstractRequestProcessor {
    private static final String TAG = NetworkRequestProcessor.class.getSimpleName();

    private static final MediaType MEDIA_TYPE_UTF = MediaType.parse("charset=utf-8");

    private final ContextInspector mContextInspector;
    private final String mEnvironmentName;
    private final ServerRequestEnvironment mServerRequestEnvironment;
    private final OkHttpClient mHttpClient;
    private final OkHttpClient mTrackingHttpClient;
    private final CoreEnvironment mCoreEnvironment;
    private final ScheduledExecutorService mScheduledExecutorService;
    private final ConcurrentLinkedQueue<Runnable> mQueue;

    public NetworkRequestProcessor(ContextInspector contextInspector,
                                   String environmentName,
                                   CoreEnvironment coreEnvironment,
                                   ServerRequestEnvironment serverRequestEnvironment,
                                   int networkTimeout,
                                   boolean enableStageSsl,
                                   List<? extends Interceptor> additionalInterceptors) {
        this.mContextInspector = contextInspector;
        this.mEnvironmentName = environmentName;
        this.mCoreEnvironment = coreEnvironment;
        this.mServerRequestEnvironment = serverRequestEnvironment;

        boolean isStageEnv = EnvironmentManager.isStage(environmentName);
        // only trustall if a stage, and stageSSL turned off
        boolean isTrustAll = isStageEnv && !enableStageSsl;
        // use ssl pinning for only live or sandbox. Partner stages, internal stages, and all other
        // machines will not have ssl pinning enabled with this flag. SSL will still be done, just
        // no pinning.
        boolean useSslPinning = !isStageEnv;

        mHttpClient = OkHttpClientFactory.getOkHttpClient(networkTimeout, isTrustAll, useSslPinning,
                coreEnvironment.getUserAgent(), this.mServerRequestEnvironment.environmentBaseUrl());

        mHttpClient.interceptors().addAll(additionalInterceptors);
        mHttpClient.interceptors().add(new HeaderLoggingInterceptor());

        mTrackingHttpClient = OkHttpClientFactory.getOkHttpClient(networkTimeout, false, false,
                coreEnvironment.getUserAgent(), this.mServerRequestEnvironment.environmentBaseUrl());
        mTrackingHttpClient.interceptors().add(new HeaderLoggingInterceptor());
        
        mScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        mQueue = new ConcurrentLinkedQueue<>();
    }

    /**
     * Execute the given request.
     *
     * @param serverRequest serverRequest Object
     * @return true if this method actually kicked off a request
     */
    @Override
    public boolean execute(final ServerRequest serverRequest) {

        if (!mContextInspector.isNetworkAvailable()) {
            serverRequest.setError(new RequestError(LibraryError.SERVER_COMMUNICATION_ERROR
                    .toString()));
            return false;
        }

        serverRequest.preExecute();

        final String endpoint = serverRequest.getServerUrl(serverRequest.getApiInfo());
        try {
            if (serverRequest.isTrackingRequest()) {
                Log.d(TAG, serverRequest.toLogString() + " endpoint: " + endpoint);
                Log.d(
                        TAG,
                        serverRequest.toLogString() + " request: " + serverRequest.getComputedRequest());
                scheduleDelayedTrackingRequest(serverRequest, endpoint);
            } else {
                // lower log levels in release
                Log.i(TAG, serverRequest.toLogString() + " endpoint: " + endpoint);
                Log.i(
                        TAG,
                        serverRequest.toLogString() + " request: " + serverRequest.getComputedRequest());
                executeHttpMethod(serverRequest, endpoint, mHttpClient, new GenericHttpResponseHandler(serverRequest));
            }
            // successfully started thread
            return true;
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "encoding failure", e);
            serverRequest.setError(new ThrowableEvent(LibraryError.INTERNAL_ERROR, e));
            return false;
        } catch (IOException e) {
            Log.e(TAG, "communication failure", e);
            serverRequest.setError(new ThrowableEvent(LibraryError.SERVER_COMMUNICATION_ERROR, e));
            return false;
        }
    }

    /**
     * Schedules Delayed Tracking Request for FPTI requests.
     *
     * @param serverRequest serverRequest Object
     * @param endpoint      URL
     */
    private void scheduleDelayedTrackingRequest(final ServerRequest serverRequest, final String endpoint) {
        mQueue.offer(new Runnable() {
            @Override
            public void run() {
                try {
                    executeHttpMethod(serverRequest, endpoint, mTrackingHttpClient, new TrackingRequestResponseHandler(
                            serverRequest));
                } catch (IOException e) {
                    // ignore, it's just FPTI
                }
            }
        });

        int delay = 10 + new Random().nextInt(190);
        Log.d(TAG, "Delaying tracking execution for " + delay + " seconds");
        mScheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                mQueue.poll().run();
            }
        }, delay, TimeUnit.SECONDS); // delay 10-200 seconds
    }

    /**
     * Executes HTTP Calls.
     *
     * @param serverRequest serverRequest Object
     * @param endpoint      URL
     * @param clientToUse   which client to use for making this call.
     * @param handlerToUse  Callback Handler
     * @throws IOException
     */
    private void executeHttpMethod(ServerRequest serverRequest, String endpoint, OkHttpClient clientToUse, Callback handlerToUse) throws IOException {
        Request request;
        switch (serverRequest.getApiInfo().getMethod()) {
            case GET:
                request = new Request.Builder()
                        .tag(TAG)
                        .url(concat(endpoint, serverRequest.getComputedRequest()))
                        .headers(getHttpHeaders(serverRequest))
                        .build();

                clientToUse.newCall(request).enqueue(handlerToUse);
                break;

            case POST:
                String postBody = serverRequest.getComputedRequest();
                RequestBody body = RequestBody.create(MEDIA_TYPE_UTF, postBody);
                request = new Request.Builder()
                        .tag(TAG)
                        .url(endpoint)
                        .post(body)
                        .headers(getHttpHeaders(serverRequest))
                        .build();

                clientToUse.newCall(request).enqueue(handlerToUse);
                break;

            case DELETE:
                request = new Request.Builder()
                        .tag(TAG)
                        .url(concat(endpoint, serverRequest.getComputedRequest()))
                        .headers(getHttpHeaders(serverRequest))
                        .delete()
                        .build();

                clientToUse.newCall(request).enqueue(handlerToUse);
                break;

            default:
                throw new RuntimeException(serverRequest.getApiInfo().getMethod()
                        + " not supported.");
        }
    }

    /**
     * Concat endpoints and request String
     *
     * @param endpoint        URL
     * @param computedRequest Request string
     * @return Concatenated string
     */
    private String concat(String endpoint, String computedRequest) {
        if (null != computedRequest) {
            if (!endpoint.endsWith("/")) {
                endpoint += "/";
            }
            return endpoint + computedRequest;
        } else {
            // don't do anything if there's no computed request
            return endpoint;
        }
    }

    /**
     * Retrieves all HTTPHeaders from ServerRequest Object
     *
     * @param serverRequest serverRequest Object
     * @return Headers
     */
    private Headers getHttpHeaders(final ServerRequest serverRequest) {
        Headers.Builder builder = new Headers.Builder();
        for (Entry<String, String> headerEntry : serverRequest.getRequestHeaders().entrySet()) {
            builder.add(headerEntry.getKey(), headerEntry.getValue());
        }

        return builder.build();
    }

    /**
     * Handler method to properly handle failures. Both IOException or HTTPExceptions are handled.
     *
     * @param serverRequest serverRequest Object
     * @param response      Response received from HTTP Request
     * @param e             Exception Object
     */
    private void handleFailure(final ServerRequest serverRequest, Response response, IOException e) {
        Log.d(TAG, serverRequest.toLogString() + " failure.");

        if (response != null) {

            Log.e(Constants.PUBLIC_TAG, "request failure with http statusCode:" + response.code()
                    + ",exception:" + response.message());

            parseError(serverRequest, response.code());

            // if parsing the response did not generate a sensible error, then use generic error.
            if (serverRequest.isSuccess()) {
                serverRequest.setServerError(
                        LibraryError.INTERNAL_SERVER_ERROR.toString(), response.code()
                                + " http response received.  Response not parsable.", null);
            }

        } else if (e != null) {
            if (e instanceof Tlsv1_0UnavailableException) {
                serverRequest.setError(new ThrowableEvent(LibraryError.DEVICE_OS_TOO_OLD, e));
            } else {
                serverRequest.setError(new ThrowableEvent(LibraryError.SERVER_COMMUNICATION_ERROR, e));
            }
        } else {
            throw new RuntimeException("Both Response or Exception cannot be null");
        }
        Log.e(
                Constants.PUBLIC_TAG,
                "request failed with server response:" + serverRequest.getServerReply());

        mServerRequestEnvironment.completeServerRequest(serverRequest);
    }

    /**
     * Cancels all the requests during shutdown.
     */
    public void cancelRequests() {
        safelyCancelRequest(mHttpClient);
        safelyCancelRequest(mTrackingHttpClient);
    }

    /**
     * Workaround for crash documented here: https://github.com/square/okhttp/issues/1592
     *
     */
    private void safelyCancelRequest(final OkHttpClient client) {
        client.getDispatcher().getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                client.cancel(TAG);
            }
        });
    }

    /**
     * Handler Class for Generic HTTP Responses
     */
    private final class GenericHttpResponseHandler implements Callback {
        private final ServerRequest serverRequest;

        private GenericHttpResponseHandler(ServerRequest serverRequest) {
            this.serverRequest = serverRequest;
        }

        @Override
        public void onResponse(Response response) throws IOException {
            try {
                String paypalDebugId = response.header("paypal-debug-id");
                serverRequest.setServerReply(response.body().string());
                if (!response.isSuccessful()) {
                    if (!TextUtils.isEmpty(paypalDebugId)) {
                        Log.w(Constants.PUBLIC_TAG, getDebugString(paypalDebugId));
                    }
                    handleFailure(serverRequest, response, null);
                    return;
                }

                serverRequest.setPayPalDebugId(paypalDebugId);

                // lower log levels in release
                Log.i(
                        TAG,
                        serverRequest.toLogString() + " success. response: "
                                + serverRequest.getServerReply());

                // log the debug ID header for the developer
                if (!TextUtils.isEmpty(paypalDebugId)) {
                    Log.w(Constants.PUBLIC_TAG, getDebugString(paypalDebugId));
                }

                if (serverRequest.isSuccess()) {
                    parse(serverRequest);
                }

                mServerRequestEnvironment.completeServerRequest(serverRequest);
            } catch (Throwable throwable) {
                Log.e(Constants.PUBLIC_TAG, "exception in response handler", throwable);
                throw throwable;
            }
        }

        @Override
        public void onFailure(Request request, IOException e) {
            try {
                serverRequest.setServerReply(e.getMessage());
                String paypalDebugId = request.header("PayPal-Debug-Id");
                // log the debug ID header for the developer
                if (!TextUtils.isEmpty(paypalDebugId)) {
                    Log.w(Constants.PUBLIC_TAG, getDebugString(paypalDebugId));
                }
                handleFailure(serverRequest, null, e);
            } catch (Throwable throwable) {
                Log.e(Constants.PUBLIC_TAG, "exception in response handler", throwable);
                throw throwable;
            }
        }

        private String getDebugString(String ppDebugIdHeader) {
            return String.format(
                    Locale.US, serverRequest.toLogString() + " PayPal Debug-ID: %s [%s, %s]",
                    ppDebugIdHeader, mEnvironmentName, mCoreEnvironment.getVersion() + ";"
                            + ((mCoreEnvironment.isDebug()) ? "debug" : "release"));
        }

    }

    /**
     * Response Handler Class for Tracking Requests
     */
    private final class TrackingRequestResponseHandler implements Callback {
        private final ServerRequest trackingRequest;

        private TrackingRequestResponseHandler(ServerRequest trackingRequest) {
            this.trackingRequest = trackingRequest;
        }

        @Override
        public void onResponse(Response response) throws IOException {
            trackingRequest.setServerReply(response.body().string());
            Log.d(TAG, trackingRequest.toLogString() + " success");
            // don't dispatch tracking requests
        }

        @Override
        public void onFailure(Request request, IOException e) {
            trackingRequest.setServerReply(e.getMessage());
            Log.d(TAG, trackingRequest.toLogString() + " failure: " + e.getMessage());
        }
    }

}
