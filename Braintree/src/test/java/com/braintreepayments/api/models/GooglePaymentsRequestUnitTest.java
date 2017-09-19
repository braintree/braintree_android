package com.braintreepayments.api.models;

import com.google.android.gms.wallet.ShippingAddressRequirements;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class GooglePaymentsRequestUnitTest {

    @Test
    public void returnsAllValues() {
        ShippingAddressRequirements shippingAddressRequirements = ShippingAddressRequirements.newBuilder().build();
        TransactionInfo transactionInfo = TransactionInfo.newBuilder()
                .setCurrencyCode("USD")
                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_NOT_CURRENTLY_KNOWN)
                .build();

        GooglePaymentsRequest request = new GooglePaymentsRequest()
                .allowPrepaidCards(true)
                .billingAddressFormat(WalletConstants.BILLING_ADDRESS_FORMAT_FULL)
                .billingAddressRequired(true)
                .emailRequired(true)
                .phoneNumberRequired(true)
                .shippingAddressRequired(true)
                .shippingAddressRequirements(shippingAddressRequirements)
                .transactionInfo(transactionInfo)
                .uiRequired(true);

        assertEquals(true, request.getAllowPrepaidCards().booleanValue());
        assertEquals(WalletConstants.BILLING_ADDRESS_FORMAT_FULL, request.getBillingAddressFormat().intValue());
        assertEquals(true, request.isBillingAddressRequired().booleanValue());
        assertEquals(true, request.isEmailRequired().booleanValue());
        assertEquals(true, request.isPhoneNumberRequired().booleanValue());
        assertEquals(true, request.isShippingAddressRequired().booleanValue());
        assertEquals(shippingAddressRequirements, request.getShippingAddressRequirements());
        assertEquals(transactionInfo, request.getTransactionInfo());
        assertEquals(true, request.isUiRequired().booleanValue());
    }

    @Test
    public void returnsNullForAllValuesWhenNotSet() {
        GooglePaymentsRequest request = new GooglePaymentsRequest();

        assertNull(request.getAllowPrepaidCards());
        assertNull(request.getBillingAddressFormat());
        assertNull(request.isBillingAddressRequired());
        assertNull(request.isEmailRequired());
        assertNull(request.isPhoneNumberRequired());
        assertNull(request.isShippingAddressRequired());
        assertNull(request.getShippingAddressRequirements());
        assertNull(request.getTransactionInfo());
        assertNull(request.isUiRequired());
    }
}
