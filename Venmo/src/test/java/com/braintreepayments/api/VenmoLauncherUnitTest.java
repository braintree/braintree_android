package com.braintreepayments.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.fragment.app.FragmentActivity;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class VenmoLauncherUnitTest {

    @Mock
    ActivityResultLauncher<VenmoAuthChallenge> activityResultLauncher;
    private VenmoAuthChallengeResultCallback callback;

    @Before
    public void beforeEach()  {
        MockitoAnnotations.openMocks(this);
        callback = mock(VenmoAuthChallengeResultCallback.class);
    }
    
    @Test
    public void constructor_createsActivityLauncher() {
        String expectedKey = "com.braintreepayments.api.Venmo.RESULT";
        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);
        FragmentActivity lifecycleOwner = new FragmentActivity();

        VenmoLauncher sut = new VenmoLauncher(activityResultRegistry, lifecycleOwner, callback);

        verify(activityResultRegistry).register(eq(expectedKey), same(lifecycleOwner),
                Mockito.<ActivityResultContract<VenmoAuthChallenge, VenmoAuthChallengeResult>>any(),
                Mockito.any());
    }

    @Test
    public void launch_launchesAuthChallenge() throws JSONException {
        VenmoAuthChallenge authChallenge =
                new VenmoAuthChallenge(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO), "profile-id", "payment-context-id", "session-id", "custom");
        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);
        FragmentActivity lifecycleOwner = new FragmentActivity();
        VenmoLauncher sut = new VenmoLauncher(activityResultRegistry, lifecycleOwner, callback);
        sut.activityLauncher = activityResultLauncher;

        sut.launch(authChallenge);
        verify(activityResultLauncher).launch(authChallenge);

    }
}
