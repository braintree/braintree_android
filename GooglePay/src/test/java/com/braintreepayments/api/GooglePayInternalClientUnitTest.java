package com.braintreepayments.api;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentsClient;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.CountDownLatch;

@RunWith(RobolectricTestRunner.class)
public class GooglePayInternalClientUnitTest {

    private FragmentActivity activity;
    private PaymentsClientWrapper paymentsClientWrapper;
    private PaymentsClient paymentsClient;
    private IsReadyToPayRequest isReadyToPayRequest;

    @Before
    public void beforeEach() {
        activity = mock(FragmentActivity.class);
        paymentsClientWrapper = mock(PaymentsClientWrapper.class);
        paymentsClient = mock(PaymentsClient.class);
        isReadyToPayRequest = IsReadyToPayRequest.fromJson("{}");
    }

    @Test
    public void isReadyToPay_onSuccess_forwardsResultToCallback() throws InterruptedException, JSONException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY);
        when(paymentsClientWrapper.getPaymentsClient(same(activity), same(configuration))).thenReturn(paymentsClient);
        when(paymentsClient.isReadyToPay(same(isReadyToPayRequest))).thenReturn(Tasks.forResult(true));

        GooglePayInternalClient sut = new GooglePayInternalClient(paymentsClientWrapper);
        sut.isReadyToPay(activity, configuration, isReadyToPayRequest, new GooglePayIsReadyToPayCallback() {
            @Override
            public void onResult(boolean isReadyToPay, Exception error) {
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
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY);
        when(paymentsClientWrapper.getPaymentsClient(same(activity), same(configuration))).thenReturn(paymentsClient);

        final ApiException error = new ApiException(Status.RESULT_INTERNAL_ERROR);
        when(paymentsClient.isReadyToPay(same(isReadyToPayRequest))).thenReturn(Tasks.<Boolean>forException(error));

        GooglePayInternalClient sut = new GooglePayInternalClient(paymentsClientWrapper);
        sut.isReadyToPay(activity, configuration, isReadyToPayRequest, new GooglePayIsReadyToPayCallback() {
            @Override
            public void onResult(boolean isReadyToPay, Exception e) {
                assertFalse(isReadyToPay);
                assertSame(error, e);
                countDownLatch.countDown();
            }
        });
        countDownLatch.await();
    }
}