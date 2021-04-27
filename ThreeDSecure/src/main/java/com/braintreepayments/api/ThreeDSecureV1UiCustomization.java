package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * UI Customization Options for 3D Secure 1 Flows.
 */
public class ThreeDSecureV1UiCustomization implements Parcelable {

    private String redirectButtonText;
    private String redirectDescription;

    public ThreeDSecureV1UiCustomization() {}

    /**
     * Optional. Text displayed in the Redirect button after a 3DS1 challenge is completed in the browser.
     *
     * @param text The text to display in the button.
     */
    public void setRedirectButtonText(String text) {
        redirectButtonText = text;
    }

    /**
     * Optional. Text displayed below the Redirect button after a 3DS1 challenge is completed in the browser.
     *
     * @param text The text to display below the button.
     */
    public void setRedirectDescription(String text) {
        redirectDescription = text;
    }

    /**
     * @return Text displayed in the Redirect button after a 3DS1 challenge is completed in browser.
     */
    public String getRedirectButtonText() {
        return redirectButtonText;
    }

    /**
     * @return Text displayed below the Redirect button after a 3DS1 challenge is completed in browser.
     */
    public String getRedirectDescription() {
        return redirectDescription;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(redirectButtonText);
        dest.writeString(redirectDescription);
    }

    private ThreeDSecureV1UiCustomization(Parcel in) {
        redirectButtonText = in.readString();
        redirectDescription = in.readString();
    }

    public static final Creator<ThreeDSecureV1UiCustomization> CREATOR = new Creator<ThreeDSecureV1UiCustomization>() {
        @Override
        public ThreeDSecureV1UiCustomization createFromParcel(Parcel in) {
            return new ThreeDSecureV1UiCustomization(in);
        }

        @Override
        public ThreeDSecureV1UiCustomization[] newArray(int size) {
            return new ThreeDSecureV1UiCustomization[size];
        }
    };
}
