package com.braintreepayments.api;

import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.models.AmericanExpressRewardsBalance;
import com.braintreepayments.api.models.Configuration;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.json.*", "org.mockito.*", "org.robolectric.*", "android.*", "com.google.gms.*"})
@PrepareForTest(TokenizationClient.class)
public class AmericanExpressUnitTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();
    private Configuration mConfiguration;

    @Before
    public void setup() throws JSONException {
        mConfiguration = Configuration.fromJson(stringFromFixture("configuration.json"));
    }

    @Test
    public void getRewardsBalance_callsListenerWithRewardsBalanceOnSuccess() throws JSONException {
        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(mConfiguration)
                .successResponse(stringFromFixture("response/amex_rewards_balance_success.json"))
                .build();

        AmericanExpress.getRewardsBalance(fragment, "fake-nonce", "USD");

        ArgumentCaptor<AmericanExpressRewardsBalance> argumentCaptor = ArgumentCaptor.forClass(AmericanExpressRewardsBalance.class);
        verify(fragment).postAmericanExpressCallback(argumentCaptor.capture());

        AmericanExpressRewardsBalance rewardsBalance = argumentCaptor.getValue();
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
    public void getRewardsBalance_callsListenerWithRewardsBalanceOnIneligibleCard() throws JSONException {
        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(mConfiguration)
                .successResponse(stringFromFixture("response/amex_rewards_balance_ineligible_card.json"))
                .build();

        AmericanExpress.getRewardsBalance(fragment, "fake-nonce", "USD");

        ArgumentCaptor<AmericanExpressRewardsBalance> argumentCaptor = ArgumentCaptor.forClass(AmericanExpressRewardsBalance.class);
        verify(fragment).postAmericanExpressCallback(argumentCaptor.capture());

        AmericanExpressRewardsBalance rewardsBalance = argumentCaptor.getValue();
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
    public void getRewardsBalance_callsListenerWithRewardsBalanceOnInsufficientPoints() throws JSONException {
        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(mConfiguration)
                .successResponse(stringFromFixture("response/amex_rewards_balance_insufficient_points.json"))
                .build();

        AmericanExpress.getRewardsBalance(fragment, "fake-nonce", "USD");

        ArgumentCaptor<AmericanExpressRewardsBalance> argumentCaptor = ArgumentCaptor.forClass(AmericanExpressRewardsBalance.class);
        verify(fragment).postAmericanExpressCallback(argumentCaptor.capture());

        AmericanExpressRewardsBalance rewardsBalance = argumentCaptor.getValue();
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
        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(mConfiguration)
                .successResponse(stringFromFixture("response/amex_rewards_balance_success.json"))
                .build();

        AmericanExpress.getRewardsBalance(fragment, "fake-nonce", "USD");

        verify(fragment).sendAnalyticsEvent("amex.rewards-balance.success");
    }

    @Test
    public void getRewardsBalance_sendsAnalyticsEventOnFailure() {
        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(mConfiguration)
                .errorResponse(new AuthorizationException("Bad fingerprint"))
                .build();

        AmericanExpress.getRewardsBalance(fragment, "fake-nonce", "USD");

        verify(fragment).sendAnalyticsEvent("amex.rewards-balance.error");
    }
}
