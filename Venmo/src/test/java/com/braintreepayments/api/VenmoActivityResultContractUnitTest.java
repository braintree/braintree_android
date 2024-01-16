package com.braintreepayments.api;

import static com.braintreepayments.api.VenmoActivityResultContract.EXTRA_PAYMENT_METHOD_NONCE;
import static com.braintreepayments.api.VenmoActivityResultContract.EXTRA_USERNAME;
import static com.braintreepayments.api.VenmoActivityResultContract.META_KEY;
import static com.braintreepayments.api.VenmoClient.EXTRA_ACCESS_TOKEN;
import static com.braintreepayments.api.VenmoClient.EXTRA_BRAINTREE_DATA;
import static com.braintreepayments.api.VenmoClient.EXTRA_ENVIRONMENT;
import static com.braintreepayments.api.VenmoClient.EXTRA_MERCHANT_ID;
import static com.braintreepayments.api.VenmoClient.EXTRA_RESOURCE_ID;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

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
    public void createIntentForVenmoIntent_returnsIntentWithExtras() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO);
        VenmoIntentData input = new VenmoIntentData(configuration, "sample-venmo-merchant", "venmo-payment-context-id", "session-id" , "custom");
        VenmoActivityResultContract sut = new VenmoActivityResultContract();
        sut.fallbackToWeb = false;

        Intent intent = sut.createIntent(context, input);

        assertEquals(new ComponentName("com.venmo", "com.venmo.controller.SetupMerchantActivity"), intent.getComponent());
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
        JSONAssert.assertEquals(expectedBraintreeData, new JSONObject(intent.getStringExtra(EXTRA_BRAINTREE_DATA)), JSONCompareMode.STRICT);
    }

    @Test
    public void createIntentForFallbackToWeb_returnsIntentWithExtras() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO);
        VenmoIntentData input = new VenmoIntentData(configuration, "sample-venmo-merchant", "venmo-payment-context-id", "session-id" , "custom");
        VenmoActivityResultContract sut = new VenmoActivityResultContract();
        sut.fallbackToWeb = true;
        sut.venmoAppInstalled = false;

        JSONObject braintreeData = new JSONObject();
        JSONObject meta = new MetadataBuilder()
            .sessionId(input.getSessionId())
            .integration(input.getIntegrationType())
            .version()
            .build();
        braintreeData.put(META_KEY, meta);

        Uri expectedUri = Uri.parse("https://venmo.com/go/checkout")
            .buildUpon()
            .appendQueryParameter("x-success", "com.braintreepayments.api.venmo.test://x-callback-url/vzero/auth/venmo/success")
            .appendQueryParameter("x-error", "com.braintreepayments.api.venmo.test://x-callback-url/vzero/auth/venmo/error")
            .appendQueryParameter("x-cancel", "com.braintreepayments.api.venmo.test://x-callback-url/vzero/auth/venmo/cancel")
            .appendQueryParameter("x-source", "com.braintreepayments.api.venmo.test")
            .appendQueryParameter("braintree_merchant_id", input.getProfileId())
            .appendQueryParameter("braintree_access_token", input.getConfiguration().getVenmoAccessToken())
            .appendQueryParameter("braintree_environment", input.getConfiguration().getVenmoEnvironment())
            .appendQueryParameter("resource_id", input.getPaymentContextId())
            .appendQueryParameter("braintree_sdk_data", braintreeData.toString())
            .appendQueryParameter("customerClient", "MOBILE_APP")
            .build();

        Intent intent = sut.createIntent(context, input);

        assertEquals(Intent.ACTION_VIEW, intent.getAction());
        assertEquals(expectedUri, intent.getData());
    }

    @Test
    public void createIntentForFallbackToWebWithVenmoInstalled_returnsIntentWithExtras() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO);
        VenmoIntentData input = new VenmoIntentData(configuration, "sample-venmo-merchant", "venmo-payment-context-id", "session-id" , "custom");
        VenmoActivityResultContract sut = new VenmoActivityResultContract();
        sut.fallbackToWeb = true;
        sut.venmoAppInstalled = true;

        Intent intent = sut.createIntent(context, input);

        assertNull(intent.getAction());
    }

    @Test
    public void parseResult_whenResultIsOK_andPaymentContextIDExists_returnsVenmoResultWithNonce() {
        VenmoActivityResultContract sut = new VenmoActivityResultContract();

        Intent successIntent = new Intent();
        successIntent.putExtra(EXTRA_RESOURCE_ID, "resource_id");
        successIntent.putExtra(EXTRA_PAYMENT_METHOD_NONCE, "payment_method_nonce");
        successIntent.putExtra(EXTRA_USERNAME, "username");

        VenmoResult venmoResult = sut.parseResult(Activity.RESULT_OK, successIntent);
        assertNotNull(venmoResult);
        assertEquals("resource_id", venmoResult.getPaymentContextId());
        assertEquals("payment_method_nonce", venmoResult.getVenmoAccountNonce());
        assertEquals("username", venmoResult.getVenmoUsername());
    }

    @Test
    public void parseResult_whenResultIsCANCELED_returnsVenomResultWithError() {
        VenmoActivityResultContract sut = new VenmoActivityResultContract();

        VenmoResult venmoResult = sut.parseResult(Activity.RESULT_CANCELED, null);
        assertNotNull(venmoResult);

        UserCanceledException error = (UserCanceledException) venmoResult.getError();
        assertNotNull("User canceled Venmo.", error.getMessage());
        assertFalse(error.isExplicitCancelation());
    }
}
