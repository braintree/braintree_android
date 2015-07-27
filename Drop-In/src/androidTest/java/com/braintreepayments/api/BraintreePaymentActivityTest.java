package com.braintreepayments.api;

import android.content.Intent;
import android.support.test.rule.ActivityTestRule;
import android.test.suitebuilder.annotation.LargeTest;

import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.junit.Rule;
import org.junit.Test;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertEquals;

@LargeTest
public class BraintreePaymentActivityTest {

    @Rule
    public ActivityTestRule<BraintreePaymentActivity> mActivityTestRule =
            new ActivityTestRule<>(BraintreePaymentActivity.class, true, false);

    @Test(timeout = 30000)
    public void setsIntegrationTypeToDropinForDropinActivity() {
        Intent intent = new Intent(getTargetContext(), BraintreePaymentActivity.class)
                .putExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN, new TestClientTokenBuilder().build());
        BraintreePaymentActivity activity = mActivityTestRule.launchActivity(intent);

        assertEquals("dropin", activity.mBraintreeFragment.mIntegrationType);
    }
}
