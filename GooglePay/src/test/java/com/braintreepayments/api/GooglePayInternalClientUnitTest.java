package com.braintreepayments.api;

import android.app.Activity;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricTestRunner.class)
@PrepareForTest({Wallet.class})
@PowerMockIgnore({"org.powermock.*", "org.mockito.*", "org.robolectric.*", "android.*", "androidx.*"})
public class GooglePayInternalClientUnitTest {

    @Rule
    public PowerMockRule powerMockRule = new PowerMockRule();

    private FragmentActivity activity;

    private PaymentsClient paymentsClient;
    private IsReadyToPayRequest isReadyToPayRequest;
    private GooglePayIsReadyToPayCallback isReadyToPayCallback;

    @Before
    public void beforeEach() {
        mockStatic(Wallet.class);

        activity = mock(FragmentActivity.class);
        isReadyToPayCallback = mock(GooglePayIsReadyToPayCallback.class);

        paymentsClient = mock(PaymentsClient.class);
        isReadyToPayRequest = IsReadyToPayRequest.fromJson("{}");
    }

    @Test
    public void isReadyToPay_whenConfigurationEnvironmentIsSandbox_requestsSandboxWalletEnvironment() throws JSONException {
        when(Wallet.getPaymentsClient(any(Activity.class), any(Wallet.WalletOptions.class))).thenReturn(paymentsClient);
        when(paymentsClient.isReadyToPay(same(isReadyToPayRequest))).thenReturn(Tasks.forResult(true));

        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY);

        GooglePayInternalClient sut = new GooglePayInternalClient();
        sut.isReadyToPay(activity, configuration, isReadyToPayRequest, isReadyToPayCallback);

        verifyStatic(Wallet.class);

        ArgumentCaptor<Wallet.WalletOptions> captor = ArgumentCaptor.forClass(Wallet.WalletOptions.class);
        Wallet.getPaymentsClient(same(activity), captor.capture());

        Wallet.WalletOptions walletOptions = captor.getValue();
        assertEquals(WalletConstants.ENVIRONMENT_TEST, walletOptions.environment);
    }

    @Test
    public void isReadyToPay_whenConfigurationEnvironmentIsProduction_requestsProductionWalletEnvironment() throws JSONException {
        when(Wallet.getPaymentsClient(any(Activity.class), any(Wallet.WalletOptions.class))).thenReturn(paymentsClient);
        when(paymentsClient.isReadyToPay(same(isReadyToPayRequest))).thenReturn(Tasks.forResult(true));

        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY_PRODUCTION);

        GooglePayInternalClient sut = new GooglePayInternalClient();
        sut.isReadyToPay(activity, configuration, isReadyToPayRequest, isReadyToPayCallback);

        verifyStatic(Wallet.class);

        ArgumentCaptor<Wallet.WalletOptions> captor = ArgumentCaptor.forClass(Wallet.WalletOptions.class);
        Wallet.getPaymentsClient(same(activity), captor.capture());

        Wallet.WalletOptions walletOptions = captor.getValue();
        assertEquals(WalletConstants.ENVIRONMENT_PRODUCTION, walletOptions.environment);
    }

    @Test
    public void isReadyToPay_onSuccess_forwardsResultToCallback() throws InterruptedException, JSONException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        when(Wallet.getPaymentsClient(same(activity), any(Wallet.WalletOptions.class))).thenReturn(paymentsClient);

        when(paymentsClient.isReadyToPay(same(isReadyToPayRequest))).thenReturn(Tasks.forResult(true));

        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY);

        GooglePayInternalClient sut = new GooglePayInternalClient();
        sut.isReadyToPay(activity, configuration, isReadyToPayRequest, new GooglePayIsReadyToPayCallback() {
            @Override
            public void onResult(Boolean isReadyToPay, Exception error) {
                assertTrue(isReadyToPay);
                assertNull(error);
                countDownLatch.countDown();
            }
        });
        countDownLatch.await();
    }

    @Test
    public void isReadyToPay_onFailure_forwardsResultToCallback() throws InterruptedException, JSONException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        when(Wallet.getPaymentsClient(any(Activity.class), any(Wallet.WalletOptions.class))).thenReturn(paymentsClient);

        final ApiException error = new ApiException(Status.RESULT_INTERNAL_ERROR);
        when(paymentsClient.isReadyToPay(same(isReadyToPayRequest))).thenReturn(Tasks.<Boolean>forException(error));

        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY);

        GooglePayInternalClient sut = new GooglePayInternalClient();
        sut.isReadyToPay(activity, configuration, isReadyToPayRequest, new GooglePayIsReadyToPayCallback() {
            @Override
            public void onResult(Boolean isReadyToPay, Exception e) {
                assertFalse(isReadyToPay);
                assertSame(error, e);
                countDownLatch.countDown();
            }
        });
        countDownLatch.await();
    }
}