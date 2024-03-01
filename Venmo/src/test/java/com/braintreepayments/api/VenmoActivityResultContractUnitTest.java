package com.braintreepayments.api;

import static com.braintreepayments.api.VenmoActivityResultContract.EXTRA_PAYMENT_METHOD_NONCE;
import static com.braintreepayments.api.VenmoActivityResultContract.EXTRA_USERNAME;
import static com.braintreepayments.api.VenmoActivityResultContract.EXTRA_ACCESS_TOKEN;
import static com.braintreepayments.api.VenmoActivityResultContract.EXTRA_BRAINTREE_DATA;
import static com.braintreepayments.api.VenmoActivityResultContract.EXTRA_ENVIRONMENT;
import static com.braintreepayments.api.VenmoActivityResultContract.EXTRA_MERCHANT_ID;
import static com.braintreepayments.api.VenmoActivityResultContract.EXTRA_RESOURCE_ID;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

@RunWith(RobolectricTestRunner.class)
public class VenmoActivityResultContractUnitTest {

    private Context context;

    @Before
    public void beforeEach() {
        context = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void createIntent_returnsIntentWithExtras() throws JSONException {
        Configuration configuration =
                Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO);
        VenmoPaymentAuthRequestParams
                input = new VenmoPaymentAuthRequestParams(configuration, "sample-venmo-merchant",
                "venmo-payment-context-id", "session-id", "custom");
        VenmoActivityResultContract sut = new VenmoActivityResultContract();

        Intent intent = sut.createIntent(context, input);

        assertEquals(new ComponentName("com.venmo", "com.venmo.controller.SetupMerchantActivity"),
                intent.getComponent());
        assertEquals("sample-venmo-merchant", intent.getStringExtra(EXTRA_MERCHANT_ID));
        assertEquals("access-token", intent.getStringExtra(EXTRA_ACCESS_TOKEN));
        assertEquals("environment", intent.getStringExtra(EXTRA_ENVIRONMENT));
        assertEquals("venmo-payment-context-id", intent.getStringExtra(EXTRA_RESOURCE_ID));

        JSONObject expectedBraintreeData = new JSONObject()
                .put("_meta", new JSONObject()
                        .put("platform", "android")
                        .put("sessionId", "session-id")
                        .put("integration", "custom")
                        .put("version", BuildConfig.VERSION_NAME)
                );
        JSONAssert.assertEquals(expectedBraintreeData,
                new JSONObject(intent.getStringExtra(EXTRA_BRAINTREE_DATA)),
                JSONCompareMode.STRICT);
    }

    @Test
    public void parseResult_whenResultIsOK_andPaymentContextIDExists_returnsVenmoResultWithNonce() {
        VenmoActivityResultContract sut = new VenmoActivityResultContract();

        Intent successIntent = new Intent();
        successIntent.putExtra(EXTRA_RESOURCE_ID, "resource_id");
        successIntent.putExtra(EXTRA_PAYMENT_METHOD_NONCE, "payment_method_nonce");
        successIntent.putExtra(EXTRA_USERNAME, "username");

        VenmoPaymentAuthResult venmoPaymentAuthResult =
                sut.parseResult(Activity.RESULT_OK, successIntent);
        assertNotNull(venmoPaymentAuthResult);
        assertEquals("resource_id", venmoPaymentAuthResult.getPaymentContextId());
        assertEquals("payment_method_nonce", venmoPaymentAuthResult.getVenmoAccountNonce());
        assertEquals("username", venmoPaymentAuthResult.getVenmoUsername());
    }

    @Test
    public void parseResult_whenResultIsCANCELED_returnsVenomResultWithError() {
        VenmoActivityResultContract sut = new VenmoActivityResultContract();

        VenmoPaymentAuthResult venmoPaymentAuthResult =
                sut.parseResult(Activity.RESULT_CANCELED, null);
        assertNotNull(venmoPaymentAuthResult);

        UserCanceledException error = (UserCanceledException) venmoPaymentAuthResult.getError();
        assertNotNull("User canceled Venmo.", error.getMessage());
        assertFalse(error.isExplicitCancelation());
    }
}
