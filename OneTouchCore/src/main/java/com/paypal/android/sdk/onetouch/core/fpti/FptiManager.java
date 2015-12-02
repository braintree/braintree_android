package com.paypal.android.sdk.onetouch.core.fpti;

import com.paypal.android.networking.ServerInterface;
import com.paypal.android.networking.analytics.TrackingLogData;
import com.paypal.android.sdk.onetouch.core.BuildConfig;
import com.paypal.android.sdk.onetouch.core.Protocol;
import com.paypal.android.sdk.onetouch.core.base.ContextInspector;
import com.paypal.android.sdk.onetouch.core.base.CoreEnvironment;
import com.paypal.android.sdk.onetouch.core.base.DeviceInspector;
import com.paypal.android.sdk.onetouch.core.network.FptiRequest;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

public class FptiManager {
    private static final String TAG = FptiManager.class.getSimpleName();

    private final CoreEnvironment mCoreEnvironment;
    private final ServerInterface mServerInterface;
    private final ContextInspector mContextInspector;
    private FptiToken token;

    public FptiManager(ServerInterface serverInterface,
            CoreEnvironment coreEnvironment,
            ContextInspector contextInspector) {
        this.mServerInterface = serverInterface;
        this.mCoreEnvironment = coreEnvironment;
        this.mContextInspector = contextInspector;
    }

    public void trackFpti(TrackingPoint point, String environmentName,
            Map<String, String> fptiDataBundle, Protocol protocol) {
        if (null == token || !token.isValid()) {
            token = new FptiToken();
        }

        DeviceInspector deviceInspector = new DeviceInspector();

        String abcde = "mobile:otc:" + point.getCd() + ":" +
                (null != protocol ? protocol.name() : "");  // also known as 'pagename'
        String xyz = "Android:" + environmentName + ":";
        String abcdexyz_error =
                abcde + ":" + xyz + (point.hasError() ? "|error" : ""); //also known as 'pageName2'

        // params in alphabetical order
        Map<String, String> params = new HashMap<>(fptiDataBundle);
        params.put("apid",
                mContextInspector.getApplicationInfoName() + "|" + BuildConfig.PRODUCT_VERSION + "|"
                        + mContextInspector.getContext().getPackageName());
        params.put("bchn", "otc");
        params.put("bzsr", "mobile");
        params.put("e",
                "im"); // always say 'impression' for OTC, as opposed to 'cl' click to match iOS
        params.put("g", getGmtOffsetInMinutes());
        params.put("lgin", "out");
        params.put("mapv", BuildConfig.PRODUCT_VERSION);
        params.put("mcar", mContextInspector.getSimOperatorName());
        params.put("mdvs", deviceInspector.getDeviceName());
        params.put("mosv", deviceInspector.getOs());
        params.put("page", abcdexyz_error);
        params.put("pgrp", abcde);
        params.put("rsta", Locale.getDefault().toString());
        params.put("srce", "otc");
        params.put("sv", "mobile");
        params.put("vers", "Android:" + environmentName + ":");

        TrackingLogData tld = new TrackingLogData(token.mToken, params);
        mServerInterface.submit(new FptiRequest(mServerInterface, mCoreEnvironment, tld));
    }

    private String getGmtOffsetInMinutes() {
        Calendar mCalendar = new GregorianCalendar();
        TimeZone mTimeZone = mCalendar.getTimeZone();
        int mGMTOffset = mTimeZone.getRawOffset();
        return Integer.toString(mGMTOffset / 1000 / 60);
    }

    private class FptiToken {
        public static final int FPTI_TOKEN_VALIDITY_IN_HOURS = 30;
        public String mToken;

        /**
         * Java Date as a long
         */
        private long mValidUntil;

        /**
         * Returns true if the token is valid (not expired)
         *
         * @return
         */
        public boolean isValid() {
            return mValidUntil > System.currentTimeMillis();
        }

        /**
         * Creates a token, good for 30 hours
         */
        public FptiToken() {
            setTrackingToken();
        }

        private void setTrackingToken() {
            final long now = System.currentTimeMillis();
            if (mToken == null) {
                mValidUntil = now; // force the below if to be true
            }

            if (((mValidUntil + (FPTI_TOKEN_VALIDITY_IN_HOURS * 60 * 1000)) > now)) {
                mValidUntil = now + (FPTI_TOKEN_VALIDITY_IN_HOURS * 60 * 1000);

                mToken = getNewTrackingToken();
            }
        }

        private String getNewTrackingToken() {
            final Random r = new Random(mValidUntil);
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; ++i) {
                sb.append((char) ('0' + (Math.abs(r.nextInt()) % 10)));
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            return FptiToken.class.getSimpleName() + "[mToken=" + mToken + ", mValidUntil=" +
                    mValidUntil + "]";
        }
    }
}
