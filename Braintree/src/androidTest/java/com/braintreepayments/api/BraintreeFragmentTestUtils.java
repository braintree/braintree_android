package com.braintreepayments.api;

import android.app.Activity;

import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodNonce;

import org.json.JSONException;

import java.util.concurrent.CountDownLatch;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.InstrumentationRegistry.getTargetContext;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static com.braintreepayments.testutils.SharedPreferencesHelper.writeMockConfiguration;
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
            when(fragment.getAuthorization()).thenReturn(Authorization.fromString(authorization));
            when(fragment.getSessionId()).thenReturn(DeviceMetadata.getFormattedUUID());
            fragment.mConfiguration = configuration;

            getInstrumentation().waitForIdleSync();
            waitForFragmentTransaction(activity);

            return fragment;
        } catch (InterruptedException | InvalidArgumentException e) {
            fail(e.getMessage());
            return new BraintreeFragment();
        }
    }

    /**
     * Get a {@link org.mockito.Spy} {@link BraintreeFragment} with the given configuration string.
     *
     * @param activity
     * @param authorizationString
     * @param configuration
     * @return
     */
    public static BraintreeFragment getMockFragment(final Activity activity, String authorizationString, String configuration) {
        try {
            Authorization authorization = Authorization.fromString(authorizationString);
            writeMockConfiguration(authorization.getConfigUrl(), configuration);

            BraintreeFragment fragment = spy(BraintreeFragment.newInstance(activity, authorizationString));
            when(fragment.getApplicationContext()).thenReturn(getTargetContext());
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

    public static BraintreeFragment getFragment(Activity activity, String authorizationString, String configurationString) {
        BraintreeFragment fragment;
        try {
            if (configurationString != null) {
                Authorization authorization = Authorization.fromString(authorizationString);
                writeMockConfiguration(authorization.getConfigUrl(), configurationString);
            }
            fragment = BraintreeFragment.newInstance(activity, authorizationString);

            getInstrumentation().waitForIdleSync();
            waitForFragmentTransaction(activity);

            return fragment;
        } catch (InterruptedException | InvalidArgumentException e) {
            fail(e.getMessage());
            return new BraintreeFragment();
        }
    }

    /**
     * Tokenize a card and return the {@link CardNonce} instance.
     *
     * @param fragment
     * @param cardBuilder
     * @return {@link CardNonce}
     */
    public static CardNonce tokenize(BraintreeFragment fragment, CardBuilder cardBuilder) {
        final CountDownLatch latch = new CountDownLatch(1);
        final CardNonce[] cardNonce = new CardNonce[1];
        PaymentMethodNonceCreatedListener listener = new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                cardNonce[0] = (CardNonce) paymentMethodNonce;
                latch.countDown();
            }
        };
        fragment.addListener(listener);

        Card.tokenize(fragment, cardBuilder);

        try {
            latch.await();
        } catch (InterruptedException ignored) {}

        fragment.removeListener(listener);
        return cardNonce[0];
    }

    public static void verifyAnalyticsEvent(BraintreeFragment fragment, String event) {
        verify(fragment).sendAnalyticsEvent(eq(event));
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
}
