package com.braintreepayments.api;

import static com.braintreepayments.api.VenmoClient.EXTRA_ACCESS_TOKEN;
import static com.braintreepayments.api.VenmoClient.EXTRA_BRAINTREE_DATA;
import static com.braintreepayments.api.VenmoClient.EXTRA_ENVIRONMENT;
import static com.braintreepayments.api.VenmoClient.EXTRA_MERCHANT_ID;
import static com.braintreepayments.api.VenmoClient.EXTRA_RESOURCE_ID;
import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNotNull;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
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
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO);
        VenmoIntentData input = new VenmoIntentData(configuration, "sample-venmo-merchant", "venmo-payment-context-id", "session-id" , "custom");
        VenmoActivityResultContract sut = new VenmoActivityResultContract();

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
    public void parseResult_whenResultIsOK_returnsVenmoResultWithNonce() {
        VenmoActivityResultContract sut = new VenmoActivityResultContract();

        Intent successIntent = new Intent();
        // TODO: fill in intent details

        VenmoResult venmoResult = sut.parseResult(Activity.RESULT_OK, successIntent);
        assertNotNull(venmoResult);

    }

    @Test
    public void parseResult_whenResultIsOKAndIntentIsNull_returnsVenmoResultWithError() {
        VenmoActivityResultContract sut = new VenmoActivityResultContract();

        VenmoResult venmoResult = sut.parseResult(Activity.RESULT_OK, null);
        assertNotNull(venmoResult);

        BraintreeException error = (BraintreeException) venmoResult.getError();
        String expectedMessage = "An unknown Android error occurred with the activity result API.";
        assertNotNull(expectedMessage, error.getMessage());
    }

    @Test
    public void parseResult_whenResultIsCANCELED_returnsVenomResultWithError() {
        VenmoActivityResultContract sut = new VenmoActivityResultContract();

        VenmoResult venmoResult = sut.parseResult(Activity.RESULT_CANCELED, new Intent());
        assertNotNull(venmoResult);

        UserCanceledException error = (UserCanceledException) venmoResult.getError();
        assertNotNull("User canceled Venmo.", error.getMessage());
    }
}
