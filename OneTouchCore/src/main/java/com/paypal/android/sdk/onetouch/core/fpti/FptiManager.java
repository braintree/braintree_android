package com.paypal.android.sdk.onetouch.core.fpti;

import android.os.Handler;
import android.support.annotation.VisibleForTesting;

import com.paypal.android.sdk.onetouch.core.BuildConfig;
import com.paypal.android.sdk.onetouch.core.base.ContextInspector;
import com.paypal.android.sdk.onetouch.core.base.DeviceInspector;
import com.paypal.android.sdk.onetouch.core.base.URLEncoderHelper;
import com.paypal.android.sdk.onetouch.core.enums.Protocol;
import com.paypal.android.sdk.onetouch.core.network.EnvironmentManager;
import com.paypal.android.sdk.onetouch.core.network.PayPalHttpClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class FptiManager {

    private final ContextInspector mContextInspector;
    private final PayPalHttpClient mHttpClient;
    private FptiToken mToken;

    public FptiManager(ContextInspector contextInspector, PayPalHttpClient httpClient) {
        mContextInspector = contextInspector;
        mHttpClient = httpClient;
    }

    public void trackFpti(TrackingPoint point, String environmentName,
            Map<String, String> fptiDataBundle, Protocol protocol) {
        if (EnvironmentManager.isMock(environmentName)) {
            return;
        }

        if (mToken == null || !mToken.isValid()) {
            mToken = new FptiToken();
        }

        long currentTimeInMillis = System.currentTimeMillis();
        String deviceId = URLEncoderHelper.encode(mContextInspector.getInstallationGUID());
        String abcde = "mobile:otc:" + point.getCd() + ":" +
                (null != protocol ? protocol.name() : "");  // also known as 'pagename'
        String xyz = "Android:" + environmentName + ":";
        String abcdexyz_error = abcde + ":" + xyz + (point.hasError() ? "|error" : ""); //also known as 'pageName2'

        // params in alphabetical order
        Map<String, String> params = new HashMap<>(fptiDataBundle);
        params.put("apid", DeviceInspector.getApplicationInfoName(mContextInspector.getContext()) +
                "|" + BuildConfig.PRODUCT_VERSION + "|" +
                mContextInspector.getContext().getPackageName());
        params.put("bchn", "otc");
        params.put("bzsr", "mobile");
        params.put("dsid", deviceId);
        params.put("e", "im"); // always say 'impression' for OTC, as opposed to 'cl' click to match iOS
        params.put("g", getGmtOffsetInMinutes());
        params.put("lgin", "out");
        params.put("mapv", BuildConfig.PRODUCT_VERSION);
        params.put("mcar", DeviceInspector.getSimOperatorName(mContextInspector.getContext()));
        params.put("mdvs", DeviceInspector.getDeviceName());
        params.put("mosv", DeviceInspector.getOs());
        params.put("page", abcdexyz_error);
        params.put("pgrp", abcde);
        params.put("rsta", Locale.getDefault().toString());
        params.put("srce", "otc");
        params.put("sv", "mobile");
        params.put("t", Long.toString(currentTimeInMillis - getGMTOffset())); // client time in millis since epoch
        params.put("vers", "Android:" + environmentName + ":");
        params.put("vid", mToken.mToken);

        try {
            JSONObject actor = new JSONObject();
            actor.accumulate("tracking_visitor_id", deviceId);
            actor.accumulate("tracking_visit_id", mToken.mToken);

            JSONObject events = new JSONObject();
            events.accumulate("actor", actor);
            events.accumulate("channel", "mobile");
            events.accumulate("tracking_event", Long.toString(currentTimeInMillis));
            events.accumulate("event_params", getEventParams(params));

            sendRequest(new JSONObject().accumulate("events", events).toString());
        } catch (JSONException ignored) {}
    }

    private JSONObject getEventParams(Map<String, String> params) throws JSONException {
        JSONObject ret = new JSONObject();
        for (String key : params.keySet()) {
            ret.accumulate(key, params.get(key));
        }
        return ret;
    }

    @VisibleForTesting
    void sendRequest(final String data) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mHttpClient.post("tracking/events", data, null);
            }
        }, (10 + new Random().nextInt(190)) * 1000);
    }

    private int getGMTOffset() {
       return new GregorianCalendar().getTimeZone().getRawOffset();
    }

    private String getGmtOffsetInMinutes() {
        return Integer.toString(getGMTOffset() / 1000 / 60);
    }
}
