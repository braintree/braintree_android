package com.braintreepayments.api;

class PaymentMethodTypeUtils {
    private PaymentMethodTypeUtils() {}

    static @PaymentMethodType int paymentMethodTypeFromString(String typeString) {
        switch (typeString) {
            case "CreditCard":
                return PaymentMethodType.CARD;
            case "PayPalAccount":
                return PaymentMethodType.PAYPAL;
            case "VisaCheckoutCard":
                return PaymentMethodType.VISA_CHECKOUT;
            case "VenmoAccount":
                return PaymentMethodType.VENMO;
            case "AndroidPayCard":
                return PaymentMethodType.GOOGLE_PAY;
            default:
                return PaymentMethodType.UNKNOWN;
        }
    }

    static String displayNameFromPaymentMethodType(@PaymentMethodType int type) {
        switch (type) {
            case PaymentMethodType.PAYPAL:
                return "PayPal";
            case PaymentMethodType.VISA_CHECKOUT:
                return "Visa Checkout";
            case PaymentMethodType.VENMO:
                return "Venmo";
            case PaymentMethodType.GOOGLE_PAY:
                return "Google Pay";
            case PaymentMethodType.CARD:
                return "Card";
            case PaymentMethodType.LOCAL_PAYMENT:
                return "Local Payment";
            case PaymentMethodType.UNKNOWN:
            default:
                return "Unknown";
        }
    }
}
