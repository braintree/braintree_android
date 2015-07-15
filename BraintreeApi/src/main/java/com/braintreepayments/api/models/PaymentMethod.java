package com.braintreepayments.api.models;

import android.os.Parcelable;
import android.support.annotation.CallSuper;

import com.braintreepayments.api.exceptions.ServerException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class representing a method of payment for a customer. {@link PaymentMethod} represents the
 * common interface of all payment methods, and can be handled by a server interchangeably.
 */
public abstract class PaymentMethod implements Parcelable {

    private static final String PAYMENT_METHOD_COLLECTION_KEY = "paymentMethods";
    private static final String PAYMENT_METHOD_TYPE_KEY = "type";
    private static final String PAYMENT_METHOD_NONCE_KEY = "nonce";
    private static final String DESCRIPTION_KEY = "description";

    protected String mNonce;
    protected String mDescription;

    protected static JSONObject getJsonObjectForType(String apiResourceKey, String response)
            throws JSONException {
        return new JSONObject(response)
                .getJSONArray(apiResourceKey)
                .getJSONObject(0);
    }

    @CallSuper
    protected void fromJson(JSONObject json) throws JSONException {
        mNonce = json.getString(PAYMENT_METHOD_NONCE_KEY);
        mDescription = json.getString(DESCRIPTION_KEY);
    }

    /**
     * @return The nonce generated for this payment method by the Braintree gateway. The nonce will
     *          represent this PaymentMethod for the purposes of creating transactions and other monetary
     *          actions.
     */
    public String getNonce() {
        return mNonce;
    }

    /**
     * @return The description of this PaymentMethod for displaying to a customer, e.g. 'Visa ending in...'
     */
    public String getDescription() {
        return mDescription;
    }

    /**
     * @return The type of this PaymentMethod for displaying to a customer, e.g. 'Visa'. Can be used
     *          for displaying appropriate logos, etc.
     */
    public abstract String getTypeLabel();

    /**
     * Parses a response from the Braintree gateway for a list of payment methods.
     *
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

            List<PaymentMethod> paymentMethodsList = new ArrayList<>();
            JSONObject paymentMethod;
            for(int i = 0; i < paymentMethods.length(); i++) {
                paymentMethod = paymentMethods.getJSONObject(i);

                switch (paymentMethod.getString(PAYMENT_METHOD_TYPE_KEY)) {
                    case Card.PAYMENT_METHOD_TYPE:
                        Card card = new Card();
                        card.fromJson(paymentMethod);
                        paymentMethodsList.add(card);
                        break;
                    case PayPalAccount.PAYMENT_METHOD_TYPE:
                        PayPalAccount payPalAccount = new PayPalAccount();
                        payPalAccount.fromJson(paymentMethod);
                        paymentMethodsList.add(payPalAccount);
                        break;
                    case AndroidPayCard.PAYMENT_METHOD_TYPE:
                        AndroidPayCard androidPayCard = new AndroidPayCard();
                        androidPayCard.fromJson(paymentMethod);
                        paymentMethodsList.add(androidPayCard);
                        break;
                }
            }

            return paymentMethodsList;
        } catch (JSONException e) {
            throw new ServerException("Parsing server response failed");
        }
    }
}
