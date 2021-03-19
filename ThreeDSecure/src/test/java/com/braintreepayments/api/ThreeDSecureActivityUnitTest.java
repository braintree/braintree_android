package com.braintreepayments.api;

import android.content.Intent;
import android.os.Bundle;

import com.cardinalcommerce.cardinalmobilesdk.a.a.c;
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalActionCode;
import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

import static android.app.Activity.RESULT_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureActivityUnitTest {

    private static final c CARDINAL_ERROR = new c(0, "");

    @Test
    public void onCreate_invokesCardinalWithLookupData() {
        ThreeDSecureResult threeDSecureResult = sampleThreeDSecureResult();

        Bundle extras = new Bundle();
        extras.putParcelable(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_RESULT, threeDSecureResult);

        Intent intent = new Intent();
        intent.putExtras(extras);

        ThreeDSecureActivity sut = new ThreeDSecureActivity();
        sut.setIntent(intent);

        CardinalClient cardinalClient = mock(CardinalClient.class);
        sut.onCreateInternal(cardinalClient);

        ArgumentCaptor<ThreeDSecureResult> captor = ArgumentCaptor.forClass(ThreeDSecureResult.class);
        verify(cardinalClient).continueLookup(same(sut), captor.capture(), same(sut));

        ThreeDSecureResult actualResult = captor.getValue();
        ThreeDSecureLookup actualLookup = actualResult.getLookup();
        assertEquals("sample-transaction-id", actualLookup.getTransactionId());
        assertEquals("sample-pareq", actualLookup.getPareq());
    }

    @Test
    public void onValidated_returnsValidationResults() {
        ThreeDSecureResult threeDSecureResult = sampleThreeDSecureResult();

        Bundle extras = new Bundle();
        extras.putParcelable(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_RESULT, threeDSecureResult);

        Intent intent = new Intent();
        intent.putExtras(extras);

        ThreeDSecureActivity sut = spy(new ThreeDSecureActivity());
        sut.setIntent(intent);

        ValidateResponse cardinalValidateResponse = new ValidateResponse( false, CardinalActionCode.SUCCESS, CARDINAL_ERROR);
        sut.onValidated(null, cardinalValidateResponse, "jwt");
        verify(sut).finish();

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(sut).setResult(eq(RESULT_OK), captor.capture());

        Intent intentForResult = captor.getValue();
        ValidateResponse activityResult = (ValidateResponse)(intentForResult.getSerializableExtra(ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE));

        assertEquals("jwt", intentForResult.getStringExtra(ThreeDSecureActivity.EXTRA_JWT));
        assertEquals(threeDSecureResult, intentForResult.getParcelableExtra(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_RESULT));
        assertNotNull(activityResult);
        assertEquals("SUCCESS", activityResult.getActionCode().getString());
    }

    private ThreeDSecureResult sampleThreeDSecureResult() {
        return ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE);
    }
}
