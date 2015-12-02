package com.paypal.android.sdk.onetouch.core.network;

import com.paypal.android.networking.processing.MockRequestProcessor;
import com.paypal.android.networking.request.ServerRequest;
import com.paypal.android.networking.request.ServerRequestEnvironment;

public class OtcMockRequestProcessor extends MockRequestProcessor {
    public OtcMockRequestProcessor(int networkDelayInMs,
            ServerRequestEnvironment serverRequestEnvironment) {
        super(networkDelayInMs, serverRequestEnvironment);
    }

    @Override
    protected boolean isFailure(ServerRequest serverRequest) {
        return false;
    }

    @Override
    protected String getFailureReply() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected int getFailureHttpStatusCode() {
        throw new UnsupportedOperationException();
    }
}
