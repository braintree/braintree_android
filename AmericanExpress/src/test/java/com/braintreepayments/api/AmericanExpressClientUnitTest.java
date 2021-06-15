package com.braintreepayments.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class AmericanExpressClientUnitTest {

    private AmericanExpressGetRewardsBalanceCallback amexRewardsCallback;

    @Before
    public void beforeEach() {
        amexRewardsCallback = mock(AmericanExpressGetRewardsBalanceCallback.class);
    }

    @Test
    public void getRewardsBalance_sendsGETRequestForAmexAwardsBalance() {
        BraintreeClient braintreeClient = mock(BraintreeClient.class);
        AmericanExpressClient sut = new AmericanExpressClient(braintreeClient);
        sut.getRewardsBalance("fake-nonce", "USD", amexRewardsCallback);

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendGET(urlCaptor.capture(), any(HTTPResponseCallback.class));

        String url = urlCaptor.getValue();
        assertEquals("/v1/payment_methods/amex_rewards_balance?paymentMethodNonce=fake-nonce&currencyIsoCode=USD", url);
    }

    @Test
    public void getRewardsBalance_callsListenerWithRewardsBalanceOnSuccess() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendGETSuccessfulResponse(Fixtures.AMEX_REWARDS_BALANCE_SUCCESS)
                .build();

        AmericanExpressClient sut = new AmericanExpressClient(braintreeClient);
        sut.getRewardsBalance("fake-nonce", "USD", amexRewardsCallback);

        ArgumentCaptor<AmericanExpressRewardsBalance> amexRewardsCaptor =
            ArgumentCaptor.forClass(AmericanExpressRewardsBalance.class);
        verify(amexRewardsCallback).onResult(amexRewardsCaptor.capture(), (Exception) isNull());

        AmericanExpressRewardsBalance rewardsBalance = amexRewardsCaptor.getValue();
        assertNotNull(rewardsBalance);
        assertEquals("0.0070", rewardsBalance.getConversionRate());
        assertEquals("316795.03", rewardsBalance.getCurrencyAmount());
        assertEquals("USD", rewardsBalance.getCurrencyIsoCode());
        assertEquals("715f4712-8690-49ed-8cc5-d7fb1c2d", rewardsBalance.getRequestId());
        assertEquals("45256433", rewardsBalance.getRewardsAmount());
        assertEquals("Points", rewardsBalance.getRewardsUnit());
        assertNull(rewardsBalance.getErrorCode());
        assertNull(rewardsBalance.getErrorMessage());
    }

    @Test
    public void getRewardsBalance_callsListenerWithRewardsBalanceWithErrorCode_OnIneligibleCard() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendGETSuccessfulResponse(Fixtures.AMEX_REWARDS_BALANCE_INELIGIBLE_CARD)
                .build();

        AmericanExpressClient sut = new AmericanExpressClient(braintreeClient);
        sut.getRewardsBalance("fake-nonce", "USD", amexRewardsCallback);

        ArgumentCaptor<AmericanExpressRewardsBalance> amexRewardsCaptor =
            ArgumentCaptor.forClass(AmericanExpressRewardsBalance.class);
        verify(amexRewardsCallback).onResult(amexRewardsCaptor.capture(), (Exception) isNull());

        AmericanExpressRewardsBalance rewardsBalance = amexRewardsCaptor.getValue();
        assertNotNull(rewardsBalance);
        assertNull(rewardsBalance.getConversionRate());
        assertNull(rewardsBalance.getCurrencyAmount());
        assertNull(rewardsBalance.getCurrencyIsoCode());
        assertNull(rewardsBalance.getRequestId());
        assertNull(rewardsBalance.getRewardsAmount());
        assertNull(rewardsBalance.getRewardsUnit());
        assertEquals("INQ2002", rewardsBalance.getErrorCode());
        assertEquals("Card is ineligible", rewardsBalance.getErrorMessage());
    }

    @Test
    public void getRewardsBalance_callsListenerWithRewardsBalanceWithErrorCode_OnInsufficientPoints() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendGETSuccessfulResponse(Fixtures.AMEX_REWARDS_BALANCE_INSUFFICIENT_POINTS)
                .build();

        AmericanExpressClient sut = new AmericanExpressClient(braintreeClient);
        sut.getRewardsBalance("fake-nonce", "USD", amexRewardsCallback);

        ArgumentCaptor<AmericanExpressRewardsBalance> amexRewardsCaptor =
            ArgumentCaptor.forClass(AmericanExpressRewardsBalance.class);
        verify(amexRewardsCallback).onResult(amexRewardsCaptor.capture(), (Exception) isNull());

        AmericanExpressRewardsBalance rewardsBalance = amexRewardsCaptor.getValue();
        assertNotNull(rewardsBalance);
        assertNull(rewardsBalance.getConversionRate());
        assertNull(rewardsBalance.getCurrencyAmount());
        assertNull(rewardsBalance.getCurrencyIsoCode());
        assertNull(rewardsBalance.getRequestId());
        assertNull(rewardsBalance.getRewardsAmount());
        assertNull(rewardsBalance.getRewardsUnit());
        assertEquals("INQ2003", rewardsBalance.getErrorCode());
        assertEquals("Insufficient points on card", rewardsBalance.getErrorMessage());
    }

    @Test
    public void getRewardsBalance_sendsAnalyticsEventOnSuccess() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendGETSuccessfulResponse(Fixtures.AMEX_REWARDS_BALANCE_SUCCESS)
                .build();

        AmericanExpressClient sut = new AmericanExpressClient(braintreeClient);
        sut.getRewardsBalance("fake-nonce", "USD", amexRewardsCallback);

        verify(braintreeClient).sendAnalyticsEvent("amex.rewards-balance.start");
        verify(braintreeClient).sendAnalyticsEvent("amex.rewards-balance.success");
    }

    @Test
    public void getRewardsBalance_sendsAnalyticsEventOnFailure() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendGETErrorResponse(new AuthorizationException("Bad fingerprint"))
                .build();

        AmericanExpressClient sut = new AmericanExpressClient(braintreeClient);
        sut.getRewardsBalance("fake-nonce", "USD", amexRewardsCallback);

        verify(braintreeClient).sendAnalyticsEvent("amex.rewards-balance.start");
        verify(braintreeClient).sendAnalyticsEvent("amex.rewards-balance.error");
    }

    @Test
    public void getRewardsBalance_sendsAnalyticsEventOnParseError() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendGETSuccessfulResponse("-- not json --")
                .build();
        AmericanExpressClient sut = new AmericanExpressClient(braintreeClient);
        sut.getRewardsBalance("fake-nonce", "USD", amexRewardsCallback);

        verify(braintreeClient).sendAnalyticsEvent("amex.rewards-balance.start");
        verify(braintreeClient).sendAnalyticsEvent("amex.rewards-balance.parse.failed");
    }
}
