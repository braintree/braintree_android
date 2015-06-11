package com.paypal.android.sdk.payments;

import android.app.Activity;
import android.content.Intent;
import android.test.AndroidTestCase;

import com.braintreepayments.api.PayPal;
import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.models.PayPalAccount;
import com.braintreepayments.api.models.PayPalAccountBuilder;
import com.google.gson.Gson;

public class PayPalTest extends AndroidTestCase {

    public void testAdapterProxiesAuthorizationCode() throws ConfigurationException {
        Intent successfulPaypal = new Intent();
        PayPalAuthorization successfulAuthorization = new PayPalAuthorization(PayPalConfiguration.ENVIRONMENT_NO_NETWORK, "fake_paypal_authorization_code", "");
        successfulPaypal.putExtra(PayPalFuturePaymentActivity.EXTRA_RESULT_AUTHORIZATION, successfulAuthorization);

        PayPalAccountBuilder accountBuilder = PayPal.getBuilderFromActivity(null,
                Activity.RESULT_OK, successfulPaypal);
        String json = new Gson().toJson(accountBuilder.build());
        assertTrue(json.contains("fake_paypal_authorization_code"));
    }

    public void testGetBuilderFromActivityDoesNotIncludeEmailIfItWasNotPresent()
            throws ConfigurationException {
        Intent successfulPaypal = new Intent();
        PayPalAuthorization successfulAuthorization = new PayPalAuthorization(PayPalConfiguration.ENVIRONMENT_NO_NETWORK, "fake_paypal_authorization_code", "");
        successfulPaypal.putExtra(PayPalFuturePaymentActivity.EXTRA_RESULT_AUTHORIZATION, successfulAuthorization);

        PayPalAccountBuilder accountBuilder = PayPal.getBuilderFromActivity(null,
                Activity.RESULT_OK, successfulPaypal);
        PayPalAccount account = accountBuilder.build();
        assertEquals("", account.getEmail() );
    }
}
