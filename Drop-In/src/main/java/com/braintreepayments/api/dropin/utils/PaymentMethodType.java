package com.braintreepayments.api.dropin.utils;

import com.braintreepayments.api.dropin.R;

public enum PaymentMethodType {

    AMEX(R.drawable.bt_amex, R.string.bt_descriptor_amex, "American Express"),
    ANDROID_PAY(R.drawable.bt_android_pay, R.string.bt_descriptor_android_pay, "Google Wallet"),
    DINERS(R.drawable.bt_diners, R.string.bt_descriptor_diners, "Diners"),
    DISCOVER(R.drawable.bt_discover, R.string.bt_descriptor_discover, "Discover"),
    JCB(R.drawable.bt_jcb, R.string.bt_descriptor_jcb, "JCB"),
    MAESTRO(R.drawable.bt_maestro, R.string.bt_descriptor_maestro, "Maestro"),
    MASTERCARD(R.drawable.bt_mastercard, R.string.bt_descriptor_mastercard, "MasterCard"),
    PAYPAL(R.drawable.bt_paypal, R.string.bt_descriptor_paypal, "PayPal"),
    VISA(R.drawable.bt_visa, R.string.bt_descriptor_visa, "Visa"),
    UNKNOWN(0, R.string.bt_descriptor_unknown, "unknown");

    private final int mDrawable;
    private final int mLocalizedName;
    private String mCanonicalName;

    PaymentMethodType(int drawable, int localizedName, String canonicalName) {
        mDrawable = drawable;
        mLocalizedName = localizedName;
        mCanonicalName = canonicalName;
    }

    /**
     * @param paymentMethodType A {@link String} representing a canonical name for a payment method.
     *
     * @return a {@link PaymentMethodType} for for the given {@link String}, or
     *         {@link PaymentMethodType#UNKNOWN} if no match could be made.
     */
    public static PaymentMethodType forType(String paymentMethodType) {
        for (PaymentMethodType type : values()) {
            if (type.mCanonicalName.equals(paymentMethodType)) {
                return type;
            }
        }
        return UNKNOWN;
    }

    /**
     * @return An id representing a {@link android.graphics.drawable.Drawable} icon for the current
     *         {@link PaymentMethodType}.
     */
    public int getDrawable() {
        return mDrawable;
    }

    /**
     * @return An id representing a localized {@link String} for the current
     *         {@link PaymentMethodType}.
     */
    public int getLocalizedName() {
        return mLocalizedName;
    }

    /**
     * @return A {@link String} name of the {@link PaymentMethodType} as it is categorized by
     *         Braintree.
     */
    public String getCanonicalName() {
        return mCanonicalName;
    }
}
