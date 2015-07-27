package com.braintreepayments.api.models;

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

    protected final JSONObject mJson;
    protected final JSONObject mPaymentMethodJson;
    private final JSONObject mOptionsJson;
    private final JSONObject mMetaJson;

    public PaymentMethodBuilder() {
        mJson = new JSONObject();
        mPaymentMethodJson = new JSONObject();
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
     * {@link com.braintreepayments.api.Braintree#create(PaymentMethodBuilder)}
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
     * {@link com.braintreepayments.api.Braintree#create(PaymentMethodBuilder)}
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
     * @param validate Flag to denote when the associated {@link com.braintreepayments.api.models.PaymentMethod}
     *   will be validated. When set to {@code true}, the {@link com.braintreepayments.api.models.PaymentMethod}
     *   will be validated immediately. When {@code false}, the {@link com.braintreepayments.api.models.PaymentMethod}
     *   will be validated when used by a server side library for a Braintree gateway action.
     */
    @SuppressWarnings("unchecked")
    public T validate(boolean validate) {
        try {
            mOptionsJson.put(VALIDATE_KEY, validate);
            mPaymentMethodJson.put(OPTIONS_KEY, mOptionsJson);
        } catch (JSONException ignored) {}

        return (T) this;
    }

    /**
     * @return String representation of {@link com.braintreepayments.api.models.PaymentMethod} for API use.
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
