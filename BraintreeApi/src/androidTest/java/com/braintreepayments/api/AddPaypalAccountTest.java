package com.braintreepayments.api;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.models.PayPalAccount;
import com.braintreepayments.api.models.PayPalAccountBuilder;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class AddPaypalAccountTest {

    @Test(timeout = 1000)
    @SmallTest
    public void create_AcceptsPayPalAccount() throws ErrorWithResponse, BraintreeException,
            JSONException {
        TestUtils.setUp(getTargetContext());
        BraintreeApi mBraintreeApi = new BraintreeApi(getTargetContext(),
                new TestClientTokenBuilder().withPayPal().build());
        PayPalAccountBuilder builder = new PayPalAccountBuilder()
                .consentCode("test-authorization-code");

        PayPalAccount account = mBraintreeApi.create(builder);

        assertNotNull(account.getNonce());
    }
}
