package com.braintreepayments.api;

import android.app.Activity;

import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.PaymentMethodCreatedListener;
import com.braintreepayments.api.models.Card;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethod;

import java.util.concurrent.CountDownLatch;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.InstrumentationRegistry.getTargetContext;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BraintreeFragmentTestUtils {

    /**
     * Get a {@link org.mockito.Spy} {@link BraintreeFragment} with the given {@link Configuration}.
     *
     * @param activity
     * @param configuration
     * @return
     */
    public static BraintreeFragment getMockFragment(Activity activity, Configuration configuration) {
        try {
            String clientToken = stringFromFixture("client_token.json");
            BraintreeFragment fragment = spy(BraintreeFragment.newInstance(activity, clientToken));
            doNothing().when(fragment).fetchConfiguration();
            when(fragment.getApplicationContext()).thenReturn(getTargetContext());
            when(fragment.getConfiguration()).thenReturn(configuration);
            getInstrumentation().waitForIdleSync();

            return fragment;
        } catch (InvalidArgumentException e) {
            fail(e.getMessage());
            return new BraintreeFragment();
        }
    }

    /**
     * Get a {@link org.mockito.Spy} {@link BraintreeFragment} with the given {@link Configuration}.
     *
     * @param activity
     * @param clientToken
     * @param configuration
     * @return
     */
    public static BraintreeFragment getMockFragment(Activity activity, String clientToken, Configuration configuration) {
        try {
            BraintreeFragment fragment = spy(BraintreeFragment.newInstance(activity, clientToken));
            doNothing().when(fragment).fetchConfiguration();
            when(fragment.getApplicationContext()).thenReturn(getTargetContext());
            when(fragment.getConfiguration()).thenReturn(configuration);
            getInstrumentation().waitForIdleSync();

            return fragment;
        } catch (InvalidArgumentException e) {
            fail(e.getMessage());
            return new BraintreeFragment();
        }
    }

    /**
     * Get a {@link BraintreeFragment} using the given ClientToken.
     *
     * @param activity
     * @param clientToken
     * @return {@link BraintreeFragment}
     */
    public static BraintreeFragment getFragment(Activity activity, String clientToken) {
        try {
            BraintreeFragment fragment = BraintreeFragment.newInstance(activity, clientToken);
            getInstrumentation().waitForIdleSync();
            return fragment;
        } catch (InvalidArgumentException e) {
            fail(e.getMessage());
            return new BraintreeFragment();
        }
    }

    /**
     * Tokenize a card and return the {@link Card} instance.
     *
     * @param fragment
     * @param cardBuilder
     * @return {@link Card}
     */
    public static Card tokenize(BraintreeFragment fragment, CardBuilder cardBuilder) {
        final CountDownLatch latch = new CountDownLatch(1);
        final Card[] card = new Card[1];
        PaymentMethodCreatedListener listener = new PaymentMethodCreatedListener() {
            @Override
            public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
                card[0] = (Card) paymentMethod;
                latch.countDown();
            }
        };
        fragment.addListener(listener);

        TokenizationClient.tokenize(fragment, cardBuilder);

        try {
            latch.await();
        } catch (InterruptedException ignored) {}

        fragment.removeListener(listener);
        return card[0];
    }

    public static void verifyAnalyticsEvent(BraintreeFragment fragment, String event) {
        verify(fragment).sendAnalyticsEvent(eq(event));
    }
}
