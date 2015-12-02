package com.paypal.android.networking.analytics;

import com.paypal.android.networking.request.ApiInfo;
import com.paypal.android.networking.request.RestApiRequest;
import com.paypal.android.networking.request.ServerRequestEnvironment;
import com.paypal.android.sdk.onetouch.core.base.CoreEnvironment;
import com.paypal.android.sdk.onetouch.core.base.URLEncoderHelper;

import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TimeZone;

import javax.xml.parsers.ParserConfigurationException;

public class AbstractFptiRequest extends RestApiRequest {
    private static final String TAG = AbstractFptiRequest.class.getSimpleName();

    private final TrackingLogData mTrackingLogData;

    public AbstractFptiRequest(ApiInfo apiInfo, ServerRequestEnvironment env,
            CoreEnvironment coreEnv, TrackingLogData logData) {
        super(apiInfo, env, coreEnv, null /* no auth needed for FPTI */);
        this.mTrackingLogData = logData;

        putHeader(HEADER_ACCEPT, "application/json; charset=utf-8");
        putHeader(HEADER_ACCEPT_LANGUAGE, "en_US");
        putHeader(HEADER_CONTENT_TYPE, "application/json");
    }

    @Override
    public boolean isTrackingRequest() {
        return true;
    }

    /*-
        sample request
        {
            "events":{
                "actor":{
                    "tracking_visitor_id":"912bddaa1390abe0eed4d1b541ff46e198",
                    "tracking_visit_id":"982bddcd1390abe0d4d1b541ff46e12198"
                },
                "channel":"mobile",
                "tracking_event":"1363303116",
                "http_params":{
                    "accept_charset":"UTF-8",
                    "accept_lang":"en",
                    "host_header":"L-MAA-00412669",
                    "user_agent":"Mozilla/5.0 (Windows NT 6.1; WOW64)",
                    "referrer":"http://en.wikipedia.org/wiki/Main_Page",
                    "ip_address":"202.76.250.50"
                },
                "event_params":{
                    "sv":"mobile",
                    "ch":"ConsAppAndroid",
                    "expn":"channel",
                    "t":"1161775163140",
                    "page":"main"
                }
            }
        }
     */
    @Override
    public String computeRequest() throws JSONException {
        String deviceId =
                URLEncoderHelper.encode(getServerRequestEnvironment().getContextInspector()
                        .getInstallationGUID());
        String visitorTrackingToken = mTrackingLogData.mTrackingToken;

        JSONObject actor = new JSONObject();
        actor.accumulate("tracking_visitor_id", deviceId);
        actor.accumulate("tracking_visit_id", visitorTrackingToken);

        JSONObject events = new JSONObject();
        events.accumulate("actor", actor);
        events.accumulate("channel", "mobile");

        long currentTimeInMillis = System.currentTimeMillis();
        events.accumulate("tracking_event", Long.toString(currentTimeInMillis));

        Calendar mCalendar = new GregorianCalendar();
        TimeZone mTimeZone = mCalendar.getTimeZone();
        int mGMTOffset = mTimeZone.getRawOffset();
        long t = currentTimeInMillis - mGMTOffset;

        // t is client time in millis since epoch
        mTrackingLogData.mParams.put("t", Long.toString(t));
        mTrackingLogData.mParams.put("dsid", deviceId);
        mTrackingLogData.mParams.put("vid", visitorTrackingToken);

        events.accumulate("event_params", getEventParams(mTrackingLogData.mParams));

        JSONObject root = new JSONObject();
        root.accumulate("events", events);

        return root.toString();
    }

    private JSONObject getEventParams(Map<String, String> params) throws JSONException {
        JSONObject ret = new JSONObject();
        for (String key : params.keySet()) {
            ret.accumulate(key, params.get(key));
        }
        return ret;
    }

    @Override
    public String getServerUrl(ApiInfo api) {
        return "https://api.paypal.com/v1/tracking/events";
    }

    @Override
    public void parse() throws ParserConfigurationException, SAXException, JSONException {
        // nothing to parse
    }

    @Override
    public void parseError() throws JSONException {
        // don't care
    }

    @Override
    public String getMockResponse() {
        return "mockResponse";
    }
}
