package com.braintreepayments.api;

import android.app.Activity;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.MediumTest;

import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.PaymentMethodCreatedListener;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.AnalyticsConfiguration;
import com.braintreepayments.api.models.Card;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethod;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.api.BraintreeFragmentTestUtils.getMockFragment;
import static com.braintreepayments.api.BraintreeFragmentTestUtils.verifyAnalyticsEvent;
import static com.braintreepayments.api.internal.BraintreeHttpClientTestUtils.clientWithExpectedResponse;
import static com.braintreepayments.testutils.CardNumber.VISA;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static com.braintreepayments.testutils.TestClientKey.CLIENT_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class CardTokenizationTest {

    @Rule
    public final ActivityTestRule<TestActivity> mActivityTestRule =
            new ActivityTestRule<>(TestActivity.class);

    private Activity mActivity;
    private BraintreeFragment mBraintreeFragment;
    private CountDownLatch mCountDownLatch;

    @Before
    public void setUp() throws InvalidArgumentException {
        mActivity = mActivityTestRule.getActivity();
        mCountDownLatch = new CountDownLatch(1);
    }

    @Test(timeout = 10000)
    @MediumTest
    public void tokenize_tokenizesACard() throws InvalidArgumentException, InterruptedException {
        setupFragment();
        mBraintreeFragment.addListener(new PaymentMethodCreatedListener() {
            @Override
            public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
                assertEquals("11", ((Card) paymentMethod).getLastTwo());
                mCountDownLatch.countDown();
            }
        });
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20");

        PaymentMethodTokenizer.tokenize(mBraintreeFragment, cardBuilder);

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void tokenize_sendsAnalyticsEventOnSuccess()
            throws InvalidArgumentException, InterruptedException, IOException, ErrorWithResponse {
        AnalyticsConfiguration analyticsConfiguration = mock(AnalyticsConfiguration.class);
        when(analyticsConfiguration.isEnabled()).thenReturn(true);
        Configuration configuration = mock(Configuration.class);
        when(configuration.getAnalytics()).thenReturn(analyticsConfiguration);
        mBraintreeFragment = getMockFragment(mActivity, configuration);
        BraintreeHttpClient httpClient = clientWithExpectedResponse(201,
                stringFromFixture("payment_methods/visa_credit_card_response.json"));
        when(mBraintreeFragment.getHttpClient()).thenReturn(httpClient);
        mBraintreeFragment.addListener(new PaymentMethodCreatedListener() {
            @Override
            public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
                mCountDownLatch.countDown();
            }
        });
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20");

        PaymentMethodTokenizer.tokenize(mBraintreeFragment, cardBuilder);

        mCountDownLatch.await();
        verifyAnalyticsEvent(mBraintreeFragment, "card.nonce-received");
    }

    @Test(timeout = 10000)
    @MediumTest
    public void tokenize_tokenizesACardWithAClientKey() throws InvalidArgumentException, InterruptedException {
        setupFragment();
        mBraintreeFragment = BraintreeFragment.newInstance(mActivityTestRule.getActivity(), CLIENT_KEY);
        mBraintreeFragment.addListener(new PaymentMethodCreatedListener() {
            @Override
            public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
                assertEquals("11", ((Card) paymentMethod).getLastTwo());
                mCountDownLatch.countDown();
            }
        });
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20");

        PaymentMethodTokenizer.tokenize(mBraintreeFragment, cardBuilder);

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void tokenize_callsListenerWithErrorOnFailure()
            throws InterruptedException, InvalidArgumentException {
        setupFragment();
        mBraintreeFragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onUnrecoverableError(Throwable throwable) {}

            @Override
            public void onRecoverableError(ErrorWithResponse error) {
                assertEquals(422, error.getStatusCode());
                assertNotNull(error.getFieldErrors());
                assertEquals("Credit card is invalid", error.getMessage());
                assertEquals(1, error.getFieldErrors().size());
                assertEquals(3, error.errorFor("creditCard").getFieldErrors().size());
                assertEquals(
                        "Credit card must include number, payment_method_nonce, or venmo_sdk_payment_method_code",
                        error.errorFor("creditCard").errorFor("base").getMessage());
                assertEquals("Expiration year is invalid",
                        error.errorFor("creditCard").errorFor("expirationYear").getMessage());
                assertEquals("Credit card number is required",
                        error.errorFor("creditCard").errorFor("number").getMessage());
                assertNull(error.errorFor("creditCard").errorFor("expirationMonth"));
                mCountDownLatch.countDown();
            }
        });

        PaymentMethodTokenizer.tokenize(mBraintreeFragment, new CardBuilder().expirationMonth("01"));

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void tokenize_sendsAnalyticsEventOnFailure()
            throws InterruptedException, InvalidArgumentException, IOException, ErrorWithResponse {
        AnalyticsConfiguration analyticsConfiguration = mock(AnalyticsConfiguration.class);
        when(analyticsConfiguration.isEnabled()).thenReturn(true);
        Configuration configuration = mock(Configuration.class);
        when(configuration.getAnalytics()).thenReturn(analyticsConfiguration);
        mBraintreeFragment = getMockFragment(mActivity, configuration);
        BraintreeHttpClient httpClient = clientWithExpectedResponse(422, "");
        when(mBraintreeFragment.getHttpClient()).thenReturn(httpClient);
        mBraintreeFragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onUnrecoverableError(Throwable throwable) {}

            @Override
            public void onRecoverableError(ErrorWithResponse error) {
                mCountDownLatch.countDown();
            }
        });

        PaymentMethodTokenizer.tokenize(mBraintreeFragment, new CardBuilder().expirationMonth("01"));

        mCountDownLatch.await();
        verifyAnalyticsEvent(mBraintreeFragment, "card.nonce-failed");
    }

    /* helpers */
    private void setupFragment() throws InvalidArgumentException {
        mBraintreeFragment = BraintreeFragment.newInstance(mActivity,
                new TestClientTokenBuilder().build());
    }
}
