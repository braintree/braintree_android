package com.paypal.android.sdk.onetouch.core.network;

import com.paypal.android.sdk.onetouch.core.base.CoreEnvironment;
import com.paypal.android.networking.events.LibraryError;
import com.paypal.android.networking.request.ServerRequest;
import com.paypal.android.networking.request.ServerRequestEnvironment;
import com.paypal.android.sdk.onetouch.core.BuildConfig;
import com.paypal.android.sdk.onetouch.core.config.ConfigFileParser;
import com.paypal.android.sdk.onetouch.core.config.OtcConfiguration;
import com.paypal.android.sdk.onetouch.core.network.OtcApiName.ApiName;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Request that gets the config and parses it for the android recipes.
 */
public class ConfigFileRequest extends ServerRequest {
    private static final String TAG = ConfigFileRequest.class.getSimpleName();

    public String minifiedJson;

    public ConfigFileRequest(ServerRequestEnvironment env, CoreEnvironment coreEnv) {
        super(new OtcApiName(ApiName.ConfigFileRequest), env, coreEnv, null);

        // TODO put header to enable gzip?  "text/html", "application/x-javascript", "text/css", or another one for json?
        //putHeader();
    }

    @Override
    public String computeRequest() {
        // no computed string required for static file
        return null;
    }

    @Override
    public void parse() throws JSONException {
        JSONObject rootObject = getParsedJsonRootObject();

        try {
            OtcConfiguration otcConfiguration = new ConfigFileParser().getParsedConfig(rootObject);
            minifiedJson = rootObject.toString();
            Log.d(TAG, "parsed config:" + otcConfiguration);
        } catch (JSONException e) {
            Log.d(TAG, "config file parsing failed", e);
            parseError();
        }
    }

    @Override
    public void parseError() throws JSONException {
        Log.d(TAG, "parseError()");

        setServerError(LibraryError.PARSE_RESPONSE_ERROR.toString(), "failed to parse config", null);
    }

    @Override
    public String getMockResponse() {
        return BuildConfig.CONFIG_FILE;
    }
}
