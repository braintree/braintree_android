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
import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.internal.HttpRequest;
import com.braintreepayments.api.internal.HttpRequest.HttpMethod;
import com.braintreepayments.api.internal.HttpRequestFactory;
import com.squareup.okhttp.OkHttpClient;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.assertThat;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.is;

public class TestUtils {

    private TestUtils() { throw new IllegalStateException("Non instantiable class"); }

    /** Returns a {@link String} client token to allow setup to make Gateway calls. */
    public static String setUpActivityTest(
            ActivityInstrumentationTestCase2<BraintreePaymentActivity> testCase) {
        return setUpActivityTest(testCase, new TestClientTokenBuilder().withPayPal().build());
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

    public static Braintree injectCountPaymentMethodListBraintree(final Context context, String clientToken, final
            AtomicInteger count) {
        HttpRequestFactory requestFactory = new HttpRequestFactory(context) {
            @Override
            public HttpRequest getRequest(HttpMethod method, final String url) {
                return new HttpRequest(new OkHttpClient(), method, url) {
                    @Override
                    public HttpRequest execute() throws UnexpectedException {
                        HttpRequest response = super.execute();

                        if (url.contains("payment_methods")) {
                            count.incrementAndGet();
                        }

                        return response;
                    }
                };
            }
        };

        Braintree braintree = new Braintree(new BraintreeApi(context, ClientToken
                .getClientToken(clientToken), requestFactory));
        return injectBraintree(clientToken, braintree);
    }

    public static Braintree injectUnexpectedExceptionThrowingBraintree(final Context context,
            String clientToken) {
        HttpRequestFactory requestFactory = new HttpRequestFactory(context) {
            @Override
            public HttpRequest getRequest(HttpMethod method, String url) {
                return new HttpRequest(new OkHttpClient(), method, url) {
                    @Override
                    public HttpRequest execute() throws UnexpectedException {
                        throw new UnexpectedException("Mocked HTTP request");
                    }
                };
            }
        };

        Braintree braintree = new Braintree(new BraintreeApi(context, ClientToken.getClientToken(clientToken), requestFactory));
        return injectBraintree(clientToken, braintree);
    }

    public static Braintree injectGeneric422ErrorOnCardCreateBraintree(final Context context,
            String clientToken) {
        HttpRequestFactory requestFactory = new HttpRequestFactory(context) {
            @Override
            public HttpRequest getRequest(HttpMethod method, final String url) {
                return new HttpRequest(new OkHttpClient(), method, url) {
                    @Override
                    public String response() {
                        if(url.contains("credit_cards")) {
                            return FixturesHelper
                                    .stringFromFixture(context, "errors/error_response.json");
                        } else {
                            return super.response();
                        }
                    }

                    @Override
                    public int statusCode() {
                        if(url.contains("credit_cards")) {
                            return 422;
                        } else {
                            return super.statusCode();
                        }
                    }
                };
            }
        };

        Braintree braintree = new Braintree(new BraintreeApi(context, ClientToken.getClientToken(clientToken), requestFactory));
        return injectBraintree(clientToken, braintree);
    }

    public static Braintree injectSlowBraintree(Context context, String clientToken, final long delay) {
        HttpRequestFactory requestFactory = new HttpRequestFactory(context) {
            @Override
            public HttpRequest getRequest(HttpMethod method, String url) {
                return new HttpRequest(new OkHttpClient(), method, url) {
                    @Override
                    public HttpRequest execute() throws UnexpectedException {
                        SystemClock.sleep(delay);
                        return super.execute();
                    }

                    @Override
                    public String response() {
                        return super.response();
                    }

                    @Override
                    public int statusCode() {
                        return super.statusCode();
                    }
                };
            }
        };

        Braintree braintree = new Braintree(new BraintreeApi(context, ClientToken.getClientToken(clientToken), requestFactory));
        return injectBraintree(clientToken, braintree);
    }

    public static Braintree injectBraintree(Context context, String clientToken) {
        return injectBraintree(clientToken, new Braintree(context, clientToken));
    }

    public static Braintree injectBraintree(String clientToken, Braintree braintree) {
        Braintree.sInstances.put(clientToken, braintree);
        return braintree;
    }

    public static void injectBraintreeApi(String clientToken, BraintreeApi braintreeApi) {
        Braintree.sInstances.put(clientToken, new Braintree(braintreeApi));
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

}
