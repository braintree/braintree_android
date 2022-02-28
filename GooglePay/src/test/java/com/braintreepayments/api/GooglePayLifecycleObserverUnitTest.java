package com.braintreepayments.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;

import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class GooglePayLifecycleObserverUnitTest {

    @Captor
    ArgumentCaptor<ActivityResultCallback<GooglePayResult>> googlePayResultCaptor;

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void onCreate_registersForAnActivityResult() {
        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);
        GooglePayClient googlePayClient = mock(GooglePayClient.class);
        GooglePayLifecycleObserver sut = new GooglePayLifecycleObserver(activityResultRegistry, googlePayClient);

        FragmentActivity lifecycleOwner = mock(FragmentActivity.class);
        sut.onStateChanged(lifecycleOwner, Lifecycle.Event.ON_CREATE);

        String expectedKey = "com.braintreepayments.api.GooglePay.RESULT";
        verify(activityResultRegistry).register(eq(expectedKey), same(lifecycleOwner), any(GooglePayActivityResultContract.class), Mockito.<ActivityResultCallback<GooglePayResult>>any());
    }

    @Test
    public void onCreate_whenActivityResultReceived_forwardsActivityResultToGooglePayClient() {
        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);
        GooglePayClient googlePayClient = mock(GooglePayClient.class);
        GooglePayLifecycleObserver sut = new GooglePayLifecycleObserver(activityResultRegistry, googlePayClient);

        FragmentActivity lifecycleOwner = mock(FragmentActivity.class);
        sut.onStateChanged(lifecycleOwner, Lifecycle.Event.ON_CREATE);

        String expectedKey = "com.braintreepayments.api.GooglePay.RESULT";
        verify(activityResultRegistry).register(eq(expectedKey), same(lifecycleOwner), any(GooglePayActivityResultContract.class), googlePayResultCaptor.capture());

        ActivityResultCallback<GooglePayResult> activityResultCallback = googlePayResultCaptor.getValue();
        GooglePayResult result = new GooglePayResult(null, null);
        activityResultCallback.onActivityResult(result);
        verify(googlePayClient).onGooglePayResult(result);
    }

    @Test
    public void launch_launchesActivity() {
        GooglePayRequest googlePayRequest = new GooglePayRequest();
        googlePayRequest.setTransactionInfo(TransactionInfo.newBuilder()
                .setTotalPrice("1.00")
                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                .setCurrencyCode("USD")
                .build());

        PaymentDataRequest paymentDataRequest = PaymentDataRequest.fromJson(googlePayRequest.toJson());
        GooglePayIntentData intentData = new GooglePayIntentData(1, paymentDataRequest);
        ActivityResultLauncher<GooglePayIntentData> resultLauncher = mock(ActivityResultLauncher.class);
        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);

        GooglePayClient googlePayClient = mock(GooglePayClient.class);
        GooglePayLifecycleObserver sut = new GooglePayLifecycleObserver(activityResultRegistry, googlePayClient);
        sut.activityLauncher = resultLauncher;

        sut.launch(intentData);
        verify(resultLauncher).launch(intentData);
    }
}

