package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

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

    private String mAcsUrl;
    private String mMd;
    private String mTermUrl;
    private String mPareq;
    private String mThreeDSecureVersion;
    private String mTransactionId;

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
            lookup.mAcsUrl = null;
        } else {
            lookup.mAcsUrl = json.getString(ACS_URL_KEY);
        }

        lookup.mMd = json.getString(MD_KEY);
        lookup.mTermUrl = json.getString(TERM_URL_KEY);
        lookup.mPareq = json.getString(PA_REQ_KEY);
        lookup.mThreeDSecureVersion = Json.optString(json, THREE_D_SECURE_VERSION_KEY, "");
        lookup.mTransactionId = Json.optString(json, TRANSACTION_ID_KEY, "");

        return lookup;
    }

    /**
     * @return The acs url from a 3D Secure lookup. May be {@code null} in which case no
     * authentication will be performed.
     */
    public String getAcsUrl() {
        return mAcsUrl;
    }

    /**
     * @return The merchant descriptor from a 3D Secure lookup
     */
    public String getMd() {
        return mMd;
    }

    /**
     * @return The term url to be used for 3D Secure authentication
     */
    public String getTermUrl() {
        return mTermUrl;
    }

    /**
     * @return The pareq to be used for 3D Secure authentication
     */
    public String getPareq() {
        return mPareq;
    }

    /**
     * @return The version of 3D Secure this merchant is using
     */
    public String getThreeDSecureVersion() {
        return mThreeDSecureVersion;
    }

    /**
     * @return The transaction ID for this 3D Secure lookup
     */
    public String getTransactionId() {
        return mTransactionId;
    }

    /**
     * @return {@code boolean}
     * When `true`, the user will be presented with a 3D Secure challenge when calling
     * {@link ThreeDSecureClient#continuePerformVerification(FragmentActivity, ThreeDSecureRequest, ThreeDSecureResult, ThreeDSecureResultCallback)}
     */
    public boolean requiresUserAuthentication() {
        return mAcsUrl != null;
    }

    public ThreeDSecureLookup() {}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mAcsUrl);
        dest.writeString(mMd);
        dest.writeString(mTermUrl);
        dest.writeString(mPareq);
        dest.writeString(mThreeDSecureVersion);
        dest.writeString(mTransactionId);
    }

    private ThreeDSecureLookup(Parcel in) {
        mAcsUrl = in.readString();
        mMd = in.readString();
        mTermUrl = in.readString();
        mPareq = in.readString();
        mThreeDSecureVersion = in.readString();
        mTransactionId = in.readString();
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
