package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

/**
 * Information pertaining to the regulatory environment for a credit card if authentication insight
 * is requested during tokenization.
 */
public class AuthenticationInsight implements Parcelable {
    private static final String GRAPHQL_REGULATION_ENVIRONMENT_KEY = "customerAuthenticationRegulationEnvironment";
    private static final String REST_REGULATION_ENVIRONMENT_KEY = "regulationEnvironment";

    private final String regulationEnvironment;

    static AuthenticationInsight fromJson(JSONObject json) {
        if (json == null) {
            return null;
        }

        String regulationEnv;
        if (json.has(GRAPHQL_REGULATION_ENVIRONMENT_KEY)) {
            regulationEnv = Json.optString(json, GRAPHQL_REGULATION_ENVIRONMENT_KEY, "");
        } else {
            regulationEnv = Json.optString(json, REST_REGULATION_ENVIRONMENT_KEY, "");
        }

        if ("psdtwo".equalsIgnoreCase(regulationEnv)) {
            regulationEnv = "psd2";
        }
        regulationEnv = regulationEnv.toLowerCase();

        return new AuthenticationInsight(regulationEnv);
    }

    AuthenticationInsight(String regulationEnvironment) {
        this.regulationEnvironment = regulationEnvironment;
    }

    /**
     *
     * @return The regulation environment for the associated nonce to help determine the need
     * for 3D Secure.
     *
     * @see <a href="https://developer.paypal.com/braintree/docs/guides/3d-secure/advanced-options/android/v4#authentication-insight">Documentation</a>
     * for possible values.
     */
    @NonNull
    public String getRegulationEnvironment() {
        return regulationEnvironment;
    }

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
