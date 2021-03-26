package com.braintreepayments.api;

import org.json.JSONException;
import org.json.JSONObject;

public class OpaquePaymentMethodNonce implements PaymentMethodInterface {

    private static final String TYPE_KEY = "type";

    private final int type;

    OpaquePaymentMethodNonce(JSONObject json) throws JSONException {
        String typeAsString = json.getString(TYPE_KEY);
        switch (typeAsString) {
            case "CreditCard":
                type = PaymentMethodType.CARD;
                break;
            case "PayPalAccount":
                type = PaymentMethodType.PAYPAL;
                break;
            case "VenmoAccount":
                type = PaymentMethodType.VENMO;
                break;
            case "VisaCheckoutCard":
                type = PaymentMethodType.VISA_CHECKOUT;
                break;
            default:
                type = PaymentMethodType.UNKNOWN;
                break;
        }
    }

    public @PaymentMethodType int getType() {
        return type;
    }

    @Override
    public String getTypeLabel() {
        switch (type) {
            case PaymentMethodType.CARD:
                // TODO: get card type from json (e.g. Visa, MasterCard, American Express)
                return "Card";
            case PaymentMethodType.PAYPAL:
                return "PayPal";
            case PaymentMethodType.VENMO:
                return "Venmo";
            case PaymentMethodType.VISA_CHECKOUT:
                return "Visa Checkout";
            case PaymentMethodType.UNKNOWN:
            default:
                // TODO: figure out what to do here
                return "Payment Method";
        }
    }

    @Override
    public String getNonce() {
        // TODO: get nonce from json depending on type
        return null;
    }
}
