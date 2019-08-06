package com.braintreepayments.api;

import android.content.Intent;
import android.os.Bundle;

import com.braintreepayments.api.models.ThreeDSecureLookup;
import com.cardinalcommerce.cardinalmobilesdk.Cardinal;
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalActionCode;
import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse;

import org.json.JSONException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "androidx.*", "org.json.*", "javax.crypto.*" })
@PrepareForTest({ Cardinal.class })
public class ThreeDSecureActivityUnitTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Test
    public void onCreate_invokesCardinalWithLookupData() {
        Cardinal cardinal = BraintreePowerMockHelper.MockStaticCardinal
                .cca_continue(CardinalActionCode.SUCCESS);

        ThreeDSecureLookup threeDSecureLookup = sampleThreeDSecureLookup();

        Bundle extras = new Bundle();
        extras.putParcelable(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_LOOKUP, threeDSecureLookup);

        Intent intent = new Intent();
        intent.putExtras(extras);

        ActivityController<ThreeDSecureActivity> activityController = Robolectric.buildActivity(ThreeDSecureActivity.class, intent)
                .create();

        verify(cardinal).cca_continue(
                eq(threeDSecureLookup.getTransactionId()),
                eq(threeDSecureLookup.getPareq()),
                eq(activityController.get()),
                eq(activityController.get())
        );
    }

    @Test
    public void onValidated_returnsValidationResults() {
        BraintreePowerMockHelper.MockStaticCardinal.cca_continue(CardinalActionCode.SUCCESS);
        ThreeDSecureLookup threeDSecureLookup = sampleThreeDSecureLookup();

        Bundle extras = new Bundle();
        extras.putParcelable(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_LOOKUP, threeDSecureLookup);

        Intent intent = new Intent();
        intent.putExtras(extras);

        ThreeDSecureActivity activity = Robolectric.buildActivity(ThreeDSecureActivity.class, intent)
                .create().get();

        Intent intentForResult = shadowOf(activity).getResultIntent();
        ValidateResponse validateResponse = (ValidateResponse)(intentForResult.getSerializableExtra(ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE));

        assertEquals("jwt", intentForResult.getStringExtra(ThreeDSecureActivity.EXTRA_JWT));
        assertEquals(threeDSecureLookup, intentForResult.getParcelableExtra(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_LOOKUP));
        assertNotNull(validateResponse);
        assertEquals("SUCCESS", validateResponse.getActionCode().getString());
    }

    private ThreeDSecureLookup sampleThreeDSecureLookup() {
        try {
            return ThreeDSecureLookup.fromJson(
                    stringFromFixture("three_d_secure/lookup_response.json"));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

}
