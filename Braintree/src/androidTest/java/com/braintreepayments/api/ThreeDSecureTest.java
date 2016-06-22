package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.support.test.runner.AndroidJUnit4;

import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.AnalyticsConfiguration;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.ThreeDSecureAuthenticationResponse;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.api.threedsecure.ThreeDSecureWebViewActivity;
import com.braintreepayments.testutils.BraintreeActivityTestRule;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.api.BraintreeFragmentTestUtils.getFragment;
import static com.braintreepayments.api.BraintreeFragmentTestUtils.getMockFragment;
import static com.braintreepayments.api.BraintreeFragmentTestUtils.tokenize;
import static com.braintreepayments.testutils.Assertions.assertIsANonce;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class ThreeDSecureTest {

    @Rule
    public final BraintreeActivityTestRule<TestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    private Activity mActivity;
    private CountDownLatch mCountDownLatch;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
        mCountDownLatch = new CountDownLatch(1);
    }

    @Test(timeout = 10000)
    public void performVerification_postsPaymentMethodNonceToListenersWhenLookupReturnsACard()
            throws InterruptedException, InvalidArgumentException {
        String clientToken = new TestClientTokenBuilder().withThreeDSecure().build();
        BraintreeFragment fragment = getFragment(mActivity, clientToken);
        String nonce = tokenize(fragment, new CardBuilder()
                .cardNumber("4000000000000051")
                .expirationDate("12/20")).getNonce();
        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                assertIsANonce(paymentMethodNonce.getNonce());
                assertEquals("51", ((CardNonce) paymentMethodNonce).getLastTwo());
                mCountDownLatch.countDown();
            }
        });

        ThreeDSecure.performVerification(fragment, nonce, "5");

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    public void performVerification_acceptsACardBuilderAndPostsAPaymentMethodNonceToListener()
            throws InterruptedException {
        String clientToken = new TestClientTokenBuilder().withThreeDSecure().build();
        BraintreeFragment fragment = getFragment(mActivity, clientToken);
        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                assertIsANonce(paymentMethodNonce.getNonce());
                assertEquals("51", ((CardNonce) paymentMethodNonce).getLastTwo());
                mCountDownLatch.countDown();
            }
        });
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber("4000000000000051")
                .expirationDate("12/20");

        ThreeDSecure.performVerification(fragment, cardBuilder, "5");

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    public void onActivityResult_postsPaymentMethodNonceToListener() throws JSONException, InterruptedException {
        Configuration configuration = mock(Configuration.class);
        when(configuration.getAnalytics()).thenReturn(mock(AnalyticsConfiguration.class));
        BraintreeFragment fragment = getMockFragment(mActivity, configuration);
        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                assertIsANonce(paymentMethodNonce.getNonce());
                assertEquals("11", ((CardNonce) paymentMethodNonce).getLastTwo());
                mCountDownLatch.countDown();
            }
        });
        JSONObject authResponse = new JSONObject(
                stringFromFixture("three_d_secure/authentication_response.json"));
        Intent data = new Intent()
                .putExtra(ThreeDSecureWebViewActivity.EXTRA_THREE_D_SECURE_RESULT,
                        ThreeDSecureAuthenticationResponse.fromJson(authResponse.toString()));

        ThreeDSecure.onActivityResult(fragment, Activity.RESULT_OK, data);

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    public void onActivityResult_postsUnrecoverableErrorsToListeners() throws InterruptedException {
        Configuration configuration = mock(Configuration.class);
        when(configuration.getAnalytics()).thenReturn(mock(AnalyticsConfiguration.class));
        BraintreeFragment fragment = getMockFragment(mActivity, configuration);
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertEquals("Error!", error.getMessage());
                mCountDownLatch.countDown();
            }
        });
        ThreeDSecureAuthenticationResponse authResponse =
                ThreeDSecureAuthenticationResponse.fromException("Error!");
        Intent data = new Intent()
                .putExtra(ThreeDSecureWebViewActivity.EXTRA_THREE_D_SECURE_RESULT, authResponse);

        ThreeDSecure.onActivityResult(fragment, Activity.RESULT_OK, data);

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    public void onActivityResult_postsRecoverableErrorsToListener() throws InterruptedException {
        Configuration configuration = mock(Configuration.class);
        when(configuration.getAnalytics()).thenReturn(mock(AnalyticsConfiguration.class));
        BraintreeFragment fragment = getMockFragment(mActivity, configuration);
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertEquals("Failed to authenticate, please try a different form of payment",
                        error.getMessage());
                mCountDownLatch.countDown();
            }
        });
        Intent data = new Intent()
                .putExtra(ThreeDSecureWebViewActivity.EXTRA_THREE_D_SECURE_RESULT,
                        ThreeDSecureAuthenticationResponse.fromJson(
                                stringFromFixture("errors/three_d_secure_error.json")));

        ThreeDSecure.onActivityResult(fragment, Activity.RESULT_OK, data);

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    public void onActivityResult_doesNothingWhenResultCodeNotOk() {
        Intent intent = mock(Intent.class);

        ThreeDSecure.onActivityResult(mock(BraintreeFragment.class), Activity.RESULT_CANCELED,
                intent);

        verifyZeroInteractions(intent);
    }
}
