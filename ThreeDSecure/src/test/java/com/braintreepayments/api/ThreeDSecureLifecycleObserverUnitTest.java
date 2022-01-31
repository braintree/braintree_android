package com.braintreepayments.api;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultRegistry;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.FragmentActivity;

import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureLifecycleObserverUnitTest {

    static class MockActivityResultRegistry extends ActivityResultRegistry {

        private final CardinalResult result;

        MockActivityResultRegistry(CardinalResult result) {
            this.result = result;
        }

        @Override
        public <I, O> void onLaunch(int requestCode, @NonNull ActivityResultContract<I, O> contract, I input, @Nullable ActivityOptionsCompat options) {
            dispatchResult(requestCode, result);
        }
    }

    @Test
    public void onCreate_registersForAnActivityResult() {
        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);

        ThreeDSecureClient threeDSecureClient = mock(ThreeDSecureClient.class);
        ThreeDSecureLifecycleObserver sut = new ThreeDSecureLifecycleObserver(activityResultRegistry, threeDSecureClient);

        FragmentActivity lifecycleOwner = new FragmentActivity();
        sut.onCreate(lifecycleOwner);

        String expectedKey = "com.braintreepayments.api.ThreeDSecure.RESULT";
        verify(activityResultRegistry).register(eq(expectedKey), same(lifecycleOwner), any(ThreeDSecureActivityResultContract.class), Mockito.<ActivityResultCallback<CardinalResult>>any());
    }

    @Test
    public void launch_launchesThreeDSecureActivityAndForwardsSuccessToThreeDSecureClient() throws JSONException {
        // TODO: this test is failing; consider using a fragment launcher to test full lifecycle flow
        ThreeDSecureResult threeDSecureResult =
                ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE);
        ValidateResponse validateResponse = mock(ValidateResponse.class);

        CardinalResult cardinalResult =
                new CardinalResult(threeDSecureResult, "sample jwt", validateResponse);

        MockActivityResultRegistry activityResultRegistry =
                new MockActivityResultRegistry(cardinalResult);

        ThreeDSecureClient threeDSecureClient = mock(ThreeDSecureClient.class);
        ThreeDSecureLifecycleObserver sut = new ThreeDSecureLifecycleObserver(activityResultRegistry, threeDSecureClient);

        FragmentActivity lifecycleOwner = new FragmentActivity();
        sut.onCreate(lifecycleOwner);

        sut.launch(threeDSecureResult);
        verify(threeDSecureClient).onCardinalResult(cardinalResult);
    }
}