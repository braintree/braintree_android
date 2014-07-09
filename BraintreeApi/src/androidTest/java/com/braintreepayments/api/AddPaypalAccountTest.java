package com.braintreepayments.api;

import android.test.AndroidTestCase;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.models.PayPalAccount;
import com.braintreepayments.api.models.PayPalAccountBuilder;

public class AddPaypalAccountTest extends AndroidTestCase {

    private BraintreeApi mBraintreeApi;

    public void setUp() {
        TestUtils.setUp(getContext());
        mBraintreeApi = new BraintreeApi(getContext(), new TestClientTokenBuilder().withFakePayPal().build());
    }

    public void testCanAddPayPalAccount() throws ErrorWithResponse, BraintreeException {
        PayPalAccountBuilder builder = new PayPalAccountBuilder()
                .authorizationCode("test-authorization-code");

        PayPalAccount account = mBraintreeApi.create(builder);

        assertNotNull(account.getNonce());
    }

    public void testCanTokenizePayPalAccount() throws ErrorWithResponse, BraintreeException {
        PayPalAccountBuilder builder = new PayPalAccountBuilder();

        String nonce = mBraintreeApi.tokenize(builder);

        assertNotNull(nonce);
    }
}
