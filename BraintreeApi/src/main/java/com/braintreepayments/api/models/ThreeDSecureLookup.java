package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.braintreepayments.api.annotations.Beta;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class to parse and contain 3D Secure lookup params
 */
@Beta
public class ThreeDSecureLookup implements Parcelable {

    private Card card;
    private String acsUrl;
    private String md;
    private String termUrl;
    private String pareq;

    public ThreeDSecureLookup() {}

    /**
     * Used to parse a response from the Braintree Gateway to be used for 3D Secure
     *
     * @param jsonString The json response from the Braintree Gateway 3D Secure lookup route
     * @return The {@link com.braintreepayments.api.models.ThreeDSecureLookup} to use when
     *         performing 3D Secure authentication
     * @throws JSONException when parsing fails
     */
    public static ThreeDSecureLookup fromJson(String jsonString) throws JSONException {
        JSONObject json = new JSONObject(jsonString);

        Gson gson = new Gson();

        Card card = gson.fromJson(json.getJSONObject("paymentMethod").toString(), Card.class);
        card.setThreeDSecureInfo(gson.fromJson(json.getJSONObject("threeDSecureInfo").toString(),
                ThreeDSecureInfo.class));

        ThreeDSecureLookup lookup = gson.fromJson(json.getJSONObject("lookup").toString(),
                ThreeDSecureLookup.class);
        lookup.card = card;

        return lookup;
    }

    /**
     * @return The {@link com.braintreepayments.api.models.Card} from the 3D Secure lookup.
     * If {@link #getAcsUrl()} is {@code null} this card may be used immediately and will benefit
     * from 3D Secure.
     */
    public Card getCard() {
        return card;
    }

    /**
     * @return The acs url from a 3D Secure lookup. May be {@code null} in which case no
     * authentication will be performed.
     */
    public String getAcsUrl() {
        return acsUrl;
    }

    /**
     * @return The merchant descriptor from a 3D Secure lookup
     */
    public String getMd() {
        return md;
    }

    /**
     * @return The term url to be used for 3D Secure authentication
     */
    public String getTermUrl() {
        return termUrl;
    }

    /**
     * @return The pareq to be used for 3D Secure authentication
     */
    public String getPareq() {
        return pareq;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.acsUrl);
        dest.writeString(this.md);
        dest.writeString(this.termUrl);
        dest.writeString(this.pareq);
    }

    private ThreeDSecureLookup(Parcel in) {
        acsUrl = in.readString();
        md = in.readString();
        termUrl = in.readString();
        pareq = in.readString();
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
