package com.braintreepayments.api;

import androidx.appcompat.app.AppCompatActivity;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceDeletedListener;
import com.braintreepayments.api.interfaces.PaymentMethodNoncesUpdatedListener;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.test.BraintreeActivityTestRule;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.api.test.TestClientTokenBuilder;
import com.braintreepayments.testutils.Fixtures;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static androidx.test.InstrumentationRegistry.getInstrumentation;
import static com.braintreepayments.api.BraintreeFragmentTestUtils.tokenize;
import static com.braintreepayments.testutils.Assertions.assertIsANonce;
import static com.braintreepayments.testutils.CardNumber.VISA;
import static com.braintreepayments.testutils.ExpirationDateHelper.validExpirationYear;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4ClassRunner.class)
public class PaymentMethodTest {

    @Rule
    public final BraintreeActivityTestRule<TestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    private AppCompatActivity mActivity;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
    }

    @Ignore("This test is passing when run individually, but not when run with other tests.")
    @Test(timeout = 10000)
    public void getPaymentMethodNonces_andDeletePaymentMethod_returnsCardNonce() throws InterruptedException, InvalidArgumentException {
        final CountDownLatch latch = new CountDownLatch(2);
        final String clientToken = new TestClientTokenBuilder().withCustomerId().build();
        final BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, clientToken);
        getInstrumentation().waitForIdleSync();

        fragment.addListener(new PaymentMethodNoncesUpdatedListener() {
            @Override
            public void onPaymentMethodNoncesUpdated(List<PaymentMethodNonce> paymentMethodNonces) {
                assertEquals(1, paymentMethodNonces.size());

                CardNonce cardNonce = (CardNonce) paymentMethodNonces.get(0);

                assertIsANonce(cardNonce.getNonce());
                assertEquals("11", cardNonce.getLastTwo());

                PaymentMethod.deletePaymentMethod(fragment, cardNonce);
                latch.countDown();
            }
        });

        fragment.addListener(new PaymentMethodNonceDeletedListener() {
            @Override
            public void onPaymentMethodNonceDeleted(PaymentMethodNonce paymentMethodNonce) {
                CardNonce cardNonce = (CardNonce) paymentMethodNonce;
                assertEquals("11", cardNonce.getLastTwo());
                latch.countDown();
            }
        });

        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                fail(error.getMessage());
            }
        });

        tokenize(fragment, new CardBuilder()
                .cardNumber(VISA)
                .expirationMonth("04")
                .expirationYear(validExpirationYear()));

        PaymentMethod.getPaymentMethodNonces(fragment);

        latch.await();
    }

    @Test(timeout = 10000)
    public void getPaymentMethodNonces_failsWithATokenizationKey() throws InterruptedException,
            InvalidArgumentException {
        final CountDownLatch latch = new CountDownLatch(1);
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, Fixtures.TOKENIZATION_KEY);
        getInstrumentation().waitForIdleSync();
        fragment.addListener(new PaymentMethodNoncesUpdatedListener() {
            @Override
            public void onPaymentMethodNoncesUpdated(List<PaymentMethodNonce> paymentMethodNonces) {
                fail("getPaymentMethodNonces succeeded");
            }
        });
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertTrue(error instanceof AuthorizationException);
                assertEquals("Tokenization key authorization not allowed for this endpoint. Please use an authentication method with upgraded permissions",
                        error.getMessage());
                latch.countDown();
            }
        });
        tokenize(fragment, new CardBuilder()
                .cardNumber(VISA)
                .expirationMonth("04")
                .expirationYear(validExpirationYear()));

        PaymentMethod.getPaymentMethodNonces(fragment);

        latch.await();
    }
}
