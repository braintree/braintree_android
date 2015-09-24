package com.braintreepayments.api.models;

import android.os.Parcel;
import android.util.Base64;

import com.braintreepayments.api.exceptions.InvalidArgumentException;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A class containing the configuration url and authorization for the current Braintree environment.
 */
public class ClientToken extends Authorization {

    protected static String BASE_64_MATCHER =
            "([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)";
    private static final String CONFIG_URL_KEY = "configUrl";
    private static final String AUTHORIZATION_FINGERPRINT_KEY = "authorizationFingerprint";

    private String mConfigUrl;
    private String mAuthorizationFingerprint;

    /**
     * Create a new {@link ClientToken} instance from a client token
     *
     * @param clientTokenString A client token from the Braintree Gateway
     * @return {@link ClientToken} instance
     */
    ClientToken(String clientTokenString) throws InvalidArgumentException {
        super(clientTokenString);
        if (clientTokenString.matches(BASE_64_MATCHER)) {
            clientTokenString = new String(Base64.decode(clientTokenString, Base64.DEFAULT));
        }

        try {
            JSONObject jsonObject = new JSONObject(clientTokenString);
            mConfigUrl = jsonObject.getString(CONFIG_URL_KEY);
            mAuthorizationFingerprint = jsonObject.getString(AUTHORIZATION_FINGERPRINT_KEY);
        } catch (JSONException e) {
            throw new InvalidArgumentException("Client token was invalid");
        }
    }

    @Override
    public String getConfigUrl() {
        return mConfigUrl;
    }

    /**
     * @return The authorizationFingerprint for the current session
     */
    public String getAuthorizationFingerprint() {
        return mAuthorizationFingerprint;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mConfigUrl);
        dest.writeString(mAuthorizationFingerprint);
    }

    protected ClientToken(Parcel in) {
        super(in);
        mConfigUrl = in.readString();
        mAuthorizationFingerprint = in.readString();
    }

    public static final Creator<ClientToken> CREATOR = new Creator<ClientToken>() {
        public ClientToken createFromParcel(Parcel source) {
            return new ClientToken(source);
        }

        public ClientToken[] newArray(int size) {
            return new ClientToken[size];
        }
    };
}
