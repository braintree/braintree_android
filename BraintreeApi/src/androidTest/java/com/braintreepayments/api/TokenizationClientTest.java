package com.braintreepayments.api;

import android.app.Activity;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.PaymentMethodResponseCallback;
import com.braintreepayments.api.interfaces.PaymentMethodsUpdatedListener;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.AndroidPayCard;
import com.braintreepayments.api.models.Card;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.ClientKey;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PayPalAccountBuilder;
import com.braintreepayments.api.models.PaymentMethod;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.testutils.FixturesHelper;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.api.BraintreeFragmentTestUtils.getFragment;
import static com.braintreepayments.api.BraintreeFragmentTestUtils.getMockFragment;
import static com.braintreepayments.api.BraintreeFragmentTestUtils.tokenize;
import static com.braintreepayments.testutils.CardNumber.VISA;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static com.braintreepayments.testutils.TestClientKey.CLIENT_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class TokenizationClientTest {

    @Rule
    public final ActivityTestRule<TestActivity> mActivityTestRule =
            new ActivityTestRule<>(TestActivity.class);

    private Activity mActivity;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void getPaymentMethods_returnsAnEmptyListIfEmpty() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        BraintreeFragment fragment = getFragment(mActivity, new TestClientTokenBuilder().build());
        fragment.addListener(new PaymentMethodsUpdatedListener() {
            @Override
            public void onPaymentMethodsUpdated(List<PaymentMethod> paymentMethods) {
                assertEquals(0, paymentMethods.size());
                latch.countDown();
            }
        });

        TokenizationClient.getPaymentMethods(fragment);

        latch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void getPaymentMethods_throwsAnError() throws ErrorWithResponse,
            BraintreeException, InterruptedException, InvalidArgumentException {
        final CountDownLatch latch = new CountDownLatch(1);
        BraintreeFragment fragment = getMockFragment(mActivity, mock(Configuration.class));
        when(fragment.getHttpClient()).thenReturn(new BraintreeHttpClient(ClientKey.fromString(CLIENT_KEY)) {
            @Override
            public void get(String path, HttpResponseCallback callback) {
                callback.failure(new UnexpectedException("Mock"));
            }
        });
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onUnrecoverableError(Throwable throwable) {
                assertTrue(throwable instanceof UnexpectedException);
                assertEquals("Mock", throwable.getMessage());
                latch.countDown();
            }

            @Override
            public void onRecoverableError(ErrorWithResponse error) {}
        });

        TokenizationClient.getPaymentMethods(fragment);

        latch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void getPaymentMethods_fetchesPaymentMethods()
            throws InvalidArgumentException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        BraintreeFragment fragment = getMockFragment(mActivity, mock(Configuration.class));
        when(fragment.getHttpClient()).thenReturn(new BraintreeHttpClient(ClientKey.fromString(CLIENT_KEY)) {
            @Override
            public void get(String path, HttpResponseCallback callback) {
                callback.success(
                        stringFromFixture("payment_methods/get_payment_methods_response.json"));
            }
        });
        fragment.addListener(new PaymentMethodsUpdatedListener() {
            @Override
            public void onPaymentMethodsUpdated(List<PaymentMethod> paymentMethods) {
                assertEquals(3, paymentMethods.size());
                assertEquals("11", ((Card) paymentMethods.get(0)).getLastTwo());
                assertEquals("PayPal", paymentMethods.get(1).getTypeLabel());
                assertEquals("11", ((AndroidPayCard) paymentMethods.get(2)).getLastTwo());

                assertEquals(3, paymentMethods.size());
                latch.countDown();
            }
        });

        TokenizationClient.getPaymentMethods(fragment);

        latch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void getPaymentMethods_getsPaymentMethodsFromServer() throws InterruptedException,
            InvalidArgumentException {
        final CountDownLatch latch = new CountDownLatch(1);
        final String clientToken = new TestClientTokenBuilder().build();
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, clientToken);
        fragment.addListener(new PaymentMethodsUpdatedListener() {
            @Override
            public void onPaymentMethodsUpdated(List<PaymentMethod> paymentMethods) {
                assertEquals(1, paymentMethods.size());
                assertEquals("11", ((Card) paymentMethods.get(0)).getLastTwo());
                latch.countDown();
            }
        });
        tokenize(fragment, new CardBuilder()
                .cardNumber(VISA)
                .expirationMonth("04")
                .expirationYear("17"));

        TokenizationClient.getPaymentMethods(fragment);

        latch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void getPaymentMethods_failsWithAClientKey() throws InterruptedException,
            InvalidArgumentException {
        final CountDownLatch latch = new CountDownLatch(1);
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, CLIENT_KEY);
        fragment.addListener(new PaymentMethodsUpdatedListener() {
            @Override
            public void onPaymentMethodsUpdated(List<PaymentMethod> paymentMethods) {
                fail("getPaymentMethods succeeded");
            }
        });
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onUnrecoverableError(Throwable throwable) {
                assertTrue(throwable instanceof AuthorizationException);
                assertEquals("Client key authorization not allowed for this endpoint. Please use an authentication method with upgraded permissions", throwable.getMessage());
                latch.countDown();
            }

            @Override
            public void onRecoverableError(ErrorWithResponse error) {}
        });
        tokenize(fragment, new CardBuilder()
                .cardNumber(VISA)
                .expirationMonth("04")
                .expirationYear("17"));

        TokenizationClient.getPaymentMethods(fragment);

        latch.await();
    }

    @Test//(timeout = 10000)
    @MediumTest
    public void tokenize_acceptsAPayPalAccount() throws InterruptedException, JSONException {
        // TODO: I think we're passing in bad credentials for OTC flow. Probably need to the stub

        final CountDownLatch latch = new CountDownLatch(1);
        JSONObject otcJson = new JSONObject(FixturesHelper.stringFromFixture("paypal_otc_response.json"));
        BraintreeFragment fragment = getFragment(mActivity, new TestClientTokenBuilder().withPayPal().build());
        PayPalAccountBuilder paypalAccountBuilder =
                new PayPalAccountBuilder()
                        .oneTouchCoreData(otcJson)
                        .clientMetadataId("client-metadata-id");

        TokenizationClient.tokenize(fragment, paypalAccountBuilder,
                new PaymentMethodResponseCallback() {
                    @Override
                    public void success(PaymentMethod paymentMethod) {
                        assertNotNull(paymentMethod.getNonce());
                        assertEquals("PayPal", paymentMethod.getTypeLabel());
                        latch.countDown();
                    }

                    @Override
                    public void failure(Exception exception) {
                        fail(exception.getMessage());
                    }
                });

        latch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void tokenize_tokenizesAPayPalAccountWithAClientKey() throws InterruptedException, JSONException {
        final CountDownLatch latch = new CountDownLatch(1);
        JSONObject otcJson = new JSONObject(FixturesHelper.stringFromFixture("paypal_otc_response.json"));
        BraintreeFragment fragment = getFragment(mActivity, CLIENT_KEY);
        PayPalAccountBuilder paypalAccountBuilder =
                new PayPalAccountBuilder().oneTouchCoreData(otcJson);

        TokenizationClient.tokenize(fragment, paypalAccountBuilder,
                new PaymentMethodResponseCallback() {
                    @Override
                    public void success(PaymentMethod paymentMethod) {
                        assertNotNull(paymentMethod.getNonce());
                        assertEquals("PayPal", paymentMethod.getTypeLabel());
                        latch.countDown();
                    }

                    @Override
                    public void failure(Exception exception) {
                        fail(exception.getMessage());
                    }
                });

        latch.await();
    }
}
