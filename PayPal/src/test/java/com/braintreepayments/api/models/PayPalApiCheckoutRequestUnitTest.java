package com.braintreepayments.api.models;

import android.os.Parcel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class PayPalApiCheckoutRequestUnitTest {

    @Test
    public void pairingId_setsClientMetadataId() {
        PayPalApiCheckoutRequest request = new PayPalApiCheckoutRequest()
                .pairingId(RuntimeEnvironment.application, "pairing-id");

        assertEquals("pairing-id", request.getClientMetadataId());
    }

    @Test
    public void parcels() {
        PayPalApiCheckoutRequest request = new PayPalApiCheckoutRequest()
                .environment("test")
                .clientId("client-id")
                .pairingId(RuntimeEnvironment.application, "pairing-id")
                .clientMetadataId("client-metadata-id")
                .cancelUrl("com.braintreepayments.demo.braintree.cancel", "cancel")
                .successUrl("com.braintreepayments.demo.braintree.success", "success")
                .approvalURL("com.braintreepayments.demo.braintree.approval-url");

        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        PayPalApiCheckoutRequest parceledRequest = PayPalApiCheckoutRequest.CREATOR.createFromParcel(parcel);

        assertEquals("test", parceledRequest.getEnvironment());
        assertEquals("client-id", parceledRequest.getClientId());
        assertEquals("client-metadata-id", parceledRequest.getClientMetadataId());
        assertEquals("pairing-id", parceledRequest.getPairingId());
        assertEquals("com.braintreepayments.demo.braintree.cancel://onetouch/v1/cancel", parceledRequest.getCancelUrl());
        assertEquals("com.braintreepayments.demo.braintree.success://onetouch/v1/success", parceledRequest.getSuccessUrl());
        assertEquals("com.braintreepayments.demo.braintree.approval-url", parceledRequest.mApprovalUrl);
        assertEquals("token", parceledRequest.mTokenQueryParamKey);
    }
}
