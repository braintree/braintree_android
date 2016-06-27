package com.braintreepayments.api.models;

import android.os.Parcel;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * An abstract class to extend when creating a builder for a payment method. Contains logic and
 * implementations shared by all payment methods.
 */
public abstract class PaymentMethodBuilder<T> {

    protected static final String OPTIONS_KEY = "options";
    private static final String VALIDATE_KEY = "validate";

    private String mIntegration = getDefaultIntegration();
    private String mSource = getDefaultSource();
    private boolean mValidate;
    private String mSessionId;

    public PaymentMethodBuilder() {}

    /**
     * Sets the integration method associated with the
     * {@link com.braintreepayments.api.TokenizationClient#tokenize(BraintreeFragment, PaymentMethodBuilder, PaymentMethodNonceCallback)}
     * call for analytics use. Defaults to custom and does not need to ever be set.
     *
     * @param integration the current integration style.
     */
    @SuppressWarnings("unchecked")
    public T integration(String integration) {
        mIntegration = integration;
        return (T) this;
    }

    /**
     * Sets the source associated with the
     * {@link com.braintreepayments.api.TokenizationClient#tokenize(BraintreeFragment, PaymentMethodBuilder, PaymentMethodNonceCallback)}
     * call for analytics use. Set automatically.
     *
     * @param source the source of the payment method.
     */
    @SuppressWarnings("unchecked")
    public T source(String source) {
        mSource = source;
        return (T) this;
    }

    /**
     * @param validate Flag to denote when the associated {@link PaymentMethodNonce}
     *   will be validated. When set to {@code true}, the {@link PaymentMethodNonce}
     *   will be validated immediately. When {@code false}, the {@link PaymentMethodNonce}
     *   will be validated when used by a server side library for a Braintree gateway action.
     */
    @SuppressWarnings("unchecked")
    public T validate(boolean validate) {
        mValidate = validate;
        return (T) this;
    }

    /**
     * @param sessionId sets the session id associated with this request. The session is a uuid tied to the lifetime of
     * a {@link BraintreeFragment}. This field is automatically set at the point of tokenization, and any previous
     * values ignored.
     */
    @SuppressWarnings("unchecked")
    public T setSessionId(String sessionId) {
        mSessionId = sessionId;
        return (T) this;
    }

    /**
     * @return String representation of {@link PaymentMethodNonce} for API use.
     */
    public String build() {
        JSONObject json = new JSONObject();
        JSONObject optionsJson = new JSONObject();
        JSONObject paymentMethodNonceJson = new JSONObject();
        JSONObject metaJson = new MetadataBuilder()
                .sessionId(mSessionId)
                .source(mSource)
                .integration(mIntegration)
                .build();

        try {
            json.put(MetadataBuilder.META_KEY, metaJson);

            if (mValidate) {
                optionsJson.put(VALIDATE_KEY, mValidate);
                paymentMethodNonceJson.put(OPTIONS_KEY, optionsJson);
            }

            build(json, paymentMethodNonceJson);
        } catch (JSONException ignored) {}

        return json.toString();
    }

    protected PaymentMethodBuilder(Parcel in) {
        mIntegration = in.readString();
        mSource = in.readString();
        mValidate = in.readByte() > 0;
        mSessionId = in.readString();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mIntegration);
        dest.writeString(mSource);
        dest.writeByte(mValidate ? (byte) 1 : 0);
        dest.writeString(mSessionId);
    }

    protected String getDefaultSource() {
        return "form";
    }

    protected String getDefaultIntegration() {
        return "custom";
    }

    protected abstract void build(JSONObject base, JSONObject paymentMethodNonceJson) throws JSONException;

    public abstract String getApiPath();

    public abstract String getResponsePaymentMethodType();
}
