package com.braintreepayments.api.googlepay;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class GooglePayLauncherUnitTest {

    private AutoCloseable closeable;

    @Mock
    ActivityResultLauncher<GooglePayPaymentAuthRequestParams> activityLauncher;

    private GooglePayLauncherCallback callback;

    @Before
    public void beforeEach() {
        closeable = MockitoAnnotations.openMocks(this);
        callback = mock(GooglePayLauncherCallback.class);
    }

    @After
    public void teardown() throws Exception {
        closeable.close();
    }

    @Test
    public void constructor_createsActivityLauncher() {
        String expectedKey = "com.braintreepayments.api.GooglePay.RESULT";
        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);
        FragmentActivity lifecycleOwner = new FragmentActivity();

        doReturn(activityLauncher).when(activityResultRegistry).register(any(), any(), any(), any());
        new GooglePayLauncher(activityResultRegistry, lifecycleOwner, callback);

        verify(activityResultRegistry).register(eq(expectedKey), same(lifecycleOwner),
                Mockito.<ActivityResultContract<GooglePayPaymentAuthRequestParams, GooglePayPaymentAuthResult>>any(),
                Mockito.any());
    }

    @Test
    public void launch_launchesActivity() {
        GooglePayRequest googlePayRequest = new GooglePayRequest("USD", "1.00", GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL);

        PaymentDataRequest paymentDataRequest =
                PaymentDataRequest.fromJson(googlePayRequest.toJson());
        GooglePayPaymentAuthRequestParams
                intentData = new GooglePayPaymentAuthRequestParams(1, paymentDataRequest);
        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);
        FragmentActivity lifecycleOwner = new FragmentActivity();

        doReturn(activityLauncher).when(activityResultRegistry).register(any(), any(), any(), any());
        GooglePayLauncher sut = new GooglePayLauncher(activityResultRegistry, lifecycleOwner, callback);

        sut.launch(new GooglePayPaymentAuthRequest.ReadyToLaunch(intentData));
        verify(activityLauncher).launch(intentData);
    }
}
