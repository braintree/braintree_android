package com.braintreepayments.api;

import android.content.Intent;
import android.os.Bundle;

import com.cardinalcommerce.cardinalmobilesdk.a.a.c;
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalActionCode;
import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse;

import org.json.JSONException;
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
        ThreeDSecureLookup threeDSecureLookup = sampleThreeDSecureLookup();

        Bundle extras = new Bundle();
        extras.putParcelable(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_LOOKUP, threeDSecureLookup);

        Intent intent = new Intent();
        intent.putExtras(extras);

        ThreeDSecureActivity sut = new ThreeDSecureActivity();
        sut.setIntent(intent);

        CardinalClient cardinalClient = mock(CardinalClient.class);
        sut.onCreateInternal(cardinalClient);

        ArgumentCaptor<ThreeDSecureLookup> captor = ArgumentCaptor.forClass(ThreeDSecureLookup.class);
        verify(cardinalClient).continueLookup(same(sut), captor.capture(), same(sut));

        ThreeDSecureLookup actualLookup = captor.getValue();
        assertEquals("sample-transaction-id", actualLookup.getTransactionId());
        assertEquals("sample-pareq", actualLookup.getPareq());
    }

    @Test
    public void onValidated_returnsValidationResults() {
        ThreeDSecureLookup threeDSecureLookup = sampleThreeDSecureLookup();
        Bundle extras = new Bundle();
        extras.putParcelable(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_LOOKUP, threeDSecureLookup);

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
        assertEquals(threeDSecureLookup, intentForResult.getParcelableExtra(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_LOOKUP));
        assertNotNull(activityResult);
        assertEquals("SUCCESS", activityResult.getActionCode().getString());
    }

    private ThreeDSecureLookup sampleThreeDSecureLookup() {
        try {
            return ThreeDSecureLookup.fromJson(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
