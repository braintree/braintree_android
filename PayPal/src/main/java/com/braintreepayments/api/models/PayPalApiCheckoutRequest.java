package com.braintreepayments.api.models;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.braintreepayments.api.PayPalDataCollector;
import com.braintreepayments.api.enums.PayPalApiResponseType;
import com.braintreepayments.api.exceptions.PayPalBrowserSwitchException;
import com.braintreepayments.api.exceptions.PayPalApiResponseParsingException;

import org.json.JSONException;
import org.json.JSONObject;

public class PayPalApiCheckoutRequest extends PayPalApiRequest<PayPalApiCheckoutRequest> implements Parcelable {

    private static final String TOKEN_QUERY_PARAM_KEY_TOKEN = "token";

    protected String mApprovalUrl;
    protected String mTokenQueryParamKey;

    private String mPairingId;

    public PayPalApiCheckoutRequest() {
        mTokenQueryParamKey = TOKEN_QUERY_PARAM_KEY_TOKEN;
    }

    public String getPairingId() {
        return mPairingId;
    }

    public PayPalApiCheckoutRequest pairingId(Context context, String pairingId) {
        mPairingId = pairingId;
        clientMetadataId(PayPalDataCollector.getClientMetadataId(context, pairingId));
        return this;
    }

    public PayPalApiCheckoutRequest approvalURL(String approvalURL) {
        mApprovalUrl = approvalURL;
        mTokenQueryParamKey = TOKEN_QUERY_PARAM_KEY_TOKEN;
        return this;
    }

    @Override
    public String getBrowserSwitchUrl() {
        return mApprovalUrl;
    }

    @Override
    public PayPalApiResult parseBrowserResponse(Uri uri) {
        String status = uri.getLastPathSegment();

        if (!Uri.parse(getSuccessUrl()).getLastPathSegment().equals(status)) {
            // return cancel result
            return new PayPalApiResult();
        }

        String requestXoToken = Uri.parse(mApprovalUrl).getQueryParameter(mTokenQueryParamKey);
        String responseXoToken = uri.getQueryParameter(mTokenQueryParamKey);
        if (responseXoToken != null && TextUtils.equals(requestXoToken, responseXoToken)) {
            try {
                JSONObject response = new JSONObject();
                response.put("webURL", uri.toString());
                return new PayPalApiResult(
                        null /*don't know the environment here*/,
                        PayPalApiResponseType.web,
                        response,
                        null /* email not sent back in checkout requests since Hermes doesn't return that info*/);
            } catch (JSONException e) {
                return new PayPalApiResult(new PayPalApiResponseParsingException(e));
            }
        } else {
            return new PayPalApiResult(
                    new PayPalBrowserSwitchException("The response contained inconsistent data."));
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);

        dest.writeString(mApprovalUrl);
        dest.writeString(mTokenQueryParamKey);
        dest.writeString(mPairingId);
    }

    protected PayPalApiCheckoutRequest(Parcel source) {
        super(source);

        mApprovalUrl = source.readString();
        mTokenQueryParamKey = source.readString();
        mPairingId = source.readString();
    }

    public static final Creator<PayPalApiCheckoutRequest> CREATOR = new Creator<PayPalApiCheckoutRequest>() {
        @Override
        public PayPalApiCheckoutRequest[] newArray(int size) {
            return new PayPalApiCheckoutRequest[size];
        }

        @Override
        public PayPalApiCheckoutRequest createFromParcel(Parcel source) {
            return new PayPalApiCheckoutRequest(source);
        }
    };
}
