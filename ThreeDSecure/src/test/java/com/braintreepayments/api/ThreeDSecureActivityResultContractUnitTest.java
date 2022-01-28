package com.braintreepayments.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureActivityResultContractUnitTest {

    private Context context;
    private ThreeDSecureResult threeDSecureResult;

    private ThreeDSecureActivityResultContract sut;

    @Before
    public void beforeEach() throws JSONException {
        context = ApplicationProvider.getApplicationContext();
        threeDSecureResult = ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE);
    }

    @Test
    public void createIntent_returnsIntentWithExtras() {
        sut = new ThreeDSecureActivityResultContract();
        Intent result = sut.createIntent(context, threeDSecureResult);

        ThreeDSecureResult extraThreeDSecureResult =
            result.getParcelableExtra(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_RESULT);
        assertNotNull(extraThreeDSecureResult);

        CardNonce cardNonce = extraThreeDSecureResult.getTokenizedCard();
        assertNotNull(cardNonce);
        assertEquals("123456-12345-12345-a-adfa", cardNonce.getString());
    }

    @Test
    public void parseResult() {
        sut = new ThreeDSecureActivityResultContract();
    }
}