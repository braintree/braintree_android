package com.braintreepayments.api;

import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.PaymentMethodBuilder;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricGradleTestRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(RobolectricGradleTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest(TokenizationClient.class)
public class CardUnitTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    private BraintreeFragment mBraintreeFragment;

    @Before
    public void setup() {
        mBraintreeFragment = mock(BraintreeFragment.class);
    }

    @Test
    public void tokenize_callsListenerWithNonceOnSuccess() {
        mockSuccessCallback();

        Card.tokenize(mBraintreeFragment, null);

        verify(mBraintreeFragment).postCallback(any(CardNonce.class));
    }

    @Test
    public void tokenize_sendsAnalyticsEventOnSuccess() {
        mockSuccessCallback();

        Card.tokenize(mBraintreeFragment, new CardBuilder());

        verify(mBraintreeFragment).sendAnalyticsEvent("card.nonce-received");
    }

    @Test
    public void tokenize_callsListenerWithErrorOnFailure() {
        mockFailureCallback();

        Card.tokenize(mBraintreeFragment, null);

        verify(mBraintreeFragment).postCallback(any(ErrorWithResponse.class));
    }

    @Test
    public void tokenize_sendsAnalyticsEventOnFailure() {
        mockFailureCallback();

        Card.tokenize(mBraintreeFragment, null);

        verify(mBraintreeFragment).sendAnalyticsEvent("card.nonce-failed");
    }

    /* helpers */
    private void mockSuccessCallback() {
        mockStatic(TokenizationClient.class);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                ((PaymentMethodNonceCallback) invocation.getArguments()[2]).success(new CardNonce());
                return null;
            }
        }).when(TokenizationClient.class);
        TokenizationClient.tokenize(any(BraintreeFragment.class), any(PaymentMethodBuilder.class),
                any(PaymentMethodNonceCallback.class));
    }

    private void mockFailureCallback() {
        mockStatic(TokenizationClient.class);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                ((PaymentMethodNonceCallback) invocation.getArguments()[2]).failure(new ErrorWithResponse(422, ""));
                return null;
            }
        }).when(TokenizationClient.class);
        TokenizationClient.tokenize(any(BraintreeFragment.class), any(PaymentMethodBuilder.class),
                any(PaymentMethodNonceCallback.class));
    }
}
