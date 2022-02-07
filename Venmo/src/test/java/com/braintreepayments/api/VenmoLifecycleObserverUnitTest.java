package com.braintreepayments.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class VenmoLifecycleObserverUnitTest {

    @Captor
    ArgumentCaptor<ActivityResultCallback<VenmoResult>> venmoResultCaptor;

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void onCreate_registersForAnActivityResult() {
        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);
        VenmoClient venmoClient = mock(VenmoClient.class);
        VenmoLifecycleObserver sut = new VenmoLifecycleObserver(activityResultRegistry, venmoClient);

        FragmentActivity lifecycleOwner = new FragmentActivity();
        sut.onStateChanged(lifecycleOwner, Lifecycle.Event.ON_CREATE);

        String expectedKey = "com.braintreepayments.api.Venmo.RESULT";
        verify(activityResultRegistry).register(eq(expectedKey), same(lifecycleOwner), any(VenmoActivityResultContract.class), Mockito.<ActivityResultCallback<VenmoResult>>any());
    }

    @Test
    public void onCreate_whenActivityResultReceived_forwardsActivityResultToVenmoClient() {
        VenmoResult venmoResult =
                new VenmoResult("paymentContextId", "venmoAccount", "venmoUsername", null);

        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);

        VenmoClient venmoClient = mock(VenmoClient.class);
        VenmoLifecycleObserver sut = new VenmoLifecycleObserver(activityResultRegistry, venmoClient);

        FragmentActivity lifecycleOwner = new FragmentActivity();
        sut.onStateChanged(lifecycleOwner, Lifecycle.Event.ON_CREATE);
        verify(activityResultRegistry).register(anyString(), any(LifecycleOwner.class), any(VenmoActivityResultContract.class), venmoResultCaptor.capture());

        ActivityResultCallback<VenmoResult> activityResultCallback = venmoResultCaptor.getValue();
        activityResultCallback.onActivityResult(venmoResult);
        verify(venmoClient).onVenmoResult(venmoResult);
    }

    @Test
    public void launch_launchesActivity() throws JSONException {
        VenmoIntentData venmoIntentData =
                new VenmoIntentData(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO), "venmoAccount", "venmoUsername", "sessionId", "integrationType");
        ActivityResultLauncher<VenmoIntentData> resultLauncher = mock(ActivityResultLauncher.class);
        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);

        VenmoClient venmoClient = mock(VenmoClient.class);
        VenmoLifecycleObserver sut = new VenmoLifecycleObserver(activityResultRegistry, venmoClient);
        sut.activityLauncher = resultLauncher;

        sut.launch(venmoIntentData);
        verify(resultLauncher).launch(venmoIntentData);
    }
}
