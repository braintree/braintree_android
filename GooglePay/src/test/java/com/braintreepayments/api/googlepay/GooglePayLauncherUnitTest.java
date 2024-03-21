package com.braintreepayments.api.googlepay;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.googlepay.GooglePayLauncher;
import com.braintreepayments.api.googlepay.GooglePayLauncherCallback;
import com.braintreepayments.api.googlepay.GooglePayPaymentAuthRequestParams;
import com.braintreepayments.api.googlepay.GooglePayPaymentAuthResult;
import com.braintreepayments.api.googlepay.GooglePayRequest;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class GooglePayLauncherUnitTest {

    @Mock
    ActivityResultLauncher<GooglePayPaymentAuthRequestParams> activityLauncher;

    private GooglePayLauncherCallback callback;

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        callback = mock(GooglePayLauncherCallback.class);
    }

    @Test
    public void constructor_createsActivityLauncher() {
        String expectedKey = "com.braintreepayments.api.GooglePay.RESULT";
        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);
        FragmentActivity lifecycleOwner = new FragmentActivity();

        GooglePayLauncher sut = new GooglePayLauncher(activityResultRegistry, lifecycleOwner,
                callback);

        verify(activityResultRegistry).register(eq(expectedKey), same(lifecycleOwner),
                Mockito.<ActivityResultContract<GooglePayPaymentAuthRequestParams, GooglePayPaymentAuthResult>>any(),
                Mockito.any());
    }

    @Test
    public void launch_launchesActivity() {
        GooglePayRequest googlePayRequest = new GooglePayRequest();
        googlePayRequest.setTransactionInfo(TransactionInfo.newBuilder()
                .setTotalPrice("1.00")
                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                .setCurrencyCode("USD")
                .build());

        PaymentDataRequest paymentDataRequest =
                PaymentDataRequest.fromJson(googlePayRequest.toJson());
        GooglePayPaymentAuthRequestParams
                intentData = new GooglePayPaymentAuthRequestParams(1, paymentDataRequest);
        ActivityResultRegistry activityResultRegistry = mock(ActivityResultRegistry.class);
        FragmentActivity lifecycleOwner = new FragmentActivity();

        GooglePayLauncher sut = new GooglePayLauncher(activityResultRegistry, lifecycleOwner,
                callback);
        sut.activityLauncher = activityLauncher;

        sut.launch(intentData);
        verify(activityLauncher).launch(intentData);
    }
}
