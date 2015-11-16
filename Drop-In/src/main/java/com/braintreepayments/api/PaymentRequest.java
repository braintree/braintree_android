package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.wallet.Cart;

import java.util.List;

/**
 * Used to start {@link BraintreePaymentActivity} and {@link PaymentButton} with specified options.
 */
public class PaymentRequest implements Parcelable {

    private String mAuthorization;

    private String mAmount;
    private String mCurrencyCode;

    private Cart mAndroidPayCart;
    private boolean mAndroidPayShippingAddressRequired;
    private boolean mAndroidPayPhoneNumberRequired;
    private int mAndroidPayRequestCode;

    private List<String> mPayPalAdditionalScopes;

    private String mActionBarTitle;
    private int mActionBarLogo;
    private String mPrimaryDescription;
    private String mSecondaryDescription;
    private String mSubmitButtonText;

    public PaymentRequest() {}

    /**
     * Provide authorization allowing this client to communicate with Braintree. Either
     * {@link #clientToken(String)} or {@link #tokenizationKey(String)} must be set or an
     * {@link com.braintreepayments.api.exceptions.AuthenticationException} will occur.
     *
     * @param clientToken The client token to use for the request.
     */
    public PaymentRequest clientToken(String clientToken) {
        mAuthorization = clientToken;
        return this;
    }

    /**
     * Provide authorization allowing this client to communicate with Braintree. Either
     * {@link #clientToken(String)} or {@link #tokenizationKey(String)} must be set or an
     * {@link com.braintreepayments.api.exceptions.AuthenticationException} will occur.
     *
     * @param tokenizationKey The tokenization key to use for the request.
     */
    public PaymentRequest tokenizationKey(String tokenizationKey) {
        mAuthorization = tokenizationKey;
        return this;
    }

    /**
     * This method is optional.
     *
     * @param amount Amount of the transaction.
     */
    public PaymentRequest amount(String amount) {
        mAmount = amount;
        return this;
    }

    /**
     * This method is optional.
     *
     * @param currencyCode Currency code of the transaction.
     */
    public PaymentRequest currencyCode(String currencyCode) {
        mCurrencyCode = currencyCode;
        return this;
    }

    /**
     * This method is optional.
     *
     * @param cart The Android Pay {@link Cart} for the transaction.
     */
    public PaymentRequest androidPayCart(Cart cart) {
        mAndroidPayCart = cart;
        return this;
    }

    /**
     * This method is optional.
     *
     * @param shippingAddressRequired {@code true} if Android Pay requests should request a
     *        shipping address from the user.
     */
    public PaymentRequest androidPayShippingAddressRequired(boolean shippingAddressRequired) {
        mAndroidPayShippingAddressRequired = shippingAddressRequired;
        return this;
    }

    /**
     * This method is optional.
     *
     * @param phoneNumberRequired {@code true} if Android Pay requests should request a phone
     *        number from the user.
     */
    public PaymentRequest androidPayPhoneNumberRequired(boolean phoneNumberRequired) {
        mAndroidPayPhoneNumberRequired = phoneNumberRequired;
        return this;
    }

    /**
     * This method is optional.
     *
     * @param requestCode The requestCode to use when making an Android Pay
     *        {@link com.google.android.gms.wallet.MaskedWalletRequest} with
     *        {@link android.app.Activity#startActivityForResult(Intent, int)}}.
     */
    public PaymentRequest androidPayRequestCode(int requestCode) {
        mAndroidPayRequestCode = requestCode;
        return this;
    }

    /**
     * Set additional scopes to request when a user is authorizing PayPal.
     *
     * This method is optional.
     *
     * @param additionalScopes A {@link java.util.List} of additional scopes.
     *        Ex: PayPal.SCOPE_ADDRESS.
     *        Acceptable scopes are defined in {@link PayPal}.
     */
    public PaymentRequest paypalAdditionalScopes(List<String> additionalScopes) {
        mPayPalAdditionalScopes = additionalScopes;
        return this;
    }

    /**
     * This method is optional.
     *
     * @param title The title to display in the action bar when present.
     */
    public PaymentRequest actionBarTitle(String title) {
        mActionBarTitle = title;
        return this;
    }

    /**
     * This method is optional.
     *
     * @param drawable The icon to display in the action bar when present.
     */
    public PaymentRequest actionBarLogo(int drawable) {
        mActionBarLogo = drawable;
        return this;
    }

    /**
     * This method is optional.
     *
     * @param primaryDescription Main header for description bar. Displayed in bold.
     */
    public PaymentRequest primaryDescription(String primaryDescription) {
        mPrimaryDescription = primaryDescription;
        return this;
    }

    /**
     * This method is optional.
     *
     * @param secondaryDescription Subheader for description bar. Displayed in normal weight text.
     */
    public PaymentRequest secondaryDescription(String secondaryDescription) {
        mSecondaryDescription = secondaryDescription;
        return this;
    }

    /**
     * This method is optional.
     *
     * @param submitButtonText Text for submit button. Displayed in uppercase.
     *        Will be combined with amount if set via {@link #amount(String)}.
     */
    public PaymentRequest submitButtonText(String submitButtonText) {
        mSubmitButtonText = submitButtonText;
        return this;
    }

    /**
     * Get an {@link Intent} that can be used in {@link android.app.Activity#startActivityForResult(Intent, int)}
     * to launch {@link BraintreePaymentActivity} and the Drop-in UI.
     *
     * @param context
     * @return {@link Intent} containing all of the options set in {@link PaymentRequest}.
     */
    public Intent getIntent(Context context) {
        return new Intent(context, BraintreePaymentActivity.class)
                .putExtra(BraintreePaymentActivity.EXTRA_CHECKOUT_REQUEST, this);
    }

    String getAuthorization() {
        return mAuthorization;
    }

    String getAmount() {
        return mAmount;
    }

    String getCurrencyCode() {
        return mCurrencyCode;
    }

    Cart getAndroidPayCart() throws NoClassDefFoundError {
        return mAndroidPayCart;
    }

    boolean isAndroidPayShippingAddressRequired() {
        return mAndroidPayShippingAddressRequired;
    }

    boolean isAndroidPayPhoneNumberRequired() {
        return mAndroidPayPhoneNumberRequired;
    }

    int getAndroidPayRequestCode() {
        return mAndroidPayRequestCode;
    }

    List<String> getPayPalAdditionalScopes() {
        return mPayPalAdditionalScopes;
    }

    String getActionBarTitle() {
        return mActionBarTitle;
    }

    int getActionBarLogo() {
        return mActionBarLogo;
    }

    String getPrimaryDescription() {
        return mPrimaryDescription;
    }

    String getSecondaryDescription() {
        return mSecondaryDescription;
    }

    String getSubmitButtonText() {
        return mSubmitButtonText;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mAuthorization);
        dest.writeString(mAmount);
        dest.writeString(mCurrencyCode);

        try {
            Cart.class.getClassLoader();
            dest.writeParcelable(mAndroidPayCart, 0);
            dest.writeByte(mAndroidPayShippingAddressRequired ? (byte) 1 : (byte) 0);
            dest.writeByte(mAndroidPayPhoneNumberRequired ? (byte) 1 : (byte) 0);
            dest.writeInt(mAndroidPayRequestCode);
        } catch (NoClassDefFoundError ignored) {}

        dest.writeStringList(mPayPalAdditionalScopes);
        dest.writeString(mActionBarTitle);
        dest.writeInt(mActionBarLogo);
        dest.writeString(mPrimaryDescription);
        dest.writeString(mSecondaryDescription);
        dest.writeString(mSubmitButtonText);
    }

    protected PaymentRequest(Parcel in) {
        mAuthorization = in.readString();
        mAmount = in.readString();
        mCurrencyCode = in.readString();

        try {
            mAndroidPayCart = in.readParcelable(Cart.class.getClassLoader());
            mAndroidPayShippingAddressRequired = in.readByte() != 0;
            mAndroidPayPhoneNumberRequired = in.readByte() != 0;
            mAndroidPayRequestCode = in.readInt();
        } catch (NoClassDefFoundError ignored) {}

        mPayPalAdditionalScopes = in.createStringArrayList();
        mActionBarTitle = in.readString();
        mActionBarLogo = in.readInt();
        mPrimaryDescription = in.readString();
        mSecondaryDescription = in.readString();
        mSubmitButtonText = in.readString();
    }

    public static final Creator<PaymentRequest> CREATOR = new Creator<PaymentRequest>() {
        public PaymentRequest createFromParcel(Parcel source) {
            return new PaymentRequest(source);
        }

        public PaymentRequest[] newArray(int size) {
            return new PaymentRequest[size];
        }
    };
}
