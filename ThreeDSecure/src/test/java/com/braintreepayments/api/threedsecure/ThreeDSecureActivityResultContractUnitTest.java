package com.braintreepayments.api.threedsecure;

import static com.braintreepayments.api.threedsecure.ThreeDSecureActivity.EXTRA_JWT;
import static com.braintreepayments.api.threedsecure.ThreeDSecureActivity.EXTRA_THREE_D_SECURE_RESULT;
import static com.braintreepayments.api.threedsecure.ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import com.braintreepayments.api.testutils.Fixtures;
import com.braintreepayments.api.core.BraintreeException;
import com.braintreepayments.api.core.UserCanceledException;
import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureActivityResultContractUnitTest {

    private Context context;
    private ThreeDSecureParams threeDSecureParams;

    private ThreeDSecureActivityResultContract sut;

    @Before
    public void beforeEach() throws JSONException {
        context = ApplicationProvider.getApplicationContext();
        threeDSecureParams = ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE);
    }

    @Test
    public void createIntent_returnsIntentWithExtras() {
        sut = new ThreeDSecureActivityResultContract();
        Intent result = sut.createIntent(context, threeDSecureParams);

        ThreeDSecureParams extraThreeDSecureParams =
            result.getParcelableExtra(EXTRA_THREE_D_SECURE_RESULT);
        assertSame(threeDSecureParams, extraThreeDSecureParams);
    }

    @Test
    public void parseResult_whenResultIsOK_returnsCardinalResultWithSuccessData() {
        sut = new ThreeDSecureActivityResultContract();

        Intent successIntent = new Intent();
        successIntent.putExtra(EXTRA_THREE_D_SECURE_RESULT, threeDSecureParams);

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        successIntent.putExtra(EXTRA_VALIDATION_RESPONSE, validateResponse);

        String jwt = "sample-jwt";
        successIntent.putExtra(EXTRA_JWT, jwt);

        ThreeDSecurePaymentAuthResult
                paymentAuthResult = sut.parseResult(Activity.RESULT_OK, successIntent);
        assertNotNull(paymentAuthResult);

        assertSame(threeDSecureParams, paymentAuthResult.getThreeDSecureParams());
        assertSame(validateResponse, paymentAuthResult.getValidateResponse());
        assertSame(jwt, paymentAuthResult.getJwt());
    }

    @Test
    public void parseResult_whenResultIsOKAndIntentIsNull_returnsCardinalResultWithError() {
        sut = new ThreeDSecureActivityResultContract();

        ThreeDSecurePaymentAuthResult paymentAuthResult = sut.parseResult(Activity.RESULT_OK, null);
        assertNotNull(paymentAuthResult);

        BraintreeException error = (BraintreeException) paymentAuthResult.getError();
        String expectedMessage = "An unknown Android error occurred with the activity result API.";
        assertNotNull(expectedMessage, error.getMessage());
    }

    @Test
    public void parseResult_whenResultIsCANCELED_returnsCardinalResultWithError() {
        sut = new ThreeDSecureActivityResultContract();

        ThreeDSecurePaymentAuthResult
                paymentAuthResult = sut.parseResult(Activity.RESULT_CANCELED, new Intent());
        assertNotNull(paymentAuthResult);

        UserCanceledException error = (UserCanceledException) paymentAuthResult.getError();
        assertEquals("User canceled 3DS.", error.getMessage());
    }

    @Test
    public void parseResult_whenResultIsCANCELEDAndHasErrorMessage_returnsCardinalResultWithError() {
        sut = new ThreeDSecureActivityResultContract();

        Intent intent = new Intent();
        intent.putExtra(ThreeDSecureActivity.EXTRA_ERROR_MESSAGE, "sample error message");

        ThreeDSecurePaymentAuthResult paymentAuthResult =
            sut.parseResult(ThreeDSecureActivity.RESULT_COULD_NOT_START_CARDINAL, intent);
        assertNotNull(paymentAuthResult);

        BraintreeException error = (BraintreeException) paymentAuthResult.getError();
        assertEquals("sample error message", error.getMessage());
    }
}
