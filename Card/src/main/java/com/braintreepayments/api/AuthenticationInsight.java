package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONObject;

/**
 * Information pertaining to the regulatory environment for a credit card if authentication insight
 * is requested during tokenization.
 */
public class AuthenticationInsight implements Parcelable {
    private static final String GRAPHQL_REGULATION_ENVIRONMENT_KEY = "customerAuthenticationRegulationEnvironment";
    private static final String REST_REGULATION_ENVIRONMENT_KEY = "regulationEnvironment";

    private String regulationEnvironment;

    static AuthenticationInsight fromJson(JSONObject json) {
        if (json == null) {
            return null;
        }

        AuthenticationInsight authInsight = new AuthenticationInsight();
        String regulationEnv;

        if (json.has(GRAPHQL_REGULATION_ENVIRONMENT_KEY)) {
            regulationEnv = Json.optString(json, GRAPHQL_REGULATION_ENVIRONMENT_KEY, null);
        } else {
            regulationEnv = Json.optString(json, REST_REGULATION_ENVIRONMENT_KEY, null);
        }

        if ("psdtwo".equalsIgnoreCase(regulationEnv)) {
            regulationEnv = "psd2";
        }

        if (regulationEnv != null ) {
            regulationEnv = regulationEnv.toLowerCase();
        }

        authInsight.regulationEnvironment = regulationEnv;

        return authInsight;
    }

    /**
     *
     * @return The regulation environment for the associated nonce to help determine the need
     * for 3D Secure.
     *
     * @see <a href="https://developers.braintreepayments.com/guides/3d-secure/advanced-options/android#authentication-insight">Documentation</a>
     * for possible values.
     */
    public String getRegulationEnvironment() {
        return regulationEnvironment;
    }

    public AuthenticationInsight() {}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(regulationEnvironment);
    }

    private AuthenticationInsight(Parcel in) {
        regulationEnvironment = in.readString();
    }

    public static final Creator<AuthenticationInsight> CREATOR = new Creator<AuthenticationInsight>() {
        public AuthenticationInsight createFromParcel(Parcel source) {
            return new AuthenticationInsight(source);
        }

        public AuthenticationInsight[] newArray(int size) {
            return new AuthenticationInsight[size];
        }
    };
}
