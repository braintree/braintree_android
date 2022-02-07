package com.braintreepayments.api;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultRegistry;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

@RunWith(RobolectricTestRunner.class)
public class VenmoLifecycleObserverUnitTest {

    @Test
    public void onCreate_registersForAnActivityResult() {
        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);
        VenmoClient venmoClient = mock(VenmoClient.class);
        VenmoLifecycleObserver sut = new VenmoLifecycleObserver(activityResultRegistry, venmoClient);

        FragmentActivity lifecycleOwner = new FragmentActivity();
        sut.onStateChanged(lifecycleOwner, Lifecycle.Event.ON_CREATE);

        String expectedKey = "";
        verify(activityResultRegistry).register(eq(expectedKey), same(lifecycleOwner), any(VenmoActivityResultContract.class), Mockito.<ActivityResultCallback<VenmoResult>>any());
    }

    @Test
    public void onCreate_whenActivityResultReceived_forwardsActivityResultToVenmoClient() {
    }

    @Test
    public void launch_launchesActivity() {
    }
}
