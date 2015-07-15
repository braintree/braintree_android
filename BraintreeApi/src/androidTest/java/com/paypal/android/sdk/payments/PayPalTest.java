package com.paypal.android.sdk.payments;

import android.app.Activity;
import android.content.Intent;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import com.braintreepayments.api.PayPal;
import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.models.PayPalAccountBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class PayPalTest {

    @Test(timeout = 1000)
    @SmallTest
    public void adapterProxiesAuthorizationCode() throws ConfigurationException {
        Intent successfulPaypal = new Intent();
        PayPalAuthorization successfulAuthorization = new PayPalAuthorization(PayPalConfiguration.ENVIRONMENT_NO_NETWORK, "fake_paypal_authorization_code", "");
        successfulPaypal.putExtra(PayPalFuturePaymentActivity.EXTRA_RESULT_AUTHORIZATION, successfulAuthorization);

        PayPalAccountBuilder accountBuilder = PayPal.getBuilderFromActivity(null,
                Activity.RESULT_OK, successfulPaypal);
        String json = accountBuilder.build();

        assertTrue(json.contains("fake_paypal_authorization_code"));
    }
}
