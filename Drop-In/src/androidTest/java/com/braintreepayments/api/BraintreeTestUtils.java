package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;

import com.braintreepayments.api.dropin.BraintreePaymentActivity;
import com.braintreepayments.api.dropin.R;
import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.internal.HttpRequest;
import com.braintreepayments.api.internal.HttpResponse;
import com.braintreepayments.testutils.FixturesHelper;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.doesNotExist;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;

public class BraintreeTestUtils {

    private BraintreeTestUtils() { throw new IllegalStateException("Non instantiable class"); }

    /** Returns a {@link String} client token to allow setup to make Gateway calls. */
    public static String setUpActivityTest(
            ActivityInstrumentationTestCase2<BraintreePaymentActivity> testCase) {
        return setUpActivityTest(testCase, new TestClientTokenBuilder().withFakePayPal().build());
    }

    public static String setUpActivityTest(
            ActivityInstrumentationTestCase2<BraintreePaymentActivity> testCase,
            String clientToken) {
        Intent intent = new Intent(testCase.getInstrumentation().getContext(),
                BraintreePaymentActivity.class);
        intent.putExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN, clientToken);
        testCase.setActivityIntent(intent);

        return clientToken;
    }

    public static void postUnrecoverableErrorFromBraintree(Braintree braintree, BraintreeException exception) {
        braintree.postUnrecoverableErrorToListeners(exception);
    }

    public static Braintree injectGeneric422ErrorOnCardCreateBraintree(final Context context,
            String token) {
        ClientToken clientToken = ClientToken.getClientToken(token);
        HttpRequest request = new HttpRequest(clientToken.getAuthorizationFingerprint()) {
            @Override
            public HttpResponse post(String url, String params) throws UnexpectedException {
                if(url.contains("credit_cards")) {
                    return new HttpResponse(422,
                            FixturesHelper.stringFromFixture(context, "errors/error_response.json"));
                } else {
                    return super.post(url, params);
                }
            }
        };

        return new Braintree(token, new BraintreeApi(context, clientToken, request));
    }

    public static Braintree injectSlowBraintree(Context context, String token, final long delay) {
        ClientToken clientToken = ClientToken.getClientToken(token);
        HttpRequest request = new HttpRequest(clientToken.getAuthorizationFingerprint()) {
            @Override
            public HttpResponse get(String url) throws UnexpectedException {
                SystemClock.sleep(delay);
                return super.get(url);
            }

            @Override
            public HttpResponse post(String url, String params) throws UnexpectedException {
                SystemClock.sleep(delay);
                return super.post(url, params);
            }
        };

        return new Braintree(token, new BraintreeApi(context, clientToken, request));
    }

    public static Braintree injectBraintree(Context context, String clientToken,
            HttpRequest httpRequest) {
        return injectBraintreeApi(clientToken,
                new BraintreeApi(context, ClientToken.getClientToken(clientToken), httpRequest));
    }

    public static Braintree injectBraintree(String clientToken, Braintree braintree) {
        Braintree.sInstances.put(clientToken, braintree);
        return braintree;
    }

    public static Braintree injectBraintreeApi(String clientToken, BraintreeApi braintreeApi) {
        return new Braintree(clientToken, braintreeApi);
    }

    public static void assertSelectedPaymentMethodIs(int string) {
        onView(withText("Choose Payment Method")).check(doesNotExist());
        onView(withId(R.id.bt_payment_method_type)).check(matches(withText(string)));
    }

}
