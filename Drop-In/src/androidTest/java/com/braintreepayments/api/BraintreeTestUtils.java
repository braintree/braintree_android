package com.braintreepayments.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;

import com.braintreepayments.api.dropin.BraintreePaymentActivity;
import com.braintreepayments.api.dropin.R;
import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.internal.HttpRequest;
import com.braintreepayments.api.internal.HttpResponse;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.doesNotExist;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.assertThat;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.is;

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


    public static Map<String, Object> getActivityResult(Activity activity) {
        assertThat("Activity did not finish", activity.isFinishing(), is(true));
        Map<String, Object> resultMap = new HashMap<String, Object>();

        try {
            Field resultCodeField = Activity.class.getDeclaredField("mResultCode");
            resultCodeField.setAccessible(true);
            resultMap.put("resultCode", resultCodeField.get(activity));

            Field resultDataField = Activity.class.getDeclaredField("mResultData");
            resultDataField.setAccessible(true);
            resultMap.put("resultData", resultDataField.get(activity));

            return resultMap;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(
                    "Looks like the Android Activity class has changed it's private fields for mResultCode or mResultData. Time to update the reflection code.", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
                            FixturesHelper .stringFromFixture(context, "errors/error_response.json"));
                } else {
                    return super.post(url, params);
                }
            }
        };

        Braintree braintree = new Braintree(new BraintreeApi(context, clientToken, request));
        return injectBraintree(token, braintree);
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

        Braintree braintree = new Braintree(new BraintreeApi(context, clientToken, request));
        return injectBraintree(token, braintree);
    }

    public static Braintree injectBraintree(Context context, String clientToken) {
        return injectBraintree(clientToken, new Braintree(context, clientToken));
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
        Braintree braintree = new Braintree(braintreeApi);
        Braintree.sInstances.put(clientToken, braintree);
        return braintree;
    }

    public static void assertBitmapsEqual(Drawable d1, Drawable d2) {
        if (d1 == null || d2 == null) {
            assertEquals(d1, d2);
        } else {
            Bitmap b1 = ((BitmapDrawable) d1).getBitmap();
            Bitmap b2 = ((BitmapDrawable) d2).getBitmap();
            if (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB_MR1) {
                assertTrue(b1.sameAs(b2));
            } else {
                assertEquals(b1.getHeight(), b2.getHeight());
                assertEquals(b1.getWidth(), b2.getWidth());
                for (int x = 0; x < b1.getWidth(); x++) {
                    for (int y = 0; y < b1.getHeight(); y++) {
                        assertEquals(b1.getPixel(x, y), b2.getPixel(x, y));
                    }
                }
            }
        }
    }

    public static void assertSelectedPaymentMethodIs(int string) {
        onView(withText("Choose Payment Method")).check(doesNotExist());
        onView(withId(R.id.bt_payment_method_type)).check(matches(withText(string)));
    }

}
