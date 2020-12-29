package com.braintreepayments.api;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.BinData;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.test.BraintreeActivityTestRule;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.api.test.TestClientTokenBuilder;
import com.braintreepayments.testutils.Fixtures;
import com.braintreepayments.testutils.SharedPreferencesHelper;

import org.json.JSONException;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.testutils.CardNumber.VISA;
import static com.braintreepayments.testutils.Fixtures.TOKENIZATION_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

@RunWith(Parameterized.class)
public class CardTest {

    private static final String REST = "REST";
    private static final String GRAPHQL = "GRAPHQL";

    @Parameterized.Parameters(name="{0}")
    public static Collection<String> requestProtocols() {
        return Arrays.asList(REST, GRAPHQL);
    }

    private final String requestProtocol;

    public CardTest(String requestProtocol) {
        this.requestProtocol = requestProtocol;
    }

    @Rule
    public final BraintreeActivityTestRule<TestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithACustomer() throws Exception {
        String authorization = new TestClientTokenBuilder().withCustomerId().build();
        overrideConfigurationCache(authorization, requestProtocol);

        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20");

        assertTokenizationSuccessful(authorization, cardBuilder);
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithoutACustomer() throws Exception {
        String authorization = new TestClientTokenBuilder().build();
        overrideConfigurationCache(authorization, requestProtocol);

        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20");

        assertTokenizationSuccessful(authorization, cardBuilder);
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithValidateTrue() throws Exception {
        String authorization = new TestClientTokenBuilder().build();
        overrideConfigurationCache(authorization, requestProtocol);

        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20")
                .validate(true);

        assertTokenizationSuccessful(authorization, cardBuilder);
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithValidateFalse() throws Exception {
        String authorization = new TestClientTokenBuilder().build();
        overrideConfigurationCache(authorization, requestProtocol);

        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20")
                .validate(false);

        assertTokenizationSuccessful(authorization, cardBuilder);
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithATokenizationKey() throws Exception {
        overrideConfigurationCache(TOKENIZATION_KEY, requestProtocol);

        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20");

        assertTokenizationSuccessful(TOKENIZATION_KEY, cardBuilder);
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithATokenizationKeyAndValidateFalse() throws Exception {
        overrideConfigurationCache(TOKENIZATION_KEY, requestProtocol);

        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20")
                .validate(false);

        assertTokenizationSuccessful(TOKENIZATION_KEY, cardBuilder);
    }

    @Test(timeout = 10000)
    public void tokenize_failsWithTokenizationKeyAndValidateTrue() throws Exception {
        overrideConfigurationCache(TOKENIZATION_KEY, requestProtocol);

        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20")
                .validate(true);

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Card sut = setupCardClient(TOKENIZATION_KEY);
        sut.tokenize(mActivityTestRule.getActivity(), cardBuilder, new CardTokenizeCallback() {
            @Override
            public void onResult(PaymentMethodNonce paymentMethodNonce, Exception error) {
                assertTrue(error instanceof AuthorizationException);

                if (requestProtocol.equals(GRAPHQL)) {
                    assertEquals("You are unauthorized to perform input validation with the provided authentication credentials.",
                            error.getMessage());
                } else {
                    assertEquals("Tokenization key authorization not allowed for this endpoint. Please use an " +
                            "authentication method with upgraded permissions", error.getMessage());
                }

                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithCvv() throws Exception {
        String authorization = new TestClientTokenBuilder().withCvvVerification().build();
        overrideConfigurationCache(authorization, requestProtocol);

        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20")
                .cvv("123");

        assertTokenizationSuccessful(authorization, cardBuilder);
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesCvvOnly() throws Exception {
        overrideConfigurationCache(TOKENIZATION_KEY, requestProtocol);

        final CountDownLatch countDownLatch = new CountDownLatch(1);

        CardBuilder cardBuilder = new CardBuilder().cvv("123");
        Card sut = setupCardClient(TOKENIZATION_KEY);
        sut.tokenize(mActivityTestRule.getActivity(), cardBuilder, new CardTokenizeCallback() {
            @Override
            public void onResult(PaymentMethodNonce paymentMethodNonce, Exception error) {
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

        countDownLatch.await();
    }

    @Ignore("Sample merchant account is not set up for CVV verification")
    @Test(timeout = 10000)
    public void tokenize_callsErrorCallbackForInvalidCvv() throws Exception {
        String authorization = new TestClientTokenBuilder().withCvvVerification().build();
        overrideConfigurationCache(authorization, requestProtocol);

        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20")
                .cvv("200");

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Card sut = setupCardClient(authorization);
        sut.tokenize(mActivityTestRule.getActivity(), cardBuilder, new CardTokenizeCallback() {
            @Override
            public void onResult(PaymentMethodNonce paymentMethodNonce, Exception error) {
                assertEquals("CVV verification failed",
                        ((ErrorWithResponse) error).errorFor("creditCard").getFieldErrors().get(0).getMessage());
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithPostalCode() throws Exception {
        String authorization = new TestClientTokenBuilder().withPostalCodeVerification().build();
        overrideConfigurationCache(authorization, requestProtocol);

        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20")
                .cvv("123");

        assertTokenizationSuccessful(authorization, cardBuilder);
    }

    @Ignore("Sample merchant account is not set up for postal code verification")
    @Test(timeout = 10000)
    public void tokenize_callsErrorCallbackForInvalidPostalCode() throws Exception {
        String authorization = new TestClientTokenBuilder().withPostalCodeVerification().build();
        overrideConfigurationCache(authorization, requestProtocol);

        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20")
                .postalCode("20000");

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Card sut = setupCardClient(authorization);
        sut.tokenize(mActivityTestRule.getActivity(), cardBuilder, new CardTokenizeCallback() {
            @Override
            public void onResult(PaymentMethodNonce paymentMethodNonce, Exception error) {
                assertEquals("Postal code verification failed",
                        ((ErrorWithResponse) error).errorFor("creditCard").errorFor("billingAddress")
                                .getFieldErrors().get(0).getMessage());
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }

    @Test
    public void tokenize_whenInvalidCountryCode_callsErrorCallbackWithDetailedError() throws Exception {
        String authorization = new TestClientTokenBuilder().build();
        overrideConfigurationCache(authorization, requestProtocol);

        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20")
                .countryCode("ABC");

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Card sut = setupCardClient(authorization);
        sut.tokenize(mActivityTestRule.getActivity(), cardBuilder, new CardTokenizeCallback() {
            @Override
            public void onResult(PaymentMethodNonce paymentMethodNonce, Exception error) {
                assertEquals("Country code (alpha3) is not an accepted country",
                        ((ErrorWithResponse) error).errorFor("creditCard").errorFor("billingAddress")
                                .getFieldErrors().get(0).getMessage());
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithACompleteBillingAddress() throws Exception {
        String authorization = new TestClientTokenBuilder().build();
        overrideConfigurationCache(authorization, requestProtocol);

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

        assertTokenizationSuccessful(authorization, cardBuilder);
    }

    private void assertTokenizationSuccessful(String authorization, CardBuilder cardBuilder) throws Exception {
        BraintreeClient braintreeClient = new BraintreeClient(Authorization.fromString(authorization), null);
        Card sut = new Card(braintreeClient);

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        sut.tokenize(mActivityTestRule.getActivity(), cardBuilder, new CardTokenizeCallback() {
            @Override
            public void onResult(PaymentMethodNonce paymentMethodNonce, Exception error) {
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

        countDownLatch.await();
    }

    private Card setupCardClient(String authorization) throws Exception {
        BraintreeClient braintreeClient = new BraintreeClient(Authorization.fromString(authorization), null);
        return new Card(braintreeClient);
    }

    private static void overrideConfigurationCache(String authString, String requestProtocol) throws JSONException, InvalidArgumentException {
        Authorization authorization = Authorization.fromString(authString);

        Configuration configuration;
        if (requestProtocol.equalsIgnoreCase(GRAPHQL)) {
            configuration = Configuration.fromJson(Fixtures.SANDBOX_CONFIGURATION_WITH_GRAPHQL);
        } else {
            configuration = Configuration.fromJson(Fixtures.SANDBOX_CONFIGURATION_WITHOUT_GRAPHQL);
        }

        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferencesHelper.overrideConfigurationCache(context, authorization, configuration);
    }
}
