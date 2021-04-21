package com.braintreepayments.demo;

import com.braintreepayments.api.BinData;
import com.braintreepayments.api.PaymentMethodNonce;
import com.braintreepayments.api.CardNonce;
import com.braintreepayments.api.GooglePayCardNonce;
import com.braintreepayments.api.LocalPaymentNonce;
import com.braintreepayments.api.PayPalAccountNonce;
import com.braintreepayments.api.PostalAddress;
import com.braintreepayments.api.VenmoAccountNonce;
import com.braintreepayments.api.VisaCheckoutAddress;
import com.braintreepayments.api.VisaCheckoutNonce;

import java.util.Arrays;
import java.util.List;

public class PaymentMethodNonceFormatter {

    public static String convertToString(PaymentMethodNonce nonce) {
        if (nonce instanceof CardNonce) {
            return convertCardNonceToString((CardNonce) nonce);
        } else if (nonce instanceof PayPalAccountNonce) {
            return convertPayPalNonceToString((PayPalAccountNonce) nonce);
        } else if (nonce instanceof GooglePayCardNonce) {
            return convertGooglePayNonceToString((GooglePayCardNonce) nonce);
        } else if (nonce instanceof VisaCheckoutNonce) {
            return convertVisaCheckoutNonceToString((VisaCheckoutNonce) nonce);
        } else if (nonce instanceof VenmoAccountNonce) {
            return convertVenmoNonceToString((VenmoAccountNonce) nonce);
        } else if (nonce instanceof LocalPaymentNonce) {
            return convertLocalPaymentNonceToString((LocalPaymentNonce) nonce);
        }
        return "";
    }

    private static String convertCardNonceToString(CardNonce nonce) {
        return "Card Last Two: " + nonce.getLastTwo() + "\n" +
                convertBinDataToString(nonce.getBinData()) + "\n" +
                "3DS: \n" +
                "         - isLiabilityShifted: " + nonce.getThreeDSecureInfo().isLiabilityShifted() + "\n" +
                "         - isLiabilityShiftPossible: " + nonce.getThreeDSecureInfo().isLiabilityShiftPossible() + "\n" +
                "         - wasVerified: " + nonce.getThreeDSecureInfo().wasVerified();
    }

    private static String convertPayPalNonceToString(PayPalAccountNonce nonce) {
        return "First Name: " + nonce.getFirstName() + "\n" +
                "Last Name: " + nonce.getLastName() + "\n" +
                "Email: " + nonce.getEmail() + "\n" +
                "Phone: " + nonce.getPhone() + "\n" +
                "Payer ID: " + nonce.getPayerId() + "\n" +
                "Client Metadata ID: " + nonce.getClientMetadataId() + "\n" +
                "Billing Address: " + formatPayPalAddress(nonce.getBillingAddress()) + "\n" +
                "Shipping Address: " + formatPayPalAddress(nonce.getShippingAddress());
    }

    private static String convertGooglePayNonceToString(GooglePayCardNonce nonce) {
        return "Underlying Card Last Two: " + nonce.getLastTwo() + "\n" +
                "Email: " + nonce.getEmail() + "\n" +
                "Billing Address: " + formatGooglePayAddress(nonce.getBillingAddress()) + "\n" +
                "Shipping Address: " + formatGooglePayAddress(nonce.getShippingAddress()) + "\n" +
                convertBinDataToString(nonce.getBinData());
    }

    private static String convertVisaCheckoutNonceToString(VisaCheckoutNonce nonce) {
        return "User Data\n" +
                "First Name: " + nonce.getUserData().getUserFirstName() + "\n" +
                "Last Name: " + nonce.getUserData().getUserLastName() + "\n" +
                "Full Name: " + nonce.getUserData().getUserFullName() + "\n" +
                "User Name: " + nonce.getUserData().getUsername() + "\n" +
                "Email: " + nonce.getUserData().getUserEmail() + "\n" +
                "Billing Address: " + formatVisaCheckoutAddress(nonce.getBillingAddress()) + "\n" +
                "Shipping Address: " + formatVisaCheckoutAddress(nonce.getShippingAddress()) + "\n" +
                convertBinDataToString(nonce.getBinData());
    }

    private static String convertVenmoNonceToString(VenmoAccountNonce nonce) {
        return "Username: " + nonce.getUsername();
    }

    private static String formatPayPalAddress(PostalAddress address) {
        String addressString = "";
        List<String> addresses = Arrays.asList(
                address.getRecipientName(),
                address.getStreetAddress(),
                address.getExtendedAddress(),
                address.getLocality(),
                address.getRegion(),
                address.getPostalCode(),
                address.getCountryCodeAlpha2()
        );

        for (String line : addresses) {
            if (line == null) {
                addressString += "null";
            } else {
                addressString += line;
            }
            addressString += " ";
        }

        return addressString;
    }

    private static String formatGooglePayAddress(PostalAddress address) {
        if (address == null) {
            return "null";
        }

        return address.getRecipientName() + " " +
                address.getStreetAddress() + " " +
                address.getExtendedAddress() + " " +
                address.getLocality() + " " +
                address.getRegion() + " " +
                address.getPostalCode() + " " +
                address.getSortingCode() + " " +
                address.getCountryCodeAlpha2() + " " +
                address.getPhoneNumber();
    }

    private static String formatVisaCheckoutAddress(VisaCheckoutAddress address) {
        return address.getFirstName() + " " +
                address.getLastName() + " " +
                address.getStreetAddress() + " " +
                address.getExtendedAddress() + " " +
                address.getLocality() + " " +
                address.getPostalCode() + " " +
                address.getRegion() + " " +
                address.getCountryCode() + " " +
                address.getPhoneNumber();
    }

    private static String formatLocalPaymentAddress(PostalAddress address) {
        return address.getRecipientName() + " " +
                address.getStreetAddress() + " " +
                address.getExtendedAddress() + " " +
                address.getLocality() + " " +
                address.getRegion() + " " +
                address.getPostalCode() + " " +
                address.getCountryCodeAlpha2();
    }

    private static String convertBinDataToString(BinData binData) {
        return "Bin Data: \n" +
                "         - Prepaid: " + binData.getHealthcare() + "\n" +
                "         - Healthcare: " + binData.getHealthcare() + "\n" +
                "         - Debit: " + binData.getDebit() + "\n" +
                "         - Durbin Regulated: " + binData.getDurbinRegulated() + "\n" +
                "         - Commercial: " + binData.getCommercial() + "\n" +
                "         - Payroll: " + binData.getPayroll() + "\n" +
                "         - Issuing Bank: " + binData.getIssuingBank() + "\n" +
                "         - Country of Issuance: " + binData.getCountryOfIssuance() + "\n" +
                "         - Product ID: " + binData.getProductId();
    }

    private static String convertLocalPaymentNonceToString(LocalPaymentNonce nonce) {
        return "First Name: " + nonce.getGivenName() + "\n" +
                "Last Name: " + nonce.getSurname() + "\n" +
                "Email: " + nonce.getEmail() + "\n" +
                "Phone: " + nonce.getPhone() + "\n" +
                "Payer ID: " + nonce.getPayerId() + "\n" +
                "Client Metadata ID: " + nonce.getClientMetadataId() + "\n" +
                "Billing Address: " + formatLocalPaymentAddress(nonce.getBillingAddress()) + "\n" +
                "Shipping Address: " + formatLocalPaymentAddress(nonce.getShippingAddress());
    }
}
