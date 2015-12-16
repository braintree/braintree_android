package com.paypal.android.sdk.onetouch.core;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import com.paypal.android.sdk.onetouch.core.base.ContextInspector;
import com.paypal.android.sdk.onetouch.core.config.CheckoutRecipe;
import com.paypal.android.sdk.onetouch.core.config.OtcConfiguration;
import com.paypal.android.sdk.onetouch.core.config.Recipe;
import com.paypal.android.sdk.onetouch.core.enums.Protocol;
import com.paypal.android.sdk.onetouch.core.enums.RequestTarget;
import com.paypal.android.sdk.onetouch.core.enums.ResponseType;
import com.paypal.android.sdk.onetouch.core.exception.BrowserSwitchException;
import com.paypal.android.sdk.onetouch.core.exception.ResponseParsingException;
import com.paypal.android.sdk.onetouch.core.fpti.TrackingPoint;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class CheckoutRequest extends Request<CheckoutRequest> implements Parcelable {

    private static final String TAG = CheckoutRequest.class.getSimpleName();

    private static final String PREFS_HERMES_TOKEN = "com.paypal.otc.hermes.token";
    private static final String TOKEN_QUERY_PARAM_KEY_TOKEN = "token";
    private static final String TOKEN_QUERY_PARAM_KEY_BA_TOKEN = "ba_token";

    protected String mApprovalUrl;
    protected String mTokenQueryParamKey;
    protected String mPairingId;

    public CheckoutRequest() {
        mTokenQueryParamKey = TOKEN_QUERY_PARAM_KEY_TOKEN;
    }

    public String getPairingId() {
        return mPairingId;
    }

    public CheckoutRequest pairingId(String pairingId) {
        mPairingId = pairingId;
        return this;
    }

    public CheckoutRequest approvalURL(String approvalURL) {
        mApprovalUrl = approvalURL;
        selectTokenQueryParamKey(approvalURL);
        return this;
    }

    public boolean isBillingAgreement() {
        return TOKEN_QUERY_PARAM_KEY_BA_TOKEN.equals(mTokenQueryParamKey);
    }

    protected void selectTokenQueryParamKey(String url) {
        if (!TextUtils.isEmpty(url) && url.contains("ba_token")) {
            mTokenQueryParamKey = TOKEN_QUERY_PARAM_KEY_BA_TOKEN;
        } else {
            mTokenQueryParamKey = TOKEN_QUERY_PARAM_KEY_TOKEN;
        }
    }

    @Override
    public String toString() {
        return String.format(
                CheckoutRequest.class.getSimpleName() + ": {" + getBaseRequestToString() +
                        ", approvalURL: %s}",
                mApprovalUrl);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getClientMetadataId());
        dest.writeString(getClientId());
        dest.writeString(getEnvironment());

        dest.writeString(mApprovalUrl);
        dest.writeString(mTokenQueryParamKey);
    }

    private CheckoutRequest(Parcel source) {
        clientMetadataId(source.readString());
        clientId(source.readString());
        environment(source.readString());

        mApprovalUrl = source.readString();
        mTokenQueryParamKey = source.readString();
    }

    /**
     * required by {@link android.os.Parcelable}
     */
    public static final Creator<CheckoutRequest> CREATOR =
            new Creator<CheckoutRequest>() {

                @Override
                public CheckoutRequest[] newArray(int size) {
                    return new CheckoutRequest[size];
                }

                @Override
                public CheckoutRequest createFromParcel(Parcel source) {
                    return new CheckoutRequest(source);
                }
            };

    @Override
    protected CheckoutRequest getThis() {
        return this;
    }

    @Override
    public String getBrowserSwitchUrl(Context context, OtcConfiguration config) {
        return mApprovalUrl;
    }

    @Override
    public Recipe getBrowserSwitchRecipe(OtcConfiguration config) {
        return config.getBrowserCheckoutConfig();
    }

    @Override
    public void persistRequiredFields(ContextInspector contextInspector) {
        contextInspector.setPreference(PREFS_HERMES_TOKEN,
                Uri.parse(mApprovalUrl).getQueryParameter(mTokenQueryParamKey));
    }

    @Override
    public Result parseBrowserResponse(ContextInspector contextInspector, Uri uri) {
        String status = uri.getLastPathSegment();

        if (!Uri.parse(getSuccessUrl()).getLastPathSegment().equals(status)) {
            // return cancel result
            return new Result();
        }

        String persistedXoToken = contextInspector.getStringPreference(PREFS_HERMES_TOKEN);
        String responseXoToken = uri.getQueryParameter(mTokenQueryParamKey);
        if (null != responseXoToken && TextUtils.equals(persistedXoToken, responseXoToken)) {
            try {
                JSONObject response = new JSONObject();
                response.put("webURL", uri.toString());
                return new Result(
                        null /*don't know the environment here*/,
                        ResponseType.web,
                        response,
                        null /* email not sent back in checkout requests since Hermes doesn't return that info*/);
            } catch (JSONException e) {
                return new Result(new ResponseParsingException(e));
            }
        } else {
            return new Result(
                    new BrowserSwitchException("The response contained inconsistent data."));
        }
    }

    @Override
    public boolean validateV1V2Response(ContextInspector contextInspector, Bundle extras) {
        String persistedXoToken = contextInspector.getStringPreference(PREFS_HERMES_TOKEN);
        String webUrl = extras.getString("webURL");
        if (null != webUrl) {
            String responseXoToken = Uri.parse(webUrl).getQueryParameter(mTokenQueryParamKey);
            if (null != responseXoToken && TextUtils.equals(persistedXoToken, responseXoToken)) {
                // they match yay!
                return true;

            } else {
                Log.e(TAG, "EC-tokens don't match");
            }
        } else {
            Log.e(TAG, "no webURL in response");
        }

        return false;
    }

    @Override
    public Recipe getRecipeToExecute(Context context, OtcConfiguration config,
            boolean isSecurityEnabled) {
        for (CheckoutRecipe recipe : config.getCheckoutRecipes()) {
            if (RequestTarget.wallet == recipe.getTarget()) {
                if (recipe.isValidAppTarget(context, isSecurityEnabled)) {
                    return recipe;
                }
            } else if (RequestTarget.browser == recipe.getTarget()) {
                String browserSwitchUrl = getBrowserSwitchUrl(context, config);

                if (recipe.isValidBrowserTarget(context, browserSwitchUrl)) {
                    return recipe;
                }
            }
        }
        return null;
    }

    @Override
    public void trackFpti(Context context, TrackingPoint trackingPoint, Protocol protocol) {
        String ecToken = Uri.parse(mApprovalUrl).getQueryParameter(mTokenQueryParamKey);

        Map<String, String> fptiDataBundle = new HashMap<>();
        fptiDataBundle.put("fltk", ecToken);
        fptiDataBundle.put("clid", getClientId());
        PayPalOneTouchCore.getFptiManager(context)
                .trackFpti(trackingPoint, getEnvironment(), fptiDataBundle, protocol);
    }

}
