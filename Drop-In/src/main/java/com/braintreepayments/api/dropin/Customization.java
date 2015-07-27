package com.braintreepayments.api.dropin;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Class to contain customizations. For use in starting
 * {@link BraintreePaymentActivity}.
 *
 * @see com.braintreepayments.api.dropin.Customization.CustomizationBuilder
 */
public class Customization implements Parcelable {

    private String mActionBarTitle;
    private int mActionBarLogo;
    private String mPrimaryDescription;
    private String mSecondaryDescription;
    private String mSubmitButtonText;
    private String mAmount;

    /**
     * @return The {@link android.app.ActionBar} title.
     */
    public String getActionBarTitle() {
        return mActionBarTitle;
    }

    /**
     * @return The {@link android.app.ActionBar} drawable logo.
     */
    public int getActionBarLogo() {
        return mActionBarLogo;
    }

    /**
     * @return The primary description.
     */
    public String getPrimaryDescription() {
        return mPrimaryDescription;
    }

    /**
     * @return The secondary description.
     */
    public String getSecondaryDescription() {
        return mSecondaryDescription;
    }

    /**
     * @return The submit button text.
     */
    public String getSubmitButtonText() {
        return mSubmitButtonText;
    }

    /**
     * @return The amount to display.
     */
    public String getAmount() {
        return mAmount;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mActionBarTitle);
        dest.writeInt(mActionBarLogo);
        dest.writeString(mPrimaryDescription);
        dest.writeString(mSecondaryDescription);
        dest.writeString(mSubmitButtonText);
        dest.writeString(mAmount);
    }

    public Customization() {}

    protected Customization(Parcel in) {
        mActionBarTitle = in.readString();
        mActionBarLogo = in.readInt();
        mPrimaryDescription = in.readString();
        mSecondaryDescription = in.readString();
        mSubmitButtonText = in.readString();
        mAmount = in.readString();
    }

    public static final Creator<Customization> CREATOR = new Creator<Customization>() {
        public Customization createFromParcel(Parcel source) {
            return new Customization(source);
        }

        public Customization[] newArray(int size) {
            return new Customization[size];
        }
    };

    /**
     * Builder for {@link com.braintreepayments.api.dropin.Customization} objects.
     * Used for instantiating {@link BraintreePaymentActivity}.
     */
    public static class CustomizationBuilder {

        private Customization mCustomization;

        public CustomizationBuilder() {
            mCustomization = new Customization();
        }

        /**
         * @param title The title to display in the action bar when present.
         */
        public CustomizationBuilder actionBarTitle(String title) {
            mCustomization.mActionBarTitle = title;
            return this;
        }

        /**
         * @param drawable The icon to display in the action bar when present.
         */
        public CustomizationBuilder actionBarLogo(int drawable) {
            mCustomization.mActionBarLogo = drawable;
            return this;
        }

        /**
         * @param primaryDescription Main header for description bar. Displayed in bold.
         */
        public CustomizationBuilder primaryDescription(String primaryDescription) {
            mCustomization.mPrimaryDescription = primaryDescription;
            return this;
        }

        /**
         * @param secondaryDescription Subheader for description bar. Displayed in normal weight text.
         */
        public CustomizationBuilder secondaryDescription(String secondaryDescription) {
            mCustomization.mSecondaryDescription = secondaryDescription;
            return this;
        }

        /**
         * @param submitButtonText Text for submit button. Will be combined with amount if set via {@link #amount(String)}.
         */
        public CustomizationBuilder submitButtonText(String submitButtonText) {
            mCustomization.mSubmitButtonText = submitButtonText;
            return this;
        }

        /**
         * @param amount Amount of transaction, including any currency symbols.
         * Will be used in header and submit button.
         */
        public CustomizationBuilder amount(String amount) {
            mCustomization.mAmount = amount;
            return this;
        }

        /**
         * Builds a {@link com.braintreepayments.api.dropin.Customization} to be passed to
         * {@link BraintreePaymentActivity}
         * as {@link BraintreePaymentActivity#EXTRA_CUSTOMIZATION}.
         *
         * @return The complete {@link com.braintreepayments.api.dropin.Customization}.
         */
        public Customization build() {
            return mCustomization;
        }
    }
}
