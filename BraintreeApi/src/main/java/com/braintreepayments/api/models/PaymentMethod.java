package com.braintreepayments.api.models;

import android.os.Parcelable;

import com.braintreepayments.api.Utils;
import com.braintreepayments.api.exceptions.ServerException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Base class representing a method of payment for a customer.
 *
 * As a general rule, this SDK is designed to eliminate multiple payment methods differently.
 * PaymentMethod represents the common surface area of all payment methods, and can be handled by a
 * server interchangeably without special case logic.
 */
public abstract class PaymentMethod implements Parcelable, Serializable {

    private static final String PAYMENT_METHOD_COLLECTION_KEY = "paymentMethods";
    private static final String PAYMENT_METHOD_TYPE_KEY = "type";

    protected String nonce;
    protected String description;
    protected PaymentMethodOptions options;
    protected transient String mSource;

    protected void setOptions(PaymentMethodOptions options) {
        this.options = options;
    }

    /**
     * @return The nonce generated for this payment method by the Braintree gateway. The nonce will
     *          represent this PaymentMethod for the purposes of creating transactions and other monetary
     *          actions.
     */
    public String getNonce() {
        return nonce;
    }

    /**
     * @return The description of this PaymentMethod for displaying to a customer, e.g. 'Visa ending in...'
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the source of the PaymentMethod
     */
    public String getSource() {
        return mSource;
    }

    /**
     * Sets source of {@link com.braintreepayments.api.models.PaymentMethod} for analytics.
     * This is set automatically and should not need to be used outside of the SDK.
     * @param source Source of the PaymentMethod.
     */
    public void setSource(String source) {
        mSource = source;
    }

    /**
     * @return The type of this PaymentMethod for displaying to a customer, e.g. 'Visa'. Can be used
     *          for displaying appropriate logos, etc.
     */
    public abstract String getTypeLabel();

    /**
     * Parses a response from the Braintree gateway for a list of payment methods
     * @param paymentMethodsString Json-formatted String containing a list of {@link com.braintreepayments.api.models.PaymentMethod}s
     * @return List of {@link com.braintreepayments.api.models.PaymentMethod}s contained in paymentMethodsString
     * @throws ServerException if parsing JSON fails
     */
    public static List<PaymentMethod> parsePaymentMethods(String paymentMethodsString) throws ServerException {
        try {
            JSONArray paymentMethods = new JSONObject(paymentMethodsString).getJSONArray(PAYMENT_METHOD_COLLECTION_KEY);

            if (paymentMethods == null) {
                return Collections.emptyList();
            }

            List<PaymentMethod> paymentMethodsList = new ArrayList<PaymentMethod>();
            JSONObject paymentMethod;
            for(int i = 0; i < paymentMethods.length(); i++) {
                paymentMethod = paymentMethods.getJSONObject(i);
                String type = paymentMethod.getString(PAYMENT_METHOD_TYPE_KEY);
                if (type.equals(Card.PAYMENT_METHOD_TYPE)) {
                    paymentMethodsList.add(Utils.getGson().fromJson(paymentMethod.toString(), Card.class));
                } else if (type.equals(PayPalAccount.PAYMENT_METHOD_TYPE)) {
                    paymentMethodsList.add(Utils.getGson().fromJson(paymentMethod.toString(), PayPalAccount.class));
                }
            }

            return paymentMethodsList;
        } catch (JSONException e) {
            throw new ServerException("Parsing server response failed");
        }
    }

    /**
     * The interface to implement when creating a builder for a PaymentMethod
     * @param <T> Type of PaymentMethod for the Builder
     */
    public static interface Builder<T extends PaymentMethod> {

        /**
         * The parameter key for sending metadata attached to a
         * {@link com.braintreepayments.api.models.PaymentMethod}
         */
        public static final String METADATA_KEY = "_meta";

        /**
         * Sets the integration method associated with the
         * {@link com.braintreepayments.api.Braintree#create(com.braintreepayments.api.models.PaymentMethod.Builder)}
         * call for analytics use. Defaults to custom and does not need to ever be set.
         * @param integration
         */
        public Builder<T> integration(String integration);

        /**
         * Sets the source associated with the
         * {@link com.braintreepayments.api.Braintree#create(com.braintreepayments.api.models.PaymentMethod.Builder)}
         * call for analytics use. Set automatically
         * @param source
         */
        public Builder<T> source(String source);

        /**
         * Required for and handled by {@link com.braintreepayments.api.Braintree}. Not intended for general consumption.
         * @return A built {@link com.braintreepayments.api.models.PaymentMethod} appropriate to the builder's type.
         */
        public T build();

        /**
         * Required for and handled by {@link com.braintreepayments.api.Braintree}. Not intended for general consumption.
         * @return Serialized representation of {@link com.braintreepayments.api.models.PaymentMethod} for API use.
         * @deprecated Replaced by {@link #toJsonString()} in 1.0.7.
         */
        @Deprecated
        public Map<String, Object> toJson();

        /**
         * Required for and handled by {@link com.braintreepayments.api.Braintree}. Not intended for general consumption.
         * @return String representation of {@link com.braintreepayments.api.models.PaymentMethod} for API use.
         */
        public String toJsonString();

        /**
         * @param validate Flag to denote when the associated {@link com.braintreepayments.api.models.PaymentMethod}
         *   will be validated. When set to {@code true}, the {@link com.braintreepayments.api.models.PaymentMethod}
         *   will be validated immediately. When {@code false}, the {@link com.braintreepayments.api.models.PaymentMethod}
         *   will be validated when used by a server side library for a Braintree gateway action.
         */
        public Builder<T> validate(boolean validate);

        /**
         * Required for and handled by {@link com.braintreepayments.api.Braintree}. Not intended for general consumption.
         * @param json Raw JSON representation of a {@link com.braintreepayments.api.models.PaymentMethod}.
         * @return {@link com.braintreepayments.api.models.PaymentMethod} for use in payment method selection UIs.
         */
        public T fromJson(String json);

        public String getApiPath();
        public String getApiResource();
    }
}
