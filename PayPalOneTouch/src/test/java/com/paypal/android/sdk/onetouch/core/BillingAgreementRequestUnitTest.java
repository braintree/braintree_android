package com.paypal.android.sdk.onetouch.core;

import android.os.Parcel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class BillingAgreementRequestUnitTest {

    @Test
    public void pairingId_setsClientMetadataId() {
        BillingAgreementRequest request = new BillingAgreementRequest()
                .pairingId(RuntimeEnvironment.application, "pairing-id");

        assertEquals("pairing-id", request.getClientMetadataId());
    }

    @Test
    public void parcels() {
        BillingAgreementRequest request = new BillingAgreementRequest();
        request.environment("test");
        request.clientId("client-id");
        request.pairingId(RuntimeEnvironment.application, "pairing-id");
        request.clientMetadataId("client-metadata-id");
        request.cancelUrl("com.braintreepayments.demo.braintree.cancel", "cancel");
        request.successUrl("com.braintreepayments.demo.braintree.success", "success");
        request.approvalURL("com.braintreepayments.demo.braintree.approval-url://?ba_token=TOKEN");

        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        CheckoutRequest parceledRequest = CheckoutRequest.CREATOR.createFromParcel(parcel);

        assertEquals("test", parceledRequest.getEnvironment());
        assertEquals("client-id", parceledRequest.getClientId());
        assertEquals("client-metadata-id", parceledRequest.getClientMetadataId());
        assertEquals("pairing-id", parceledRequest.getPairingId());
        assertEquals("com.braintreepayments.demo.braintree.cancel://onetouch/v1/cancel", parceledRequest.getCancelUrl());
        assertEquals("com.braintreepayments.demo.braintree.success://onetouch/v1/success", parceledRequest.getSuccessUrl());
        assertEquals("com.braintreepayments.demo.braintree.approval-url://?ba_token=TOKEN", parceledRequest.mApprovalUrl);
        assertEquals("ba_token", parceledRequest.mTokenQueryParamKey);
    }
}
