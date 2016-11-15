package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONObject;

public class VisaCheckoutShippingAddress implements Parcelable {
    private final String firstName;
    private final String lastName;
    private final String streetAddress;
    private final String locality;
    private final String region;
    private final String postalCode;
    private final String countryCode;

    public VisaCheckoutShippingAddress(JSONObject json) {
        firstName = json.optString("firstName");
        lastName = json.optString("lastName");
        streetAddress = json.optString("streetAddress");
        locality = json.optString("locality");
        region = json.optString("region");
        postalCode = json.optString("postalCode");
        countryCode = json.optString("countryCode");
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public String getLocality() {
        return locality;
    }

    public String getRegion() {
        return region;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public VisaCheckoutShippingAddress(Parcel in) {
        firstName = in.readString();
        lastName = in.readString();
        streetAddress = in.readString();
        locality = in.readString();
        region = in.readString();
        postalCode = in.readString();
        countryCode = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(firstName);
        dest.writeString(lastName);
        dest.writeString(streetAddress);
        dest.writeString(locality);
        dest.writeString(region);
        dest.writeString(postalCode);
        dest.writeString(countryCode);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<VisaCheckoutShippingAddress> CREATOR = new Creator<VisaCheckoutShippingAddress>() {
        @Override
        public VisaCheckoutShippingAddress createFromParcel(Parcel in) {
            return new VisaCheckoutShippingAddress(in);
        }

        @Override
        public VisaCheckoutShippingAddress[] newArray(int size) {
            return new VisaCheckoutShippingAddress[size];
        }
    };

}
