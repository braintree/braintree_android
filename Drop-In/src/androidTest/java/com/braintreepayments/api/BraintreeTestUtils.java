package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;

import com.braintreepayments.api.dropin.BraintreePaymentActivity;
import com.braintreepayments.api.dropin.R;
import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.internal.HttpRequest;
import com.braintreepayments.api.internal.HttpResponse;
import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static com.braintreepayments.testutils.ui.Matchers.withId;

public class BraintreeTestUtils {

    private BraintreeTestUtils() { throw new IllegalStateException("Non instantiable class"); }

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
            String clientTokenString) {
        ClientToken clientToken = ClientToken.fromString(clientTokenString);
        HttpRequest request = new HttpRequest(clientToken.getClientApiUrl(),
                clientToken.getAuthorizationFingerprint()) {
            @Override
            public HttpResponse post(String url, String params)
                    throws ErrorWithResponse, BraintreeException {
                if(url.contains("credit_cards")) {
                    return new HttpResponse(422,
                            stringFromFixture(context, "error_response.json"));
                } else {
                    return super.post(url, params);
                }
            }
        };

        return injectBraintree(context, clientTokenString, request);
    }

    public static Braintree injectSlowBraintree(Context context, String clientTokenString, final long delay) {
        ClientToken clientToken = ClientToken.fromString(clientTokenString);
        HttpRequest request = new HttpRequest(clientToken.getClientApiUrl(),
                clientToken.getAuthorizationFingerprint()) {
            @Override
            public HttpResponse get(String url) throws BraintreeException, ErrorWithResponse {
                SystemClock.sleep(delay);
                return super.get(url);
            }

            @Override
            public HttpResponse post(String url, String params)
                    throws BraintreeException, ErrorWithResponse {
                SystemClock.sleep(delay);
                return super.post(url, params);
            }
        };

        return injectBraintree(context, clientTokenString, request);
    }

    public static Braintree injectBraintree(Context context, String clientToken,
            HttpRequest httpRequest) {
        return injectBraintreeApi(clientToken,
                new BraintreeApi(context, Configuration.fromJson(clientToken), httpRequest));
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
