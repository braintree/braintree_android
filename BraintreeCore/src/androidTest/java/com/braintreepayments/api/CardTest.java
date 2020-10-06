package com.braintreepayments.api;

import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.BinData;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.test.BraintreeActivityTestRule;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.api.test.TestClientTokenBuilder;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.testutils.CardNumber.VISA;
import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

@RunWith(Parameterized.class)
public class CardTest {

    private static final String REST = "REST";
    private static final String GRAPHQL = "GRAPHQL";

    @Parameters(name="{0}")
    public static Collection requestProtocols() {
        return Arrays.asList(REST, GRAPHQL);
    }

    @Rule
    public final BraintreeActivityTestRule<TestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    private String mRequestProtocol;

    public CardTest(String requestProtocol) {
        mRequestProtocol = requestProtocol;
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithACustomer() throws Exception {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20");

        assertTokenizationSuccessful(new TestClientTokenBuilder().withCustomerId().build(), cardBuilder);
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithoutACustomer() throws Exception {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20");

        assertTokenizationSuccessful(new TestClientTokenBuilder().build(), cardBuilder);
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithValidateTrue() throws Exception {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20")
                .validate(true);

        assertTokenizationSuccessful(new TestClientTokenBuilder().build(), cardBuilder);
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithValidateFalse() throws Exception {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20")
                .validate(false);

        assertTokenizationSuccessful(new TestClientTokenBuilder().build(), cardBuilder);
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithATokenizationKey() throws Exception {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20");

        assertTokenizationSuccessful(TOKENIZATION_KEY, cardBuilder);
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithATokenizationKeyAndValidateFalse() throws Exception {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20")
                .validate(false);

        assertTokenizationSuccessful(TOKENIZATION_KEY, cardBuilder);
    }

    @Test(timeout = 10000)
    public void tokenize_failsWithTokenizationKeyAndValidateTrue() throws Exception {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20")
                .validate(true);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        BraintreeFragment fragment = setupBraintreeFragment(TOKENIZATION_KEY);
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertTrue(error instanceof AuthorizationException);

                if (mRequestProtocol.equals(GRAPHQL)) {
                    assertEquals("You are unauthorized to perform input validation with the provided authentication credentials.",
                            error.getMessage());
                } else {
                    assertEquals("Tokenization key authorization not allowed for this endpoint. Please use an " +
                            "authentication method with upgraded permissions", error.getMessage());
                }

                countDownLatch.countDown();
            }
        });

        Card.tokenize(fragment, cardBuilder);

        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithCvv() throws Exception {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20")
                .cvv("123");

        assertTokenizationSuccessful(new TestClientTokenBuilder().withCvvVerification().build(), cardBuilder);
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesCvvOnly() throws Exception {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        BraintreeFragment fragment = setupBraintreeFragment(TOKENIZATION_KEY);
        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                CardNonce cardNonce = (CardNonce) paymentMethodNonce;

                assertNotNull(cardNonce.getBinData());
                assertEquals("Unknown", cardNonce.getCardType());
                assertEquals("", cardNonce.getLastFour());
                assertEquals("", cardNonce.getLastTwo());
                assertNotNull(cardNonce.getThreeDSecureInfo());
                assertFalse(cardNonce.isDefault());
                assertEquals("", cardNonce.getDescription());
                assertNotNull(cardNonce.getNonce());

                countDownLatch.countDown();
            }
        });

        CardBuilder cardBuilder = new CardBuilder().cvv("123");
        Card.tokenize(fragment, cardBuilder);

        countDownLatch.await();
    }

    @Ignore("Sample merchant account is not set up for CVV verification")
    @Test(timeout = 10000)
    public void tokenize_callsErrorCallbackForInvalidCvv() throws Exception {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20")
                .cvv("200");

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        BraintreeFragment fragment = setupBraintreeFragment(new TestClientTokenBuilder().withCvvVerification().build());
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertEquals("CVV verification failed",
                        ((ErrorWithResponse) error).errorFor("creditCard").getFieldErrors().get(0).getMessage());
                countDownLatch.countDown();
            }
        });

        Card.tokenize(fragment, cardBuilder);

        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithPostalCode() throws Exception {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20")
                .cvv("123");

        assertTokenizationSuccessful(new TestClientTokenBuilder().withPostalCodeVerification().build(), cardBuilder);
    }

    @Ignore("Sample merchant account is not set up for postal code verification")
    @Test(timeout = 10000)
    public void tokenize_callsErrorCallbackForInvalidPostalCode() throws Exception {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20")
                .postalCode("20000");

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        BraintreeFragment fragment = setupBraintreeFragment(
                new TestClientTokenBuilder().withPostalCodeVerification().build());
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertEquals("Postal code verification failed",
                        ((ErrorWithResponse) error).errorFor("creditCard").errorFor("billingAddress")
                                .getFieldErrors().get(0).getMessage());
                countDownLatch.countDown();
            }
        });

        Card.tokenize(fragment, cardBuilder);

        countDownLatch.await();
    }

    @Test
    public void tokenize_whenInvalidCountryCode_callsErrorCallbackWithDetailedError() throws Exception {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20")
                .countryCode("ABC");

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        BraintreeFragment fragment = setupBraintreeFragment(new TestClientTokenBuilder().build());
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertEquals("Country code (alpha3) is not an accepted country",
                        ((ErrorWithResponse) error).errorFor("creditCard").errorFor("billingAddress")
                                .getFieldErrors().get(0).getMessage());
                countDownLatch.countDown();
            }
        });

        Card.tokenize(fragment, cardBuilder);

        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithACompleteBillingAddress() throws Exception {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20")
                .cvv("123")
                .cardholderName("Joe Smith")
                .firstName("Joe")
                .lastName("Smith")
                .company("Company")
                .streetAddress("1 Main St")
                .extendedAddress("Unit 1")
                .locality("Some Town")
                .postalCode("12345")
                .region("Some Region")
                .countryCode("USA");

        assertTokenizationSuccessful(new TestClientTokenBuilder().build(), cardBuilder);
    }

    private void assertTokenizationSuccessful(String authorization, CardBuilder cardBuilder) throws Exception {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        BraintreeFragment fragment = setupBraintreeFragment(authorization);
        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                CardNonce cardNonce = (CardNonce) paymentMethodNonce;

                assertNotNull(cardNonce.getNonce());
                assertEquals("Visa", cardNonce.getCardType());
                assertEquals("1111", cardNonce.getLastFour());
                assertEquals("11", cardNonce.getLastTwo());
                assertEquals("08", cardNonce.getExpirationMonth());
                assertEquals("20", cardNonce.getExpirationYear());
                assertEquals(BinData.UNKNOWN, cardNonce.getBinData().getPrepaid());
                assertEquals(BinData.UNKNOWN, cardNonce.getBinData().getHealthcare());
                assertEquals(BinData.UNKNOWN, cardNonce.getBinData().getDebit());
                assertEquals(BinData.UNKNOWN, cardNonce.getBinData().getDurbinRegulated());
                assertEquals(BinData.UNKNOWN, cardNonce.getBinData().getCommercial());
                assertEquals(BinData.UNKNOWN, cardNonce.getBinData().getPayroll());
                assertEquals(BinData.UNKNOWN, cardNonce.getBinData().getIssuingBank());
                assertEquals(BinData.UNKNOWN, cardNonce.getBinData().getCountryOfIssuance());
                assertEquals(BinData.UNKNOWN, cardNonce.getBinData().getProductId());
                assertFalse(cardNonce.getThreeDSecureInfo().wasVerified());

                countDownLatch.countDown();
            }
        });
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                throw new RuntimeException(error);
            }
        });

        Card.tokenize(fragment, cardBuilder);

        countDownLatch.await();
    }

    private BraintreeFragment setupBraintreeFragment(String authorization) throws Exception {
        final BraintreeFragment fragment = BraintreeFragment.newInstance(mActivityTestRule.getActivity(), authorization);
        final CountDownLatch latch = new CountDownLatch(1);

        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                if (mRequestProtocol.equals(REST)) {
                    try {
                        JSONObject configJson = new JSONObject(configuration.toJson());
                        configJson.remove("graphQL");
                        fragment.setConfiguration(Configuration.fromJson(configJson.toString()));
                    } catch (JSONException ignored) {}

                    assertFalse(fragment.getConfiguration().getGraphQL().isEnabled());
                } else if (mRequestProtocol.equals(GRAPHQL)) {
                    assertTrue(configuration.getGraphQL().isEnabled());
                }

                latch.countDown();
            }
        });

        latch.await();
        return fragment;
    }
}
