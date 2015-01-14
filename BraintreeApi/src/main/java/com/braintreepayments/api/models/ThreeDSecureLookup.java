package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.braintreepayments.api.Utils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class to parse and contain 3D Secure lookup params
 */
public class ThreeDSecureLookup implements Parcelable {

    private String acsUrl;
    private String md;
    private String termUrl;
    private String pareq;

    public ThreeDSecureLookup() {}

    public static ThreeDSecureLookup fromJson(String jsonString) throws JSONException {
        return Utils.getGson().fromJson(new JSONObject(jsonString).optJSONObject("lookup").toString(),
                ThreeDSecureLookup.class);
    }

    public String getAcsUrl() {
        return acsUrl;
    }

    public String getMd() {
        return md;
    }

    public String getTermUrl() {
        return termUrl;
    }

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
