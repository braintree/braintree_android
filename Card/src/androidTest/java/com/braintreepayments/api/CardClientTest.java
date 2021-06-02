package com.braintreepayments.api;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.json.JSONException;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.api.CardNumber.VISA;
import static com.braintreepayments.api.Fixtures.TOKENIZATION_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

@RunWith(Parameterized.class)
public class CardClientTest {

    private static final String REST = "REST";
    private static final String GRAPHQL = "GRAPHQL";

    @Parameterized.Parameters(name = "{0}")
    public static Collection<String> requestProtocols() {
        return Arrays.asList(REST, GRAPHQL);
    }

    private final String requestProtocol;

    public CardClientTest(String requestProtocol) {
        this.requestProtocol = requestProtocol;
    }

    @Rule
    public final BraintreeActivityTestRule<TestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithACustomer() throws Exception {
        String authorization = new TestClientTokenBuilder().withCustomerId().build();
        overrideConfigurationCache(authorization, requestProtocol);

        Card card = new Card();
        card.setNumber(VISA);
        card.setExpirationDate("08/20");

        assertTokenizationSuccessful(authorization, card);
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithoutACustomer() throws Exception {
        String authorization = new TestClientTokenBuilder().build();
        overrideConfigurationCache(authorization, requestProtocol);

        Card card = new Card();
        card.setNumber(VISA);
        card.setExpirationDate("08/20");

        assertTokenizationSuccessful(authorization, card);
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithValidateTrue() throws Exception {
        String authorization = new TestClientTokenBuilder().build();
        overrideConfigurationCache(authorization, requestProtocol);

        Card card = new Card();
        card.setNumber(VISA);
        card.setExpirationDate("08/20");
        card.setShouldValidate(true);

        assertTokenizationSuccessful(authorization, card);
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithValidateFalse() throws Exception {
        String authorization = new TestClientTokenBuilder().build();
        overrideConfigurationCache(authorization, requestProtocol);

        Card card = new Card();
        card.setNumber(VISA);
        card.setExpirationDate("08/20");
        card.setShouldValidate(false);

        assertTokenizationSuccessful(authorization, card);
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithATokenizationKey() throws Exception {
        overrideConfigurationCache(TOKENIZATION_KEY, requestProtocol);

        Card card = new Card();
        card.setNumber(VISA);
        card.setExpirationDate("08/20");

        assertTokenizationSuccessful(TOKENIZATION_KEY, card);
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithATokenizationKeyAndValidateFalse() throws Exception {
        overrideConfigurationCache(TOKENIZATION_KEY, requestProtocol);

        Card card = new Card();
        card.setNumber(VISA);
        card.setExpirationDate("08/20");
        card.setShouldValidate(false);

        assertTokenizationSuccessful(TOKENIZATION_KEY, card);
    }

    @Test(timeout = 10000)
    public void tokenize_failsWithTokenizationKeyAndValidateTrue() throws Exception {
        overrideConfigurationCache(TOKENIZATION_KEY, requestProtocol);

        Card card = new Card();
        card.setNumber(VISA);
        card.setExpirationDate("08/20");
        card.setShouldValidate(true);

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        CardClient sut = setupCardClient(TOKENIZATION_KEY);
        sut.tokenize(card, new CardTokenizeCallback() {
            @Override
            public void onResult(CardNonce cardNonce, Exception error) {
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

        Card card = new Card();
        card.setNumber(VISA);
        card.setExpirationDate("08/20");
        card.setCvv("123");

        assertTokenizationSuccessful(authorization, card);
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesCvvOnly() throws Exception {
        overrideConfigurationCache(TOKENIZATION_KEY, requestProtocol);

        final CountDownLatch countDownLatch = new CountDownLatch(1);

        Card card = new Card();
        card.setCvv("123");

        CardClient sut = setupCardClient(TOKENIZATION_KEY);
        sut.tokenize(card, new CardTokenizeCallback() {
            @Override
            public void onResult(CardNonce cardNonce, Exception error) {

                assertNotNull(cardNonce.getBinData());
                assertEquals("Unknown", cardNonce.getCardType());
                assertEquals("", cardNonce.getLastFour());
                assertEquals("", cardNonce.getLastTwo());
                assertNotNull(cardNonce.getThreeDSecureInfo());
                assertFalse(cardNonce.isDefault());
                assertNotNull(cardNonce.getString());

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

        Card card = new Card();
        card.setNumber(VISA);
        card.setExpirationDate("08/20");
        card.setCvv("200");

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        CardClient sut = setupCardClient(authorization);
        sut.tokenize(card, new CardTokenizeCallback() {
            @Override
            public void onResult(CardNonce cardNonce, Exception error) {
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

        Card card = new Card();
        card.setNumber(VISA);
        card.setExpirationDate("08/20");
        card.setCvv("123");

        assertTokenizationSuccessful(authorization, card);
    }

    @Ignore("Sample merchant account is not set up for postal code verification")
    @Test(timeout = 10000)
    public void tokenize_callsErrorCallbackForInvalidPostalCode() throws Exception {
        String authorization = new TestClientTokenBuilder().withPostalCodeVerification().build();
        overrideConfigurationCache(authorization, requestProtocol);

        Card card = new Card();
        card.setNumber(VISA);
        card.setExpirationDate("08/20");
        card.setPostalCode("20000");

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        CardClient sut = setupCardClient(authorization);
        sut.tokenize(card, new CardTokenizeCallback() {
            @Override
            public void onResult(CardNonce cardNonce, Exception error) {
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

        Card card = new Card();
        card.setNumber(VISA);
        card.setExpirationDate("08/20");
        card.setCountryCode("ABC");
        card.setShouldValidate(true);

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        CardClient sut = setupCardClient(authorization);
        sut.tokenize(card, new CardTokenizeCallback() {
            @Override
            public void onResult(CardNonce cardNonce, Exception error) {
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

        Card card = new Card();
        card.setNumber(VISA);
        card.setExpirationDate("08/20");
        card.setCvv("123");
        card.setCardholderName("Joe Smith");
        card.setFirstName("Joe");
        card.setLastName("Smith");
        card.setCompany("Company");
        card.setStreetAddress("1 Main St");
        card.setExtendedAddress("Unit 1");
        card.setLocality("Some Town");
        card.setPostalCode("12345");
        card.setRegion("Some Region");
        card.setCountryCode("USA");

        assertTokenizationSuccessful(authorization, card);
    }

    private void assertTokenizationSuccessful(String authorization, Card card) throws Exception {
        BraintreeClient braintreeClient = new BraintreeClient(ApplicationProvider.getApplicationContext(), authorization);
        CardClient sut = new CardClient(braintreeClient);

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        sut.tokenize(card, new CardTokenizeCallback() {
            @Override
            public void onResult(CardNonce cardNonce, Exception error) {

                assertNotNull(cardNonce.getString());
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

    private CardClient setupCardClient(String authorization) throws Exception {
        BraintreeClient braintreeClient = new BraintreeClient(ApplicationProvider.getApplicationContext(), authorization);
        return new CardClient(braintreeClient);
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
