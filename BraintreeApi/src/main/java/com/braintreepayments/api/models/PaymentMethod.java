package com.braintreepayments.api.models;

import android.os.Parcelable;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;

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
     * @throws JSONException
     */
    public static List<PaymentMethod> parsePaymentMethods(String paymentMethodsString)
            throws JSONException {
        JSONArray paymentMethods = new JSONObject(paymentMethodsString).getJSONArray(PAYMENT_METHOD_COLLECTION_KEY);

        if (paymentMethods == null) {
            return Collections.emptyList();
        }

        List<PaymentMethod> paymentMethodsList = new ArrayList<>();
        JSONObject json;
        PaymentMethod paymentMethod;
        for(int i = 0; i < paymentMethods.length(); i++) {
            json = paymentMethods.getJSONObject(i);
            paymentMethod = parsePaymentMethod(json, json.getString(PAYMENT_METHOD_TYPE_KEY));
            if (paymentMethod != null) {
                paymentMethodsList.add(paymentMethod);
            }
        }

        return paymentMethodsList;
    }

    /**
     * Parses a {@link PaymentMethod} from json.
     *
     * @param json {@link String} representation of a {@link PaymentMethod}.
     * @param type The {@link String} type of the {@link PaymentMethod}.
     * @return {@link PaymentMethod}
     * @throws JSONException
     */
    @Nullable
    public static PaymentMethod parsePaymentMethod(String json, String type) throws JSONException {
        return parsePaymentMethod(new JSONObject(json), type);
    }

    /**
     * Parses a {@link PaymentMethod} from json.
     *
     * @param json {@link JSONObject} representation of a {@link PaymentMethod}.
     * @param type The {@link String} type of the {@link PaymentMethod}.
     * @return {@link PaymentMethod}
     * @throws JSONException
     */
    @Nullable
    public static PaymentMethod parsePaymentMethod(JSONObject json, String type) throws JSONException {
        switch (type) {
            case Card.PAYMENT_METHOD_TYPE:
                if (json.has(Card.API_RESOURCE_KEY)) {
                    return Card.fromJson(json.toString());
                } else {
                    Card card = new Card();
                    card.fromJson(json);
                    return card;
                }
            case PayPalAccount.PAYMENT_METHOD_TYPE:
                if (json.has(PayPalAccount.API_RESOURCE_KEY)) {
                    return PayPalAccount.fromJson(json.toString());
                } else {
                    PayPalAccount payPalAccount = new PayPalAccount();
                    payPalAccount.fromJson(json);
                    return payPalAccount;
                }
            case AndroidPayCard.PAYMENT_METHOD_TYPE:
                if (json.has(AndroidPayCard.API_RESOURCE_KEY)) {
                    return AndroidPayCard.fromJson(json.toString());
                } else {
                    AndroidPayCard androidPayCard = new AndroidPayCard();
                    androidPayCard.fromJson(json);
                    return androidPayCard;
                }
            default:
                return null;
        }
    }
}
