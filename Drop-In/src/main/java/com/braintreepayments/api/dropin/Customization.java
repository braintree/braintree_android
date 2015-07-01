package com.braintreepayments.api.dropin;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Class to contain view customizations. For use in instantiating {@link com.braintreepayments.api.dropin.BraintreePaymentActivity}.
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

    protected String getActionBarTitle() {
        return mActionBarTitle;
    }

    protected void setActionBarTitle(String actionBarTitle) {
        mActionBarTitle = actionBarTitle;
    }

    protected int getActionBarLogo() {
        return mActionBarLogo;
    }

    protected void setActionBarLogo(int actionBarLogo) {
        mActionBarLogo = actionBarLogo;
    }

    protected String getPrimaryDescription() {
        return mPrimaryDescription;
    }

    protected void setPrimaryDescription(String primaryDescription) {
        mPrimaryDescription = primaryDescription;
    }

    protected String getSecondaryDescription() {
        return mSecondaryDescription;
    }

    protected void setSecondaryDescription(String secondaryDescription) {
        mSecondaryDescription = secondaryDescription;
    }

    protected String getSubmitButtonText() {
        return mSubmitButtonText;
    }

    protected void setSubmitButtonText(String submitButtonText) {
        mSubmitButtonText = submitButtonText;
    }

    protected String getAmount() {
        return mAmount;
    }

    protected void setAmount(String amount) {
        mAmount = amount;
    }

    @Override
    public int describeContents() { return 0; }

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
     * Used for instantiating {@link com.braintreepayments.api.dropin.BraintreePaymentActivity}.
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
            mCustomization.setActionBarTitle(title);
            return this;
        }

        /**
         * @param drawable The icon to display in the action bar when present.
         */
        public CustomizationBuilder actionBarLogo(int drawable) {
            mCustomization.setActionBarLogo(drawable);
            return this;
        }

        /**
         * @param primaryDescription Main header for description bar. Displayed in bold.
         */
        public CustomizationBuilder primaryDescription(String primaryDescription) {
            mCustomization.setPrimaryDescription(primaryDescription);
            return this;
        }

        /**
         * @param secondaryDescription Subheader for description bar. Displayed in normal weight text.
         */
        public CustomizationBuilder secondaryDescription(String secondaryDescription) {
            mCustomization.setSecondaryDescription(secondaryDescription);
            return this;
        }

        /**
         * @param submitButtonText Text for submit button. Will be combined with amount if set via {@link #setAmount(String)}.
         */
        public CustomizationBuilder submitButtonText(String submitButtonText) {
            mCustomization.setSubmitButtonText(submitButtonText);
            return this;
        }

        /**
         * @param amount Amount of transaction, including any currency symbols. Will be used in header and submit button.
         */
        public CustomizationBuilder amount(String amount) {
            mCustomization.setAmount(amount);
            return this;
        }

        /**
         * Builds a {@link com.braintreepayments.api.dropin.Customization} to be passed to {@link com.braintreepayments.api.dropin.BraintreePaymentActivity}
         * as {@link com.braintreepayments.api.dropin.BraintreePaymentActivity#EXTRA_CUSTOMIZATION}.
         * @return The complete {@link com.braintreepayments.api.dropin.Customization}.
         */
        public Customization build() {
            return mCustomization;
        }
    }
}
