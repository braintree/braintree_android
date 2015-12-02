package com.paypal.android.networking.processing;

import android.util.Log;

import com.paypal.android.networking.events.LibraryError;
import com.paypal.android.networking.events.ThrowableEvent;
import com.paypal.android.networking.request.ServerRequest;
import com.paypal.android.sdk.onetouch.core.base.Constants;

import org.json.JSONException;

public abstract class AbstractRequestProcessor implements RequestProcessor {
    private static final String TAG = AbstractRequestProcessor.class.getSimpleName();

    @Override
    public abstract boolean execute(ServerRequest serverRequest);

    protected void parse(final ServerRequest serverRequest) {
        try {
            serverRequest.parse();
        } catch (Exception e) {
            Log.e(Constants.PUBLIC_TAG, "Exception parsing server response", e);
            serverRequest.setError(new ThrowableEvent(LibraryError.PARSE_RESPONSE_ERROR, e));
        }
    }

    protected void parseError(ServerRequest serverRequest, int statusCode) {
        serverRequest.setHttpStatusCode(statusCode);

        try {
            Log.d(TAG, "parsing error response:\n" + serverRequest.getServerReply());
            serverRequest.parseError();
        } catch (JSONException e) {
            Log.e(Constants.PUBLIC_TAG, "Exception parsing server response", e);
            serverRequest.setServerError(
                    LibraryError.INTERNAL_SERVER_ERROR.toString(), statusCode
                            + " http response received.  Response not parsable: " + e.getMessage(),
                    null);
        }
    }
}
