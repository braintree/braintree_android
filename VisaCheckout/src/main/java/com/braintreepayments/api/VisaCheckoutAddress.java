package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import org.json.JSONObject;

/**
 * A class containing Visa Checkout information about the user's address.
 */
public class VisaCheckoutAddress implements Parcelable {

    private String firstName;
    private String lastName;
    private String streetAddress;
    private String extendedAddress;
    private String locality;
    private String region;
    private String postalCode;
    private String countryCode;
    private String phoneNumber;

    public static VisaCheckoutAddress fromJson(JSONObject json) {
        if (json == null) {
            json = new JSONObject();
        }

        VisaCheckoutAddress visaCheckoutAddress = new VisaCheckoutAddress();

        visaCheckoutAddress.firstName = Json.optString(json, "firstName", "");
        visaCheckoutAddress.lastName = Json.optString(json, "lastName", "");
        visaCheckoutAddress.streetAddress = Json.optString(json, "streetAddress", "");
        visaCheckoutAddress.extendedAddress = Json.optString(json, "extendedAddress", "");
        visaCheckoutAddress.locality = Json.optString(json, "locality", "");
        visaCheckoutAddress.region = Json.optString(json, "region", "");
        visaCheckoutAddress.postalCode = Json.optString(json, "postalCode", "");
        visaCheckoutAddress.countryCode = Json.optString(json, "countryCode", "");
        visaCheckoutAddress.phoneNumber = Json.optString(json, "phoneNumber", "");

        return visaCheckoutAddress;
    }

    public VisaCheckoutAddress() {}

    /**
     * @return The user's first name.
     */
    @Nullable
    public String getFirstName() {
        return firstName;
    }

    /**
     * @return The user's last name.
     */
    @Nullable
    public String getLastName() {
        return lastName;
    }

    /**
     * @return The user's street address.
     */
    @Nullable
    public String getStreetAddress() {
        return streetAddress;
    }

    /**
     * @return The user's extended address.
     */
    @Nullable
    public String getExtendedAddress() {
        return extendedAddress;
    }

    /**
     * @return The user's locality.
     */
    @Nullable
    public String getLocality() {
        return locality;
    }

    /**
     * @return The user's region.
     */
    @Nullable
    public String getRegion() {
        return region;
    }

    /**
     * @return The user's postal code.
     */
    @Nullable
    public String getPostalCode() {
        return postalCode;
    }

    /**
     * @return The user's country code.
     */
    @Nullable
    public String getCountryCode() {
        return countryCode;
    }

    /**
     * @return The user's phone number.
     */
    @Nullable
    public String getPhoneNumber() {
        return phoneNumber;
    }

    public VisaCheckoutAddress(Parcel in) {
        firstName = in.readString();
        lastName = in.readString();
        streetAddress = in.readString();
        extendedAddress = in.readString();
        locality = in.readString();
        region = in.readString();
        postalCode = in.readString();
        countryCode = in.readString();
        phoneNumber = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(firstName);
        dest.writeString(lastName);
        dest.writeString(streetAddress);
        dest.writeString(extendedAddress);
        dest.writeString(locality);
        dest.writeString(region);
        dest.writeString(postalCode);
        dest.writeString(countryCode);
        dest.writeString(phoneNumber);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<VisaCheckoutAddress> CREATOR = new Creator<VisaCheckoutAddress>() {
        @Override
        public VisaCheckoutAddress createFromParcel(Parcel in) {
            return new VisaCheckoutAddress(in);
        }

        @Override
        public VisaCheckoutAddress[] newArray(int size) {
            return new VisaCheckoutAddress[size];
        }
    };
}
