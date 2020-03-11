package com.braintreepayments.api;

import android.content.Intent;

import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.shadows.ShadowVisaCheckout;
import com.braintreepayments.api.test.AppCompatTestActivity;
import com.braintreepayments.api.test.FragmentTestActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.CountDownLatch;

import androidx.appcompat.app.AppCompatActivity;

import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "androidx.*", "org.json.*" })
@PrepareForTest({ ShadowVisaCheckout.class })
public class VisaCheckoutFacadeTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    private AppCompatActivity mActivity;
    private BraintreeFragment mFragment;
    private CountDownLatch mLatch;
    private Intent mIntent = new Intent();

    @Before
    public void setup() throws InvalidArgumentException {
        mActivity = spy(Robolectric.setupActivity(AppCompatTestActivity.class));
        mFragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        mLatch = new CountDownLatch(1);
    }

    @Test
    public void onActivityResult_invokesOnActivityResultOnVisaCheckoutBraintreeClass() {
        VisaCheckoutFacade.sVisaCheckoutClassName = ShadowVisaCheckout.class.getName();

        mockStatic(ShadowVisaCheckout.class);

        mFragment.onActivityResult(BraintreeRequestCodes.VISA_CHECKOUT, AppCompatActivity.RESULT_OK, mIntent);

        verifyStatic();
        ShadowVisaCheckout.onActivityResult(mFragment, AppCompatActivity.RESULT_OK, mIntent);
    }
}
