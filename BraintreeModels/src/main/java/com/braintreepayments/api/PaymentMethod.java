package com.braintreepayments.api;

import android.os.Parcel;

import com.braintreepayments.api.GraphQLConstants.Keys;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * An abstract class to extend when creating a payment method. Contains logic and
 * implementations shared by all payment methods.
 */
public abstract class PaymentMethod {

    protected static final String OPTIONS_KEY = "options";
    protected static final String OPERATION_NAME_KEY = "operationName";

    private static final String VALIDATE_KEY = "validate";
    private static final String GRAPHQL_CLIENT_SDK_METADATA_KEY = "clientSdkMetadata";

    private String mIntegration = getDefaultIntegration();
    private String mSource = getDefaultSource();
    private boolean mValidate;
    private boolean mValidateSet;
    private String mSessionId;

    public PaymentMethod() {}

    /**
     * Sets the integration method associated with the tokenization call for analytics use.
     * Defaults to custom and does not need to ever be set.
     *
     * @param integration the current integration style.
     */
    void setIntegration(String integration) {
        mIntegration = integration;
    }

    /**
     * Sets the source associated with the tokenization call for analytics use. Set automatically.
     *
     * @param source the source of the payment method.
     */
    void setSource(String source) {
        mSource = source;
    }

    /**
     * @param validate Flag to denote when the associated {@link BraintreeNonce}
     *   will be validated. When set to {@code true}, the {@link BraintreeNonce}
     *   will be validated immediately. When {@code false}, the {@link BraintreeNonce}
     *   will be validated when used by a server side library for a Braintree gateway action.
     */
    public void setValidate(boolean validate) {
        mValidate = validate;
        mValidateSet = true;
    }

    /**
     * @param sessionId sets the session id associated with this request. The session is a uuid.
     * This field is automatically set at the point of tokenization, and any previous
     * values ignored.
     */
    void setSessionId(String sessionId) {
        mSessionId = sessionId;
    }

    /**
     * @return String representation of {@link BraintreeNonce} for API use.
     */
    public String buildJSON() {
        JSONObject base = new JSONObject();
        JSONObject optionsJson = new JSONObject();
        JSONObject paymentMethodNonceJson = new JSONObject();

        try {
            base.put(MetadataBuilder.META_KEY, new MetadataBuilder()
                    .sessionId(mSessionId)
                    .source(mSource)
                    .integration(mIntegration)
                    .build());

            if (mValidateSet) {
                optionsJson.put(VALIDATE_KEY, mValidate);
                paymentMethodNonceJson.put(OPTIONS_KEY, optionsJson);
            }

            buildJSON(base, paymentMethodNonceJson);
        } catch (JSONException ignored) {}

        return base.toString();
    }

    /**
     * @param authorization The current authorization being used.
     * @return String representation of a GraphQL request for {@link BraintreeNonce}.
     * @throws BraintreeException Thrown if resources cannot be accessed.
     */
    public String buildGraphQL(Authorization authorization) throws BraintreeException {
        JSONObject base = new JSONObject();
        JSONObject input = new JSONObject();
        JSONObject variables = new JSONObject();

        try {
            base.put(GRAPHQL_CLIENT_SDK_METADATA_KEY, new MetadataBuilder()
                    .sessionId(mSessionId)
                    .source(mSource)
                    .integration(mIntegration)
                    .build());

            JSONObject optionsJson = new JSONObject();
            if (mValidateSet) {
                optionsJson.put(VALIDATE_KEY, mValidate);
            } else {
                if (authorization instanceof ClientToken) {
                    optionsJson.put(VALIDATE_KEY, true);
                } else if (authorization instanceof TokenizationKey) {
                    optionsJson.put(VALIDATE_KEY, false);
                }
            }
            input.put(OPTIONS_KEY, optionsJson);
            variables.put(Keys.INPUT, input);

            buildGraphQL(base, variables);

            base.put(Keys.VARIABLES, variables);
        } catch (JSONException ignored) {}

        return base.toString();
    }

    protected PaymentMethod(Parcel in) {
        mIntegration = in.readString();
        mSource = in.readString();
        mValidate = in.readByte() > 0;
        mValidateSet = in.readByte() > 0;
        mSessionId = in.readString();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mIntegration);
        dest.writeString(mSource);
        dest.writeByte(mValidate ? (byte) 1 : 0);
        dest.writeByte(mValidateSet ? (byte) 1 : 0);
        dest.writeString(mSessionId);
    }

    protected String getDefaultSource() {
        return "form";
    }

    protected String getDefaultIntegration() {
        return "custom";
    }

    protected abstract void buildJSON(JSONObject base, JSONObject paymentMethodNonceJson) throws JSONException;

    protected abstract void buildGraphQL(JSONObject base, JSONObject input) throws BraintreeException,
            JSONException;

    public abstract String getApiPath();

    public abstract String getResponsePaymentMethodType();
}
