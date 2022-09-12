package com.braintreepayments.api;

import static com.braintreepayments.api.BraintreeRequestCodes.PAYPAL;
import static com.braintreepayments.api.BraintreeRequestCodes.THREE_D_SECURE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse;

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
public class ThreeDSecureLifecycleObserverUnitTest {

    @Captor
    ArgumentCaptor<ActivityResultCallback<CardinalResult>> cardinalResultCaptor;

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void onCreate_registersForAnActivityResult() {
        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);

        ThreeDSecureClient threeDSecureClient = mock(ThreeDSecureClient.class);
        ThreeDSecureLifecycleObserver sut = new ThreeDSecureLifecycleObserver(activityResultRegistry, threeDSecureClient);

        FragmentActivity lifecycleOwner = new FragmentActivity();
        sut.onStateChanged(lifecycleOwner, Lifecycle.Event.ON_CREATE);

        String expectedKey = "com.braintreepayments.api.ThreeDSecure.RESULT";
        verify(activityResultRegistry).register(eq(expectedKey), same(lifecycleOwner), any(ThreeDSecureActivityResultContract.class), Mockito.<ActivityResultCallback<CardinalResult>>any());
    }

    @Test
    public void onCreate_whenActivityResultReceived_forwardsActivityResultToThreeDSecureClient() throws JSONException {
        ThreeDSecureResult threeDSecureResult =
                ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE);
        ValidateResponse validateResponse = mock(ValidateResponse.class);

        CardinalResult cardinalResult =
                new CardinalResult(threeDSecureResult, "sample jwt", validateResponse);

        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);

        ThreeDSecureClient threeDSecureClient = mock(ThreeDSecureClient.class);
        ThreeDSecureLifecycleObserver sut = new ThreeDSecureLifecycleObserver(activityResultRegistry, threeDSecureClient);

        FragmentActivity lifecycleOwner = new FragmentActivity();
        sut.onStateChanged(lifecycleOwner, Lifecycle.Event.ON_CREATE);
        verify(activityResultRegistry).register(anyString(), any(LifecycleOwner.class), any(ThreeDSecureActivityResultContract.class), cardinalResultCaptor.capture());

        ActivityResultCallback<CardinalResult> activityResultCallback = cardinalResultCaptor.getValue();
        activityResultCallback.onActivityResult(cardinalResult);
        verify(threeDSecureClient).onCardinalResult(cardinalResult);
    }

    @Test
    public void onResume_whenLifeCycleObserverIsFragment_threeDSecureClientDeliversResultWithFragmentActivity() {
        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);

        Fragment fragment = mock(Fragment.class);
        FragmentActivity activity = new FragmentActivity();
        when(fragment.getActivity()).thenReturn(activity);

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getRequestCode()).thenReturn(THREE_D_SECURE);

        ThreeDSecureClient threeDSecureClient = mock(ThreeDSecureClient.class);
        when(threeDSecureClient.getBrowserSwitchResult(activity)).thenReturn(browserSwitchResult);
        when(threeDSecureClient.deliverBrowserSwitchResult(activity)).thenReturn(browserSwitchResult);

        ThreeDSecureLifecycleObserver sut = new ThreeDSecureLifecycleObserver(activityResultRegistry, threeDSecureClient);

        sut.onStateChanged(fragment, Lifecycle.Event.ON_RESUME);

        verify(threeDSecureClient).onBrowserSwitchResult(same(browserSwitchResult));
    }

    @Test
    public void onResume_whenLifeCycleObserverIsActivity_threeDSecureClientDeliversResultWithSameActivity() {
        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);
        FragmentActivity activity = new FragmentActivity();

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getRequestCode()).thenReturn(THREE_D_SECURE);

        ThreeDSecureClient threeDSecureClient = mock(ThreeDSecureClient.class);
        when(threeDSecureClient.getBrowserSwitchResult(activity)).thenReturn(browserSwitchResult);
        when(threeDSecureClient.deliverBrowserSwitchResult(activity)).thenReturn(browserSwitchResult);

        ThreeDSecureLifecycleObserver sut = new ThreeDSecureLifecycleObserver(activityResultRegistry, threeDSecureClient);

        sut.onStateChanged(activity, Lifecycle.Event.ON_RESUME);

        verify(threeDSecureClient).onBrowserSwitchResult(same(browserSwitchResult));
    }

    @Test
    public void onResume_whenLifeCycleObserverIsFragment_threeDSecureClientDeliversResultFromCacheWithFragmentActivity() {
        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);

        Fragment fragment = mock(Fragment.class);
        FragmentActivity activity = new FragmentActivity();
        when(fragment.getActivity()).thenReturn(activity);

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getRequestCode()).thenReturn(THREE_D_SECURE);

        ThreeDSecureClient threeDSecureClient = mock(ThreeDSecureClient.class);
        when(threeDSecureClient.getBrowserSwitchResultFromCache(activity)).thenReturn(browserSwitchResult);
        when(threeDSecureClient.deliverBrowserSwitchResultFromCache(activity)).thenReturn(browserSwitchResult);

        ThreeDSecureLifecycleObserver sut = new ThreeDSecureLifecycleObserver(activityResultRegistry, threeDSecureClient);

        sut.onStateChanged(fragment, Lifecycle.Event.ON_RESUME);

        verify(threeDSecureClient).onBrowserSwitchResult(same(browserSwitchResult));
    }

    @Test
    public void onResume_whenLifeCycleObserverIsActivity_threeDSecureClientDeliversResultFromCacheWithSameActivity() {
        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);
        FragmentActivity activity = new FragmentActivity();

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getRequestCode()).thenReturn(THREE_D_SECURE);

        ThreeDSecureClient threeDSecureClient = mock(ThreeDSecureClient.class);
        when(threeDSecureClient.getBrowserSwitchResultFromCache(activity)).thenReturn(browserSwitchResult);
        when(threeDSecureClient.deliverBrowserSwitchResultFromCache(activity)).thenReturn(browserSwitchResult);

        ThreeDSecureLifecycleObserver sut = new ThreeDSecureLifecycleObserver(activityResultRegistry, threeDSecureClient);

        sut.onStateChanged(activity, Lifecycle.Event.ON_RESUME);

        verify(threeDSecureClient).onBrowserSwitchResult(same(browserSwitchResult));
    }

    @Test
    public void onResume_whenPendingBrowserSwitchResultExists_andRequestCodeNotThreeDSecure_doesNothing() {
        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);
        FragmentActivity activity = new FragmentActivity();

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getRequestCode()).thenReturn(PAYPAL);

        ThreeDSecureClient threeDSecureClient = mock(ThreeDSecureClient.class);
        when(threeDSecureClient.getBrowserSwitchResult(activity)).thenReturn(browserSwitchResult);
        when(threeDSecureClient.deliverBrowserSwitchResult(activity)).thenReturn(browserSwitchResult);

        ThreeDSecureLifecycleObserver sut = new ThreeDSecureLifecycleObserver(activityResultRegistry, threeDSecureClient);

        sut.onStateChanged(activity, Lifecycle.Event.ON_RESUME);

        verify(threeDSecureClient, never()).onBrowserSwitchResult(any(BrowserSwitchResult.class));
    }

    @Test
    public void launch_launchesActivityWithThreeDSecureResult() throws JSONException {
        ThreeDSecureResult threeDSecureResult =
                ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE);
        ActivityResultLauncher<ThreeDSecureResult> resultLauncher = mock(ActivityResultLauncher.class);
        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);

        ThreeDSecureClient threeDSecureClient = mock(ThreeDSecureClient.class);
        ThreeDSecureLifecycleObserver sut = new ThreeDSecureLifecycleObserver(activityResultRegistry, threeDSecureClient);
        sut.activityLauncher = resultLauncher;

        sut.launch(threeDSecureResult);
        verify(resultLauncher).launch(threeDSecureResult);
    }
}