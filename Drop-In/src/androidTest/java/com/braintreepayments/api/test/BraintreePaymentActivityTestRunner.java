package com.braintreepayments.api.test;

import android.content.Intent;
import android.support.test.runner.AndroidJUnit4;

import com.braintreepayments.api.BraintreePaymentActivity;
import com.braintreepayments.api.BraintreePaymentTestActivity;
import com.braintreepayments.api.PayPalTestSignatureVerification;
import com.braintreepayments.api.PaymentRequest;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.testutils.BraintreeActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getTargetContext;

@RunWith(AndroidJUnit4.class)
public class BraintreePaymentActivityTestRunner {

    @Rule
    public BraintreeActivityTestRule<BraintreePaymentTestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(BraintreePaymentTestActivity.class, true, false);

    @Before
    public void setUp() {
        BraintreeHttpClient.DEBUG = true;
        PayPalTestSignatureVerification.disableAppSwitchSignatureVerification();
    }

    public BraintreePaymentActivity getActivity(String clientToken) {
        Intent intent = new PaymentRequest()
                .clientToken(clientToken)
                .getIntent(getTargetContext());
        intent.setClass(getTargetContext(), BraintreePaymentTestActivity.class);
        return mActivityTestRule.launchActivity(intent);
    }

    public BraintreePaymentActivity getActivity(String clientToken, long delay) {
        Intent intent = new PaymentRequest()
                .clientToken(clientToken)
                .getIntent(getTargetContext());
        intent.setClass(getTargetContext(), BraintreePaymentTestActivity.class)
                .putExtra(BraintreePaymentTestActivity.EXTRA_DELAY, delay);
        return mActivityTestRule.launchActivity(intent);
    }

    public BraintreePaymentActivity getActivity(Intent intent) {
        intent.setClass(getTargetContext(), BraintreePaymentTestActivity.class);
        return mActivityTestRule.launchActivity(intent);
    }

    @Test(timeout = 100)
    public void test(){}
}
