package com.braintreepayments.api.models;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * An abstract class to extend when creating a builder for a payment method. Contains logic and
 * implementations shared by all payment methods.
 */
public abstract class PaymentMethodBuilder<T> {

    protected static final String METADATA_KEY = "_meta";
    private static final String SOURCE_KEY = "source";
    private static final String INTEGRATION_KEY = "integration";
    private static final String OPTIONS_KEY = "options";
    private static final String VALIDATE_KEY = "validate";
    private static final String SESSION_ID_KEY = "sessionId";

    protected final JSONObject mJson;
    protected final JSONObject mPaymentMethodNonceJson;
    private final JSONObject mOptionsJson;
    private final JSONObject mMetaJson;

    public PaymentMethodBuilder() {
        mJson = new JSONObject();
        mPaymentMethodNonceJson = new JSONObject();
        mOptionsJson = new JSONObject();
        mMetaJson = new JSONObject();

        try {
            mMetaJson.put(SOURCE_KEY, getDefaultSource());
            mMetaJson.put(INTEGRATION_KEY, getDefaultIntegration());
            mJson.put(METADATA_KEY, mMetaJson);
        } catch (JSONException ignored) {}
    }

    /**
     * Sets the integration method associated with the
     * {@link com.braintreepayments.api.TokenizationClient#tokenize(BraintreeFragment, PaymentMethodBuilder, PaymentMethodNonceCallback)}
     * call for analytics use. Defaults to custom and does not need to ever be set.
     *
     * @param integration the current integration style.
     */
    @SuppressWarnings("unchecked")
    public T integration(String integration) {
        try {
            mMetaJson.put(INTEGRATION_KEY, integration);
        } catch (JSONException ignored) {}

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
        try {
            mMetaJson.put(SOURCE_KEY, source);
        } catch (JSONException ignored) { }

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
        try {
            mOptionsJson.put(VALIDATE_KEY, validate);
            mPaymentMethodNonceJson.put(OPTIONS_KEY, mOptionsJson);
        } catch (JSONException ignored) {}

        return (T) this;
    }

    /**
     * @param sessionId sets the session id associated with this request. The session is a uuid tied to the lifetime of
     * a {@link BraintreeFragment}. This field is automatically set at the point of tokenization, and any previous
     * values ignored.
     */
    @SuppressWarnings("unchecked")
    public T setSessionId(String sessionId) {
        try {
            mMetaJson.put(SESSION_ID_KEY, sessionId);
        } catch (JSONException ignored) {}

        return (T) this;
    }

    /**
     * @return String representation of {@link PaymentMethodNonce} for API use.
     */
    public String build() {
        return mJson.toString();
    }

    protected String getDefaultSource() {
        return "form";
    }

    protected String getDefaultIntegration() {
        return "custom";
    }

    public abstract String getApiPath();

    public abstract String getResponsePaymentMethodType();
}
