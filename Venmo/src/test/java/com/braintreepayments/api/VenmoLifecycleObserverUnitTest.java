package com.braintreepayments.api;

import static android.os.Looper.getMainLooper;
import static com.braintreepayments.api.BraintreeRequestCodes.PAYPAL;
import static com.braintreepayments.api.BraintreeRequestCodes.THREE_D_SECURE;
import static com.braintreepayments.api.BraintreeRequestCodes.VENMO;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class VenmoLifecycleObserverUnitTest {

    @Mock
    ActivityResultLauncher<VenmoIntentData> activityResultLauncher;

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
                new VenmoResult("payment-context-id", "venmoAccount", "venmoUsername", null);

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
        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);

        VenmoClient venmoClient = mock(VenmoClient.class);
        VenmoLifecycleObserver sut = new VenmoLifecycleObserver(activityResultRegistry, venmoClient);
        sut.activityLauncher = activityResultLauncher;

        sut.launch(venmoIntentData);
        verify(activityResultLauncher).launch(venmoIntentData);
    }

    @Test
    public void onResume_whenLifeCycleObserverIsFragment_venmoClientDeliversResultWithFragmentActivity() {
        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);
        Fragment fragment = mock(Fragment.class);
        FragmentActivity activity = mock(FragmentActivity.class);
        when(fragment.getActivity()).thenReturn(activity);

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getRequestCode()).thenReturn(VENMO);

        VenmoClient venmoClient = mock(VenmoClient.class);
        when(venmoClient.getBrowserSwitchResult(activity)).thenReturn(browserSwitchResult);
        when(venmoClient.deliverBrowserSwitchResult(activity)).thenReturn(browserSwitchResult);

        VenmoLifecycleObserver sut = new VenmoLifecycleObserver(activityResultRegistry, venmoClient);
        sut.onStateChanged(fragment, Lifecycle.Event.ON_RESUME);

        // Ref: https://robolectric.org/blog/2019/06/04/paused-looper/
        shadowOf(getMainLooper()).idle();
        verify(venmoClient).onBrowserSwitchResult(same(browserSwitchResult));
    }

    @Test
    public void onResume_whenLifeCycleObserverIsActivity_venmoClientDeliversResultWithSameActivity() {
        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);
        FragmentActivity activity = mock(FragmentActivity.class);

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getRequestCode()).thenReturn(VENMO);

        VenmoClient venmoClient = mock(VenmoClient.class);
        when(venmoClient.getBrowserSwitchResult(activity)).thenReturn(browserSwitchResult);
        when(venmoClient.deliverBrowserSwitchResult(activity)).thenReturn(browserSwitchResult);

        VenmoLifecycleObserver sut = new VenmoLifecycleObserver(activityResultRegistry, venmoClient);
        sut.onStateChanged(activity, Lifecycle.Event.ON_RESUME);

        shadowOf(getMainLooper()).idle();
        verify(venmoClient).onBrowserSwitchResult(same(browserSwitchResult));
    }

    @Test
    public void onResume_whenLifeCycleObserverIsFragment_venmoClientDeliversResultFromCacheWithFragmentActivity() {
        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);
        Fragment fragment = mock(Fragment.class);
        FragmentActivity activity = mock(FragmentActivity.class);
        when(fragment.getActivity()).thenReturn(activity);

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getRequestCode()).thenReturn(VENMO);

        VenmoClient venmoClient = mock(VenmoClient.class);
        when(venmoClient.getBrowserSwitchResultFromNewTask(activity)).thenReturn(browserSwitchResult);
        when(venmoClient.deliverBrowserSwitchResultFromNewTask(activity)).thenReturn(browserSwitchResult);

        VenmoLifecycleObserver sut = new VenmoLifecycleObserver(activityResultRegistry, venmoClient);
        sut.onStateChanged(fragment, Lifecycle.Event.ON_RESUME);

        shadowOf(getMainLooper()).idle();
        verify(venmoClient).onBrowserSwitchResult(same(browserSwitchResult));
    }

    @Test
    public void onResume_whenLifeCycleObserverIsActivity_venmoClientDeliversResultFromCacheWithSameActivity() {
        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);
        FragmentActivity activity = mock(FragmentActivity.class);

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getRequestCode()).thenReturn(VENMO);

        VenmoClient venmoClient = mock(VenmoClient.class);
        when(venmoClient.getBrowserSwitchResultFromNewTask(activity)).thenReturn(browserSwitchResult);
        when(venmoClient.deliverBrowserSwitchResultFromNewTask(activity)).thenReturn(browserSwitchResult);

        VenmoLifecycleObserver sut = new VenmoLifecycleObserver(activityResultRegistry, venmoClient);
        sut.onStateChanged(activity, Lifecycle.Event.ON_RESUME);

        shadowOf(getMainLooper()).idle();
        verify(venmoClient).onBrowserSwitchResult(same(browserSwitchResult));
    }

    @Test
    public void onResume_whenPendingBrowserSwitchResultExists_andRequestCodeNotVenmo_doesNothing() {
        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);
        FragmentActivity activity = mock(FragmentActivity.class);

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getRequestCode()).thenReturn(THREE_D_SECURE);

        VenmoClient venmoClient = mock(VenmoClient.class);
        when(venmoClient.getBrowserSwitchResult(activity)).thenReturn(browserSwitchResult);
        when(venmoClient.deliverBrowserSwitchResult(activity)).thenReturn(browserSwitchResult);

        VenmoLifecycleObserver sut = new VenmoLifecycleObserver(activityResultRegistry, venmoClient);
        sut.onStateChanged(activity, Lifecycle.Event.ON_RESUME);

        shadowOf(getMainLooper()).idle();
        verify(venmoClient, never()).onBrowserSwitchResult(any(BrowserSwitchResult.class));
    }

    @Test
    public void onResume_whenCachedBrowserSwitchResultExists_andRequestCodeNotVenmo_doesNothing() {
        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);
        FragmentActivity activity = mock(FragmentActivity.class);

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getRequestCode()).thenReturn(BraintreeRequestCodes.PAYPAL);

        VenmoClient venmoClient = mock(VenmoClient.class);
        when(venmoClient.getBrowserSwitchResult(activity)).thenReturn(browserSwitchResult);

        VenmoLifecycleObserver sut = new VenmoLifecycleObserver(activityResultRegistry, venmoClient);

        sut.onStateChanged(activity, Lifecycle.Event.ON_RESUME);

        verify(venmoClient, never()).onBrowserSwitchResult(browserSwitchResult);
    }
}
