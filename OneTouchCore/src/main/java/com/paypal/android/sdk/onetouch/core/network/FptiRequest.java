package com.paypal.android.sdk.onetouch.core.network;

import com.paypal.android.sdk.onetouch.core.base.CoreEnvironment;
import com.paypal.android.networking.analytics.AbstractFptiRequest;
import com.paypal.android.networking.analytics.TrackingLogData;
import com.paypal.android.networking.request.ServerRequestEnvironment;
import com.paypal.android.sdk.onetouch.core.network.OtcApiName.ApiName;

public class FptiRequest extends AbstractFptiRequest {
    public FptiRequest(ServerRequestEnvironment env,
                       CoreEnvironment coreEnv, TrackingLogData logData) {
        super(new OtcApiName(ApiName.FptiRequest), env, coreEnv, logData);
    }
}
