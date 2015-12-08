package com.paypal.android.sdk.onetouch.core.network;

import com.paypal.android.networking.events.LibraryError;
import com.paypal.android.networking.request.ServerRequest;
import com.paypal.android.networking.request.ServerRequestEnvironment;
import com.paypal.android.sdk.onetouch.core.BuildConfig;
import com.paypal.android.sdk.onetouch.core.base.CoreEnvironment;
import com.paypal.android.sdk.onetouch.core.network.OtcApiName.ApiName;

import org.json.JSONException;

/**
 * Request that gets the config and parses it for the android recipes.
 */
public class ConfigFileRequest extends ServerRequest {
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
        minifiedJson = getParsedJsonRootObject().toString();
    }

    @Override
    public void parseError() throws JSONException {
        setServerError(LibraryError.PARSE_RESPONSE_ERROR.toString(), "failed to parse config",
                null);
    }

    @Override
    public String getMockResponse() {
        return BuildConfig.CONFIGURATION;
    }
}
