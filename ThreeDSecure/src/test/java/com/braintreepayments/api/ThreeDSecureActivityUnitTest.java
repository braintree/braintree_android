package com.braintreepayments.api;

import android.content.Intent;
import android.os.Bundle;

import com.braintreepayments.api.models.ThreeDSecureLookup;
import com.braintreepayments.testutils.Fixtures;
import com.cardinalcommerce.cardinalmobilesdk.a.a.c;
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalActionCode;
import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "androidx.*", "org.json.*", "javax.crypto.*" })
@PrepareForTest({ CardinalClient.class })
public class ThreeDSecureActivityUnitTest {

    private static final c CARDINAL_ERROR = new c(0, "");

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Before
    public void beforeEach() {
        mockStatic(CardinalClient.class);
    }

    @Test
    public void onCreate_invokesCardinalWithLookupData() {
        CardinalClient cardinalClient = mock(CardinalClient.class);
        when(CardinalClient.newInstance()).thenReturn(cardinalClient);

        ThreeDSecureLookup threeDSecureLookup = sampleThreeDSecureLookup();

        Bundle extras = new Bundle();
        extras.putParcelable(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_LOOKUP, threeDSecureLookup);

        Intent intent = new Intent();
        intent.putExtras(extras);

        ActivityController<ThreeDSecureActivity> activityController =
            Robolectric.buildActivity(ThreeDSecureActivity.class, intent).create();
        ThreeDSecureActivity activity = activityController.get();

        ArgumentCaptor<ThreeDSecureLookup> captor = ArgumentCaptor.forClass(ThreeDSecureLookup.class);
        verify(cardinalClient).continueLookup(same(activity), captor.capture(), same(activity));

        ThreeDSecureLookup actualLookup = captor.getValue();
        assertEquals("sample-transaction-id", actualLookup.getTransactionId());
        assertEquals("sample-pareq", actualLookup.getPareq());
    }

    @Test
    public void onValidated_returnsValidationResults() {
        CardinalClient cardinalClient = mock(CardinalClient.class);
        when(CardinalClient.newInstance()).thenReturn(cardinalClient);

        ThreeDSecureLookup threeDSecureLookup = sampleThreeDSecureLookup();
        Bundle extras = new Bundle();
        extras.putParcelable(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_LOOKUP, threeDSecureLookup);

        Intent intent = new Intent();
        intent.putExtras(extras);

        ThreeDSecureActivity activity = Robolectric.buildActivity(ThreeDSecureActivity.class, intent)
                .create().get();

        ValidateResponse cardinalValidateResponse = new ValidateResponse( false, CardinalActionCode.SUCCESS, CARDINAL_ERROR);
        activity.onValidated(null, cardinalValidateResponse, "jwt");

        Intent intentForResult = shadowOf(activity).getResultIntent();
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
