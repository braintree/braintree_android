package com.braintreepayments.api;

import android.app.Activity;
import android.os.SystemClock;
import android.support.test.runner.AndroidJUnit4;
import android.text.TextUtils;

import com.braintreepayments.api.exceptions.BraintreeApiErrorResponse;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.BraintreePaymentResultListener;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.internal.BraintreeSharedPreferences;
import com.braintreepayments.api.models.BraintreePaymentResult;
import com.braintreepayments.api.models.IdealBank;
import com.braintreepayments.api.models.IdealRequest;
import com.braintreepayments.api.models.IdealResult;
import com.braintreepayments.api.test.BraintreeActivityTestRule;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.api.test.TestClientTokenBuilder;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static com.braintreepayments.api.BraintreeFragmentTestUtils.getFragmentWithAuthorization;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class IdealTest {

    @Rule
    public final BraintreeActivityTestRule<TestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    private BraintreeFragment mBraintreeFragment;
    private CountDownLatch mCountDownLatch;

    @Before
    public void setUp() throws InvalidArgumentException {
        mCountDownLatch = new CountDownLatch(1);

        mBraintreeFragment = getFragmentWithAuthorization(mActivityTestRule.getActivity(),
                new TestClientTokenBuilder().withIdeal().withoutCustomer().build());
    }

    @Test(timeout = 10000)
    public void fetchIssuingBanks_returnsIssuingBanks() throws InterruptedException {
        Ideal.fetchIssuingBanks(mBraintreeFragment, new BraintreeResponseListener<List<IdealBank>>() {
            @Override
            public void onResponse(List<IdealBank> idealBanks) {
                assertFalse(idealBanks.isEmpty());
                assertFalse(TextUtils.isEmpty(idealBanks.get(0).getId()));
                assertFalse(TextUtils.isEmpty(idealBanks.get(0).getName()));
                assertFalse(TextUtils.isEmpty(idealBanks.get(0).getImageUri()));
                mCountDownLatch.countDown();
            }
        });

        mBraintreeFragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                fail(error.getMessage());
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    public void startPayment_returnsPendingPayment() throws InterruptedException {
        IdealRequest builder = new IdealRequest()
                .currency("EUR")
                .amount("10")
                .issuerId("INGBNL2A")
                .orderId(UUID.randomUUID().toString().substring(0, 15));

        Ideal.startPayment(mBraintreeFragment, builder, null);

        String savedId;
        do {
            savedId = BraintreeSharedPreferences
                    .getString(mBraintreeFragment.getApplicationContext(), Ideal.IDEAL_RESULT_ID);
            SystemClock.sleep(500);
        } while (savedId.equals(""));

        Ideal.onActivityResult(mBraintreeFragment, Activity.RESULT_OK);

        mBraintreeFragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                fail(error.getMessage());
            }
        });

        mBraintreeFragment.addListener(new BraintreePaymentResultListener() {
            @Override
            public void onBraintreePaymentResult(BraintreePaymentResult result) {
                assertTrue(result instanceof IdealResult);
                IdealResult idealResult = (IdealResult) result;
                assertFalse(TextUtils.isEmpty(idealResult.getId()));
                assertFalse(TextUtils.isEmpty(idealResult.getShortId()));
                assertFalse(TextUtils.isEmpty(idealResult.getStatus()));

                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    public void startPayment_returnsError_whenOrderId_isAbsent() throws InterruptedException {
        IdealRequest builder = new IdealRequest()
                .currency("EUR")
                .amount("10")
                .issuerId("INGBNL2A");

        Ideal.startPayment(mBraintreeFragment, builder, null);

        mBraintreeFragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertTrue(error instanceof BraintreeApiErrorResponse);
                mCountDownLatch.countDown();
            }
        });

        mBraintreeFragment.addListener(new BraintreePaymentResultListener() {
            @Override
            public void onBraintreePaymentResult(BraintreePaymentResult result) {
                fail("BraintreeApiErrorResponse expected");
            }
        });

        mCountDownLatch.await();
    }
}
