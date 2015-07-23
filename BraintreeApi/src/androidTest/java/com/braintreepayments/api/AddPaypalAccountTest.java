package com.braintreepayments.api;

import android.test.AndroidTestCase;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.models.PayPalAccount;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.json.JSONException;

public class AddPaypalAccountTest extends AndroidTestCase {

    private BraintreeApi mBraintreeApi;

    public void setUp() {
        TestUtils.setUp(getContext());
        mBraintreeApi = new BraintreeApi(getContext(), new TestClientTokenBuilder().withFakePayPal().build());
    }

    public void testCanAddPayPalAccount()
            throws ErrorWithResponse, BraintreeException, JSONException {

        PayPalAccount account = mBraintreeApi.create(TestUtils.fakePayPalAccountBuilder().validate(true));

        assertNotNull(account.getNonce());
    }

    public void testCanTokenizePayPalAccount()
            throws ErrorWithResponse, BraintreeException, JSONException {
        String nonce = mBraintreeApi.tokenize(TestUtils.fakePayPalAccountBuilder());

        assertNotNull(nonce);
    }
}
