package com.paypal.android.sdk.onetouch.core.sdk;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Wrapper of response from PayPal Wallet to SDK.
 */
public final class PayPalTouchResponseBundle implements Parcelable {
    private static final String TAG = PayPalTouchResponseBundle.class.getSimpleName();

    private final String version;
    private final String display_name;
    private final String access_token;
    private final String response_type;
    private final String authorization_code;
    private final String expires_in;
    private final String scope;
    private final String email;
    private final String photo_url;
    private final String error;
    private final String webURL;

    public PayPalTouchResponseBundle(String version, String display_name, String access_token,
            String response_type, String authorization_code, String expires_in,
            String scope, String email, String photo_url, String error, String webURL) {
        this.version = version;
        this.display_name = display_name;
        this.access_token = access_token;
        this.response_type = response_type;
        this.authorization_code = authorization_code;
        this.expires_in = expires_in;
        this.scope = scope;
        this.email = email;
        this.photo_url = photo_url;
        this.error = error;
        this.webURL = webURL;
    }

    /**
     * Required by {@link android.os.Parcelable}
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Required by {@link android.os.Parcelable}
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(version);
        dest.writeString(display_name);
        dest.writeString(access_token);
        dest.writeString(response_type);
        dest.writeString(authorization_code);
        dest.writeString(expires_in);
        dest.writeString(scope);
        dest.writeString(email);
        dest.writeString(photo_url);
        dest.writeString(error);
        dest.writeString(webURL);
    }

    private PayPalTouchResponseBundle(Parcel source) {
        this(source.readString(),
                source.readString(),
                source.readString(),
                source.readString(),
                source.readString(),
                source.readString(),
                source.readString(),
                source.readString(),
                source.readString(),
                source.readString(),
                source.readString());
    }

    /**
     * Required by {@link android.os.Parcelable}
     */
    public static final Creator<PayPalTouchResponseBundle> CREATOR =
            new Creator<PayPalTouchResponseBundle>() {

                @Override
                public PayPalTouchResponseBundle createFromParcel(Parcel source) {
                    return new PayPalTouchResponseBundle(source);
                }

                @Override
                public PayPalTouchResponseBundle[] newArray(int size) {
                    return new PayPalTouchResponseBundle[size];
                }
            };

    /**
     * @return JSONObject suitable for sending to a remote service.
     */
    public JSONObject toJSONObject() {
        JSONObject result = new JSONObject();
        try {
            result.put("version", version);
            result.put("display_name", display_name);
            result.put("access_token", access_token);
            result.put("response_type", response_type);
            result.put("authorization_code", authorization_code);
            result.put("expires_in", expires_in);
            result.put("scope", scope);
            result.put("email", email);
            result.put("photo_url", photo_url);
            result.put("error", error);
            result.put("webURL", webURL);

        } catch (JSONException e) {
            Log.e(TAG, "error encoding JSON", e);
            result = null;
        }
        return result;
    }

    public String getScope() {
        return scope;
    }
}
