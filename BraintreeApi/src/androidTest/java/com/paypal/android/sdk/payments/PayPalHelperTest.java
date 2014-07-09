package com.paypal.android.sdk.payments;

import android.app.Activity;
import android.content.Intent;
import android.test.AndroidTestCase;

import com.braintreepayments.api.PayPalHelper;
import com.braintreepayments.api.Utils;
import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.models.PayPalAccount;
import com.braintreepayments.api.models.PayPalAccountBuilder;

public class PayPalHelperTest extends AndroidTestCase {

    public void testAdapterProxiesAuthorizationCode() throws ConfigurationException {
        Intent successfulPaypal = new Intent();
        PayPalAuthorization successfulAuthorization = new PayPalAuthorization(PayPalConfiguration.ENVIRONMENT_NO_NETWORK, "fake_paypal_authorization_code", "");
        successfulPaypal.putExtra(PayPalFuturePaymentActivity.EXTRA_RESULT_AUTHORIZATION, successfulAuthorization);

        PayPalAccountBuilder accountBuilder = PayPalHelper.getBuilderFromActivity(
                Activity.RESULT_OK, successfulPaypal);
        String json = Utils.getGson().toJson(accountBuilder.build());
        assertTrue(json.contains("fake_paypal_authorization_code"));
    }

    public void testGetBuilderFromActivityDoesNotIncludeEmailIfItWasNotPresent()
            throws ConfigurationException {
        Intent successfulPaypal = new Intent();
        PayPalAuthorization successfulAuthorization = new PayPalAuthorization(PayPalConfiguration.ENVIRONMENT_NO_NETWORK, "fake_paypal_authorization_code", "");
        successfulPaypal.putExtra(PayPalFuturePaymentActivity.EXTRA_RESULT_AUTHORIZATION, successfulAuthorization);

        PayPalAccountBuilder accountBuilder = PayPalHelper.getBuilderFromActivity(
                Activity.RESULT_OK, successfulPaypal);
        PayPalAccount account = accountBuilder.build();
        assertEquals("", account.getEmail() );
    }
}
