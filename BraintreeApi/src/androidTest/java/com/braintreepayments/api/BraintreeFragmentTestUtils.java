package com.braintreepayments.api;

import android.app.Activity;
import android.os.Bundle;

import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.PaymentMethodCreatedListener;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.Card;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethod;

import org.json.JSONException;

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
        return getMockFragment(activity, stringFromFixture("client_token.json"), configuration);
    }

    /**
     * Get a {@link org.mockito.Spy} {@link BraintreeFragment} with the given {@link Configuration}.
     *
     * @param activity
     * @param authorization
     * @param configuration
     * @return
     */
    public static BraintreeFragment getMockFragment(final Activity activity, String authorization, Configuration configuration) {
        try {
            BraintreeFragment fragment = spy(BraintreeFragment.newInstance(activity, authorization));
            doNothing().when(fragment).fetchConfiguration();
            when(fragment.getApplicationContext()).thenReturn(getTargetContext());
            when(fragment.getConfiguration()).thenReturn(configuration);
            when(fragment.getAuthorization()).thenReturn(Authorization.fromString(authorization));
            fragment.mConfiguration = configuration;

            getInstrumentation().waitForIdleSync();
            waitForFragmentTransaction(activity);

            return fragment;
        } catch (InterruptedException | InvalidArgumentException e) {
            fail(e.getMessage());
            return new BraintreeFragment();
        }
    }

    public static BraintreeFragment getMockFragment(final Activity activity, String clientKeyOrToken, String configuration) {
            Bundle bundle = new Bundle();
        bundle.putString(BraintreeFragment.EXTRA_CONFIGURATION, configuration);
        try {
            BraintreeFragment fragment = spy(BraintreeFragment.newInstance(activity, clientKeyOrToken, bundle));
            doNothing().when(fragment).fetchConfiguration();
            when(fragment.getApplicationContext()).thenReturn(getTargetContext());
            when(fragment.getAuthorization()).thenReturn(Authorization.fromString(clientKeyOrToken));
            when(fragment.getConfiguration()).thenReturn(Configuration.fromJson(configuration));

            getInstrumentation().waitForIdleSync();
            waitForFragmentTransaction(activity);

            return fragment;
        } catch (InterruptedException | InvalidArgumentException | JSONException e) {
            fail(e.getMessage());
            return new BraintreeFragment();
        }
    }

    /**
     * Get a {@link BraintreeFragment} using the given ClientToken.
     *
     * @param activity
     * @param authorization
     * @return {@link BraintreeFragment}
     */
    public static BraintreeFragment getFragment(Activity activity, String authorization) {
        return getFragment(activity, authorization, null);
    }

    public static BraintreeFragment getFragment(Activity activity, String authorization, String configurationString) {
        BraintreeFragment fragment;
        Bundle args = new Bundle();
        try {
            if (configurationString != null) {
                args.putString(BraintreeFragment.EXTRA_CONFIGURATION, configurationString);
            }
            fragment = BraintreeFragment.newInstance(activity, authorization, args);

            getInstrumentation().waitForIdleSync();
            waitForFragmentTransaction(activity);

            return fragment;
        } catch (InterruptedException | InvalidArgumentException e) {
            fail(e.getMessage());
            return new BraintreeFragment();
        }
    }

    private static void waitForFragmentTransaction(final Activity activity) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.getFragmentManager().executePendingTransactions();
                latch.countDown();
            }
        });
        latch.await();
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
