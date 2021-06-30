package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class to parse and contain 3D Secure lookup params
 */
public class ThreeDSecureLookup implements Parcelable {

    private static final String ACS_URL_KEY = "acsUrl";
    private static final String MD_KEY = "md";
    private static final String TERM_URL_KEY = "termUrl";
    private static final String PA_REQ_KEY = "pareq";
    private static final String THREE_D_SECURE_VERSION_KEY = "threeDSecureVersion";
    private static final String TRANSACTION_ID_KEY = "transactionId";

    private String acsUrl;
    private String md;
    private String termUrl;
    private String pareq;
    private String threeDSecureVersion;
    private String transactionId;

    /**
     * Used to parse a response from the Braintree Gateway to be used for 3D Secure.
     *
     * @param jsonString The json response from the Braintree Gateway 3D Secure lookup route.
     * @return The {@link ThreeDSecureLookup} to use when
     *         performing 3D Secure authentication.
     * @throws JSONException when parsing fails.
     */
    static ThreeDSecureLookup fromJson(String jsonString) throws JSONException {
        ThreeDSecureLookup lookup = new ThreeDSecureLookup();

        JSONObject json = new JSONObject(jsonString);
        if (json.isNull(ACS_URL_KEY)) {
            lookup.acsUrl = null;
        } else {
            lookup.acsUrl = json.getString(ACS_URL_KEY);
        }

        lookup.md = json.getString(MD_KEY);
        lookup.termUrl = json.getString(TERM_URL_KEY);
        lookup.pareq = json.getString(PA_REQ_KEY);
        lookup.threeDSecureVersion = Json.optString(json, THREE_D_SECURE_VERSION_KEY, "");
        lookup.transactionId = Json.optString(json, TRANSACTION_ID_KEY, "");

        return lookup;
    }

    /**
     * @return The acs url from a 3D Secure lookup. May be {@code null} in which case no
     * authentication will be performed.
     */
    @Nullable
    public String getAcsUrl() {
        return acsUrl;
    }

    /**
     * @return The merchant descriptor from a 3D Secure lookup
     */
    @NonNull
    public String getMd() {
        return md;
    }

    /**
     * @return The term url to be used for 3D Secure authentication
     */
    @NonNull
    public String getTermUrl() {
        return termUrl;
    }

    /**
     * @return The pareq to be used for 3D Secure authentication
     */
    @NonNull
    public String getPareq() {
        return pareq;
    }

    /**
     * @return The version of 3D Secure this merchant is using
     */
    @NonNull
    public String getThreeDSecureVersion() {
        return threeDSecureVersion;
    }

    /**
     * @return The transaction ID for this 3D Secure lookup
     */
    @NonNull
    public String getTransactionId() {
        return transactionId;
    }

    /**
     * @return {@code boolean}
     * When `true`, the user will be presented with a 3D Secure challenge when calling
     * {@link ThreeDSecureClient#continuePerformVerification(FragmentActivity, ThreeDSecureRequest, ThreeDSecureResult, ThreeDSecureResultCallback)}
     */
    public boolean requiresUserAuthentication() {
        return acsUrl != null;
    }

    public ThreeDSecureLookup() {}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(acsUrl);
        dest.writeString(md);
        dest.writeString(termUrl);
        dest.writeString(pareq);
        dest.writeString(threeDSecureVersion);
        dest.writeString(transactionId);
    }

    private ThreeDSecureLookup(Parcel in) {
        acsUrl = in.readString();
        md = in.readString();
        termUrl = in.readString();
        pareq = in.readString();
        threeDSecureVersion = in.readString();
        transactionId = in.readString();
    }

    public static final Creator<ThreeDSecureLookup> CREATOR = new Creator<ThreeDSecureLookup>() {
        public ThreeDSecureLookup createFromParcel(Parcel source) {
            return new ThreeDSecureLookup(source);
        }

        public ThreeDSecureLookup[] newArray(int size) {
            return new ThreeDSecureLookup[size];
        }
    };
}
