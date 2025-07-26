package com.braintreepayments.api.card;

import static com.braintreepayments.api.testutils.CardNumber.VISA;
import static com.braintreepayments.api.testutils.Fixtures.TOKENIZATION_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.braintreepayments.api.core.Authorization;
import com.braintreepayments.api.core.Configuration;
import com.braintreepayments.api.core.ErrorWithResponse;
import com.braintreepayments.api.sharedutils.AuthorizationException;
import com.braintreepayments.api.testutils.ExpirationDateHelper;
import com.braintreepayments.api.testutils.Fixtures;
import com.braintreepayments.api.testutils.SharedPreferencesHelper;
import com.braintreepayments.api.testutils.TestClientTokenBuilder;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

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

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithACustomer() throws Exception {
        String authorization = new TestClientTokenBuilder().withCustomerId().build();
        overrideConfigurationCache(authorization, requestProtocol);

        Card card = new Card();
        card.setNumber(VISA);
        card.setExpirationMonth("08");
        card.setExpirationYear(ExpirationDateHelper.validExpirationYear());

        assertTokenizationSuccessful(authorization, card);
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithoutACustomer() throws Exception {
        String authorization = new TestClientTokenBuilder().build();
        overrideConfigurationCache(authorization, requestProtocol);

        Card card = new Card();
        card.setNumber(VISA);
        card.setExpirationMonth("08");
        card.setExpirationYear(ExpirationDateHelper.validExpirationYear());

        assertTokenizationSuccessful(authorization, card);
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithValidateTrue() throws Exception {
        String authorization = new TestClientTokenBuilder().build();
        overrideConfigurationCache(authorization, requestProtocol);

        Card card = new Card();
        card.setNumber(VISA);
        card.setShouldValidate(true);
        card.setExpirationMonth("08");
        card.setExpirationYear(ExpirationDateHelper.validExpirationYear());

        assertTokenizationSuccessful(authorization, card);
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithValidateFalse() throws Exception {
        String authorization = new TestClientTokenBuilder().build();
        overrideConfigurationCache(authorization, requestProtocol);

        Card card = new Card();
        card.setNumber(VISA);
        card.setShouldValidate(false);
        card.setExpirationMonth("08");
        card.setExpirationYear(ExpirationDateHelper.validExpirationYear());

        assertTokenizationSuccessful(authorization, card);
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithATokenizationKey() throws Exception {
        overrideConfigurationCache(TOKENIZATION_KEY, requestProtocol);

        Card card = new Card();
        card.setNumber(VISA);
        card.setExpirationMonth("08");
        card.setExpirationYear(ExpirationDateHelper.validExpirationYear());

        assertTokenizationSuccessful(TOKENIZATION_KEY, card);
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithATokenizationKeyAndValidateFalse() throws Exception {
        overrideConfigurationCache(TOKENIZATION_KEY, requestProtocol);

        Card card = new Card();
        card.setNumber(VISA);
        card.setShouldValidate(false);
        card.setExpirationMonth("08");
        card.setExpirationYear(ExpirationDateHelper.validExpirationYear());

        assertTokenizationSuccessful(TOKENIZATION_KEY, card);
    }

    @Test(timeout = 10000)
    public void tokenize_failsWithTokenizationKeyAndValidateTrue() throws Exception {
        overrideConfigurationCache(TOKENIZATION_KEY, requestProtocol);

        Card card = new Card();
        card.setNumber(VISA);
        card.setShouldValidate(true);
        card.setExpirationMonth("08");
        card.setExpirationYear(ExpirationDateHelper.validExpirationYear());

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        CardClient sut = setupCardClient(TOKENIZATION_KEY);
        sut.tokenize(card, cardResult -> {
            assertTrue(cardResult instanceof CardResult.Failure);
            Exception error = ((CardResult.Failure) cardResult).getError();
            assertTrue(error instanceof AuthorizationException);

            if (requestProtocol.equals(GRAPHQL)) {
                assertEquals("You are unauthorized to perform input validation with the provided authentication credentials.",
                        error.getMessage());
            } else {
                assertEquals("Tokenization key authorization not allowed for this endpoint. Please use an " +
                        "authentication method with upgraded permissions", error.getMessage());
            }

            countDownLatch.countDown();
        });

        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithCvv() throws Exception {
        String authorization = new TestClientTokenBuilder().withCvvVerification().build();
        overrideConfigurationCache(authorization, requestProtocol);

        Card card = new Card();
        card.setNumber(VISA);
        card.setCvv("123");
        card.setExpirationMonth("08");
        card.setExpirationYear(ExpirationDateHelper.validExpirationYear());

        assertTokenizationSuccessful(authorization, card);
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesCvvOnly() throws Exception {
        overrideConfigurationCache(TOKENIZATION_KEY, requestProtocol);

        final CountDownLatch countDownLatch = new CountDownLatch(1);

        Card card = new Card();
        card.setCvv("123");
        card.setExpirationMonth("08");
        card.setExpirationYear(ExpirationDateHelper.validExpirationYear());

        CardClient sut = setupCardClient(TOKENIZATION_KEY);
        sut.tokenize(card, cardResult -> {
            assertTrue(cardResult instanceof CardResult.Success);
            CardNonce cardNonce = ((CardResult.Success) cardResult).getNonce();

            assertNotNull(cardNonce.getBinData());
            assertEquals("Unknown", cardNonce.getCardType());
            assertEquals("", cardNonce.getLastFour());
            assertEquals("", cardNonce.getLastTwo());
            assertFalse(cardNonce.isDefault());
            assertNotNull(cardNonce.getString());

            countDownLatch.countDown();
        });

        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithPostalCode() throws Exception {
        String authorization = new TestClientTokenBuilder().withPostalCodeVerification().build();
        overrideConfigurationCache(authorization, requestProtocol);

        Card card = new Card();
        card.setNumber(VISA);
        card.setCvv("123");
        card.setExpirationMonth("08");
        card.setExpirationYear(ExpirationDateHelper.validExpirationYear());

        assertTokenizationSuccessful(authorization, card);
    }

    @Test
    public void tokenize_whenInvalidCountryCode_callsErrorCallbackWithDetailedError() throws Exception {
        String authorization = new TestClientTokenBuilder().build();
        overrideConfigurationCache(authorization, requestProtocol);

        Card card = new Card();
        card.setNumber(VISA);
        card.setCountryCode("ABC");
        card.setShouldValidate(true);
        card.setExpirationMonth("08");
        card.setExpirationYear(ExpirationDateHelper.validExpirationYear());

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        CardClient sut = setupCardClient(authorization);
        sut.tokenize(card, (cardResult) -> {
            assertTrue(cardResult instanceof CardResult.Failure);
            Exception error = ((CardResult.Failure) cardResult).getError();
            assertEquals("Country code (alpha3) is not an accepted country",
                    ((ErrorWithResponse) error).errorFor("creditCard").errorFor("billingAddress")
                            .getFieldErrors().get(0).getMessage());
            countDownLatch.countDown();
        });

        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithACompleteBillingAddress() throws Exception {
        String authorization = new TestClientTokenBuilder().build();
        overrideConfigurationCache(authorization, requestProtocol);

        Card card = new Card();
        card.setNumber(VISA);
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
        card.setExpirationMonth("08");
        card.setExpirationYear(ExpirationDateHelper.validExpirationYear());

        assertTokenizationSuccessful(authorization, card);
    }

    private void assertTokenizationSuccessful(String authorization, Card card) throws Exception {
        CardClient sut = setupCardClient(authorization);

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        sut.tokenize(card, (cardResult) -> {

            assertTrue(cardResult instanceof CardResult.Success);
            CardNonce cardNonce = ((CardResult.Success) cardResult).getNonce();
            assertNotNull(cardNonce.getString());
            assertEquals("Visa", cardNonce.getCardType());
            assertEquals("1111", cardNonce.getLastFour());
            assertEquals("11", cardNonce.getLastTwo());
            assertEquals(BinType.Unknown, cardNonce.getBinData().getPrepaid());
            assertEquals(BinType.Unknown, cardNonce.getBinData().getHealthcare());
            assertEquals(BinType.Unknown, cardNonce.getBinData().getDebit());
            assertEquals(BinType.Unknown, cardNonce.getBinData().getDurbinRegulated());
            assertEquals(BinType.Unknown, cardNonce.getBinData().getCommercial());
            assertEquals(BinType.Unknown, cardNonce.getBinData().getPayroll());
            assertEquals(BinType.Unknown.name(), cardNonce.getBinData().getIssuingBank());
            assertEquals(BinType.Unknown.name(), cardNonce.getBinData().getCountryOfIssuance());
            assertEquals(BinType.Unknown.name(), cardNonce.getBinData().getProductId());

            countDownLatch.countDown();
        });

        countDownLatch.await();
    }

    private CardClient setupCardClient(String authorization) {
        return new CardClient(ApplicationProvider.getApplicationContext(), authorization);
    }

    private static void overrideConfigurationCache(String authString, String requestProtocol) throws JSONException {
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
