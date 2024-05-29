package com.braintreepayments.api.threedsecure;

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

import com.braintreepayments.api.testutils.Fixtures;
import com.braintreepayments.api.core.BraintreeException;

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
    ActivityResultLauncher<ThreeDSecureParams> activityResultLauncher;
    private ThreeDSecureLauncherCallback callback;

    @Before
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
        callback = mock(ThreeDSecureLauncherCallback.class);
    }

    @Test
    public void constructor_createsActivityLauncher() {
        String expectedKey = "com.braintreepayments.api.ThreeDSecure.RESULT";
        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);
        FragmentActivity lifecycleOwner = new FragmentActivity();

        ThreeDSecureLauncher sut = new ThreeDSecureLauncher(activityResultRegistry, lifecycleOwner,
                callback);

        verify(activityResultRegistry).register(eq(expectedKey), same(lifecycleOwner),
                Mockito.<ActivityResultContract<ThreeDSecureParams, ThreeDSecurePaymentAuthResult>>any(),
                Mockito.any());
    }

    @Test
    public void launch_launchesAuthChallenge()  {
        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);
        FragmentActivity lifecycleOwner = new FragmentActivity();
        ThreeDSecureLauncher sut = new ThreeDSecureLauncher(activityResultRegistry, lifecycleOwner,
                callback);
        sut.activityLauncher = activityResultLauncher;

        ThreeDSecureParams threeDSecureParams = new ThreeDSecureParams();
        ThreeDSecurePaymentAuthRequest.ReadyToLaunch paymentAuthRequest = new ThreeDSecurePaymentAuthRequest.ReadyToLaunch(
                threeDSecureParams);

        sut.launch(paymentAuthRequest);
        verify(activityResultLauncher).launch(threeDSecureParams);

    }

    @Test
    public void launch_whenTransactionTooLarge_callsBackError() throws JSONException {
        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);
        FragmentActivity lifecycleOwner = new FragmentActivity();
        ThreeDSecureLauncher sut = new ThreeDSecureLauncher(activityResultRegistry, lifecycleOwner,
                callback);
        sut.activityLauncher = activityResultLauncher;

        ThreeDSecureParams threeDSecureParams =
                ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE);
        ThreeDSecurePaymentAuthRequest.ReadyToLaunch paymentAuthRequest = new ThreeDSecurePaymentAuthRequest.ReadyToLaunch(
                threeDSecureParams);

        TransactionTooLargeException transactionTooLargeException =
                new TransactionTooLargeException();
        RuntimeException runtimeException = new RuntimeException(
                "runtime exception caused by transaction too large", transactionTooLargeException);

        doThrow(runtimeException)
                .when(activityResultLauncher).launch(any(ThreeDSecureParams.class));

        sut.launch(paymentAuthRequest);

        ArgumentCaptor<ThreeDSecurePaymentAuthResult> captor =
                ArgumentCaptor.forClass(ThreeDSecurePaymentAuthResult.class);
        verify(callback).onThreeDSecurePaymentAuthResult(captor.capture());

        Exception exception = captor.getValue().getError();
        assertTrue(exception instanceof BraintreeException);
        String expectedMessage = "The 3D Secure response returned is too large to continue. "
                + "Please contact Braintree Support for assistance.";
        assertEquals(expectedMessage, exception.getMessage());
    }
}
