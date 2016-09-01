package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.support.test.runner.AndroidJUnit4;

import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.api.test.BraintreeActivityTestRule;
import com.braintreepayments.testutils.TestConfigurationBuilder;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static com.braintreepayments.api.BraintreeFragmentTestUtils.getMockFragment;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(AndroidJUnit4.class)
public class PayPalTest {

    @Rule
    public final BraintreeActivityTestRule<TestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    @Test
    public void authorizeAccount_onBackPressed_callsCancelListenerOnlyOnce() {
        Configuration configuration = new TestConfigurationBuilder()
                .paypalEnabled(true)
                .buildConfiguration();

        final BraintreeFragment fragment = getMockFragment(mActivityTestRule.getActivity(), configuration);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                fragment.onActivityResult(PayPal.PAYPAL_REQUEST_CODE, Activity.RESULT_CANCELED, new Intent());
                return null;
            }
        }).when(fragment).startActivity(any(Intent.class));

        PayPal.authorizeAccount(fragment);

        verify(fragment, times(1)).postCancelCallback(PayPal.PAYPAL_REQUEST_CODE);
    }
}
