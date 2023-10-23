package com.braintreepayments.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.os.TransactionTooLargeException;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.fragment.app.FragmentActivity;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureLauncherUnitTest {

    @Mock
    ActivityResultLauncher<ThreeDSecureResult> activityResultLauncher;
    private CardinalResultCallback callback;

    @Before
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
        callback = mock(CardinalResultCallback.class);
    }

    @Test
    public void constructor_createsActivityLauncher() {
        String expectedKey = "com.braintreepayments.api.ThreeDSecure.RESULT";
        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);
        FragmentActivity lifecycleOwner = new FragmentActivity();

        ThreeDSecureLauncher sut = new ThreeDSecureLauncher(activityResultRegistry, lifecycleOwner,
                callback);

        verify(activityResultRegistry).register(eq(expectedKey), same(lifecycleOwner),
                Mockito.<ActivityResultContract<ThreeDSecureResult, CardinalResult>>any(),
                Mockito.any());
    }

    @Test
    public void launch_launchesAuthChallenge()  {
        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);
        FragmentActivity lifecycleOwner = new FragmentActivity();
        ThreeDSecureLauncher sut = new ThreeDSecureLauncher(activityResultRegistry, lifecycleOwner,
                callback);
        sut.activityLauncher = activityResultLauncher;

        ThreeDSecureResult threeDSecureResult = new ThreeDSecureResult();

        sut.launch(threeDSecureResult);
        verify(activityResultLauncher).launch(threeDSecureResult);

    }

    @Test
    public void launch_whenTransactionTooLarge_callsBackError() throws JSONException {
        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);
        FragmentActivity lifecycleOwner = new FragmentActivity();
        ThreeDSecureLauncher sut = new ThreeDSecureLauncher(activityResultRegistry, lifecycleOwner,
                callback);
        sut.activityLauncher = activityResultLauncher;

        ThreeDSecureResult threeDSecureResult =
                ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE);

        TransactionTooLargeException transactionTooLargeException =
                new TransactionTooLargeException();
        RuntimeException runtimeException = new RuntimeException(
                "runtime exception caused by transaction too large", transactionTooLargeException);

        doThrow(runtimeException)
                .when(activityResultLauncher).launch(any(ThreeDSecureResult.class));

        sut.launch(threeDSecureResult);

        ArgumentCaptor<CardinalResult> captor =
                ArgumentCaptor.forClass(CardinalResult.class);
        verify(callback).onCardinalResult(captor.capture());

        Exception exception = captor.getValue().getError();
        assertTrue(exception instanceof BraintreeException);
        String expectedMessage = "The 3D Secure response returned is too large to continue. "
                + "Please contact Braintree Support for assistance.";
        assertEquals(expectedMessage, exception.getMessage());
    }
}
