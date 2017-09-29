package com.braintreepayments.api;

import android.support.test.runner.AndroidJUnit4;

import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.BinData;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.test.BraintreeActivityTestRule;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.api.test.TestClientTokenBuilder;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.api.test.Assertions.assertIsANonce;
import static com.braintreepayments.testutils.CardNumber.VISA;
import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class CardTest {

    @Rule
    public final BraintreeActivityTestRule<TestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithACustomer() throws Exception {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20");

        assertTokenizationSuccessful(new TestClientTokenBuilder().build(), cardBuilder);
    }

    @Test
    public void tokenize_tokenizesACardWithoutACustomer() throws Exception {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20");

        assertTokenizationSuccessful(new TestClientTokenBuilder().withoutCustomer().build(), cardBuilder);
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
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivityTestRule.getActivity(), TOKENIZATION_KEY);
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertTrue(error instanceof AuthorizationException);
                assertEquals("Tokenization key authorization not allowed for this endpoint. Please use an authentication method with upgraded permissions",
                        error.getMessage());
                countDownLatch.countDown();
            }
        });
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20")
                .validate(true);

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
    public void tokenize_callsErrorCallbackForInvalidCvv() throws Exception {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20")
                .cvv("200");

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivityTestRule.getActivity(),
                new TestClientTokenBuilder().withCvvVerification().build());
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

    @Test(timeout = 10000)
    public void tokenize_callsErrorCallbackForInvalidPostalCode() throws Exception {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20")
                .postalCode("20000");

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivityTestRule.getActivity(),
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
                .countryName("United States")
                .countryCodeAlpha2("US")
                .countryCodeAlpha3("USA")
                .countryCodeNumeric("840");

        assertTokenizationSuccessful(new TestClientTokenBuilder().build(), cardBuilder);
    }

    private void assertTokenizationSuccessful(String authorization, CardBuilder cardBuilder) throws Exception {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivityTestRule.getActivity(), authorization);
        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                CardNonce cardNonce = (CardNonce) paymentMethodNonce;

                assertIsANonce(cardNonce.getNonce());
                assertEquals("11", cardNonce.getLastTwo());
                assertEquals(BinData.UNKNOWN, cardNonce.getBinData().getPrepaid());
                assertEquals(BinData.UNKNOWN, cardNonce.getBinData().getHealthcare());
                assertEquals(BinData.UNKNOWN, cardNonce.getBinData().getDebit());
                assertEquals(BinData.UNKNOWN, cardNonce.getBinData().getDurbinRegulated());
                assertEquals(BinData.UNKNOWN, cardNonce.getBinData().getCommercial());
                assertEquals(BinData.UNKNOWN, cardNonce.getBinData().getPayroll());
                assertEquals(BinData.UNKNOWN, cardNonce.getBinData().getIssuingBank());
                assertEquals(BinData.UNKNOWN, cardNonce.getBinData().getCountryOfIssuance());
                assertEquals(BinData.UNKNOWN, cardNonce.getBinData().getProductId());

                countDownLatch.countDown();
            }
        });

        Card.tokenize(fragment, cardBuilder);

        countDownLatch.await();
    }
}
