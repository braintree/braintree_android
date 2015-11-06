package com.paypal.android.networking.processing;

import com.paypal.android.networking.request.ServerRequest;
import com.paypal.android.networking.request.ServerRequestEnvironment;

import android.text.TextUtils;
import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public abstract class MockRequestProcessor extends AbstractRequestProcessor {
    private static final String TAG = MockRequestProcessor.class.getSimpleName();

    private final ServerRequestEnvironment mServerRequestEnvironment;
    private final ThreadPoolExecutor mThreadPoolExecutor;
    private final int mNetworkDelayInMs;

    public MockRequestProcessor(int networkDelayInMs, ServerRequestEnvironment serverRequestEnvironment) {
        this.mNetworkDelayInMs = networkDelayInMs;
        this.mServerRequestEnvironment = serverRequestEnvironment;
        this.mThreadPoolExecutor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    }

    @Override
    public boolean execute(final ServerRequest serverRequest) {
        mThreadPoolExecutor.submit(getTask(serverRequest));
        return true;
    }

    @Override
    public void cancelRequests() {
        // Nothing really to do here
    }

    private Runnable getTask(final ServerRequest serverRequest) {
        return new Runnable() {
            @Override
            public void run() {
                if(isFailure(serverRequest)) {
                    String failure = getFailureReply();

                    serverRequest.setServerReply(failure);
                    parseError(serverRequest, getFailureHttpStatusCode());
                } else {
                    String mockResponse =  serverRequest.getMockResponse();
                    if (TextUtils.isEmpty(mockResponse)) {
                        throw new RuntimeException("Empty mock value for " + serverRequest.getApiInfo());
                    }

                    serverRequest.setServerReply(mockResponse);
                    parse(serverRequest);
                }

                // sleep and complete real requests only
                if (!serverRequest.isTrackingRequest()) {
                    try {
                        Thread.sleep(mNetworkDelayInMs);
                    } catch (InterruptedException e) {
                        // ignore
                    }

                    mServerRequestEnvironment.completeServerRequest(serverRequest);
                }
            }
        };
    }


    /**
     * Returns true if this request should be a mock failure
     * @param serverRequest
     * @return
     */
    protected abstract boolean isFailure(ServerRequest serverRequest);

    /**
     * The failure string if #isFailure returns true
     * @return
     */
    protected abstract String getFailureReply();

    /**
     * If there's a failure, this method will be called to retrieve the http status code
     * @return
     */
    protected abstract int getFailureHttpStatusCode();

}
