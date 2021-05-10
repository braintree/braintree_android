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

    static final String OPERATION_NAME_KEY = "operationName";
    static final String OPTIONS_KEY = "options";
    static final String VALIDATE_KEY = "validate";

    private String integration = getDefaultIntegration();
    private String source = getDefaultSource();
    private String sessionId;

    PaymentMethod() {
    }

    /**
     * Sets the integration method associated with the tokenization call for analytics use.
     * Defaults to custom and does not need to ever be set.
     *
     * @param integration the current integration style.
     */
    void setIntegration(String integration) {
        this.integration = integration;
    }

    /**
     * Sets the source associated with the tokenization call for analytics use. Set automatically.
     *
     * @param source the source of the payment method.
     */
    void setSource(String source) {
        this.source = source;
    }

    /**
     * @param sessionId sets the session id associated with this request. The session is a uuid.
     *                  This field is automatically set at the point of tokenization, and any previous
     *                  values ignored.
     */
    void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    JSONObject buildMetadataJSON() {
        return new MetadataBuilder()
                .sessionId(sessionId)
                .source(source)
                .integration(integration)
                .build();
    }


    JSONObject buildJSON() throws JSONException {
        JSONObject base = new JSONObject();
        base.put(MetadataBuilder.META_KEY, buildMetadataJSON());
        return base;
    }

    PaymentMethod(Parcel in) {
        integration = in.readString();
        source = in.readString();
        sessionId = in.readString();
    }

    void writeToParcel(Parcel dest, int flags) {
        dest.writeString(integration);
        dest.writeString(source);
        dest.writeString(sessionId);
    }

    String getDefaultSource() {
        return "form";
    }

    String getDefaultIntegration() {
        return "custom";
    }

    abstract String getApiPath();
}
