package com.braintreepayments.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse;

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
        assertSame(threeDSecureResult, extraThreeDSecureResult);
    }

    @Test
    public void parseResult_whenResultIsOK_returnsCardinalResultWithSuccessData() {
        sut = new ThreeDSecureActivityResultContract();

        Intent successIntent = new Intent();
        successIntent.putExtra(
                ThreeDSecureActivity.EXTRA_THREE_D_SECURE_RESULT, threeDSecureResult);

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        successIntent.putExtra(ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE, validateResponse);

        String jwt = "sample-jwt";
        successIntent.putExtra(ThreeDSecureActivity.EXTRA_JWT, jwt);

        CardinalResult cardinalResult = sut.parseResult(Activity.RESULT_OK, successIntent);
        assertNotNull(cardinalResult);

        assertSame(threeDSecureResult, cardinalResult.getThreeSecureResult());
        assertSame(validateResponse, cardinalResult.getValidateResponse());
        assertSame(jwt, cardinalResult.getJWT());
    }

    @Test
    public void parseResult_whenResultIsCANCELED_returnsCardinalResultWithError() {
        sut = new ThreeDSecureActivityResultContract();

        CardinalResult cardinalResult = sut.parseResult(Activity.RESULT_CANCELED, new Intent());
        assertNotNull(cardinalResult);

        UserCanceledException error = (UserCanceledException) cardinalResult.getError();
        assertNotNull("User canceled 3DS.", error.getMessage());
    }
}