package com.braintreepayments.api;

import static android.app.Activity.RESULT_OK;
import static com.braintreepayments.api.ThreeDSecureActivity.RESULT_COULD_NOT_START_CARDINAL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.os.Bundle;

import com.cardinalcommerce.cardinalmobilesdk.models.CardinalActionCode;
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalChallengeObserver;
import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureActivityUnitTest {

    @Test
    public void onCreate_withExtras_invokesCardinalWithLookupData()
            throws JSONException, BraintreeException {
        ThreeDSecurePaymentAuthRequest paymentAuthRequest =
                ThreeDSecurePaymentAuthRequest.fromJson(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE);

        Bundle extras = new Bundle();
        extras.putParcelable(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_RESULT, paymentAuthRequest);

        Intent intent = new Intent();
        intent.putExtras(extras);

        ThreeDSecureActivity sut = new ThreeDSecureActivity();
        sut.setIntent(intent);

        CardinalClient cardinalClient = mock(CardinalClient.class);
        sut.launchCardinalAuthChallenge(cardinalClient);

        ArgumentCaptor<ThreeDSecurePaymentAuthRequest> captor =
                ArgumentCaptor.forClass(ThreeDSecurePaymentAuthRequest.class);
        verify(cardinalClient).continueLookup(captor.capture(), any());

        ThreeDSecurePaymentAuthRequest actualResult = captor.getValue();
        ThreeDSecureLookup actualLookup = actualResult.getLookup();
        assertEquals("sample-transaction-id", actualLookup.getTransactionId());
        assertEquals("sample-pareq", actualLookup.getPareq());
    }

    @Test
    public void onCreate_withExtrasAndCardinalError_finishesWithError()
            throws JSONException, BraintreeException {
        ThreeDSecurePaymentAuthRequest paymentAuthRequest =
                ThreeDSecurePaymentAuthRequest.fromJson(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE);

        Bundle extras = new Bundle();
        extras.putParcelable(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_RESULT, paymentAuthRequest);

        Intent intent = new Intent();
        intent.putExtras(extras);

        ThreeDSecureActivity sut = spy(new ThreeDSecureActivity());
        sut.setIntent(intent);

        BraintreeException cardinalError = new BraintreeException("fake cardinal error");

        CardinalClient cardinalClient = mock(CardinalClient.class);
        doThrow(cardinalError).when(cardinalClient)
                .continueLookup(any(ThreeDSecurePaymentAuthRequest.class), any());

        sut.launchCardinalAuthChallenge(cardinalClient);
        verify(sut).finish();

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(sut).setResult(eq(RESULT_COULD_NOT_START_CARDINAL), captor.capture());

        Intent intentForResult = captor.getValue();
        assertEquals("fake cardinal error",
                intentForResult.getStringExtra(ThreeDSecureActivity.EXTRA_ERROR_MESSAGE));
    }

    @Test
    public void onCreate_withoutExtras_finishesWithError() throws BraintreeException {
        Intent intent = new Intent();
        ThreeDSecureActivity sut = spy(new ThreeDSecureActivity());
        sut.setIntent(intent);

        CardinalClient cardinalClient = mock(CardinalClient.class);
        sut.launchCardinalAuthChallenge(cardinalClient);

        verify(cardinalClient, never()).continueLookup(any(ThreeDSecurePaymentAuthRequest.class),
                any(CardinalChallengeObserver.class));
        verify(sut).finish();

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(sut).setResult(eq(RESULT_COULD_NOT_START_CARDINAL), captor.capture());

        Intent intentForResult = captor.getValue();
        assertEquals("Unable to launch 3DS authentication.",
                intentForResult.getStringExtra(ThreeDSecureActivity.EXTRA_ERROR_MESSAGE));
    }

    @Test
    public void onValidated_returnsValidationResults() throws JSONException {
        ThreeDSecurePaymentAuthRequest paymentAuthRequest =
                ThreeDSecurePaymentAuthRequest.fromJson(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE);

        Bundle extras = new Bundle();
        extras.putParcelable(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_RESULT, paymentAuthRequest);

        Intent intent = new Intent();
        intent.putExtras(extras);

        ThreeDSecureActivity sut = spy(new ThreeDSecureActivity());
        sut.setIntent(intent);

        ValidateResponse cardinalValidateResponse = mock(ValidateResponse.class);
        when(cardinalValidateResponse.getActionCode()).thenReturn(CardinalActionCode.SUCCESS);
        sut.onValidated(null, cardinalValidateResponse, "jwt");
        verify(sut).finish();

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(sut).setResult(eq(RESULT_OK), captor.capture());

        Intent intentForResult = captor.getValue();
        ValidateResponse activityResult = (ValidateResponse) (intentForResult.getSerializableExtra(
                ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE));

        assertEquals("jwt", intentForResult.getStringExtra(ThreeDSecureActivity.EXTRA_JWT));
        assertEquals(paymentAuthRequest, intentForResult.getParcelableExtra(
                ThreeDSecureActivity.EXTRA_THREE_D_SECURE_RESULT));
        assertNotNull(activityResult);
        assertEquals("SUCCESS", activityResult.getActionCode().getString());
    }
}
