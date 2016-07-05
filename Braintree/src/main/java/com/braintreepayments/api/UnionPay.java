package com.braintreepayments.api;

import android.net.Uri;

import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.UnionPayCapabilities;
import com.braintreepayments.api.models.UnionPayCardBuilder;
import com.braintreepayments.api.models.UnionPayConfiguration;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class containing UnionPay specific logic.
 */
public class UnionPay {

    private static final String UNIONPAY_ENROLLMENT_ID_KEY = "unionPayEnrollmentId";
    private static final String UNIONPAY_SMS_REQUIRED_KEY = "smsCodeRequired";
    private static final String UNIONPAY_CAPABILITIES_PATH = TokenizationClient.versionedPath(
            "payment_methods/credit_cards/capabilities");
    private static final String UNIONPAY_ENROLLMENT_PATH = TokenizationClient.versionedPath("union_pay_enrollments");

    /**
     * Fetches the capabilities of a card. If the card needs to be enrolled use {@link
     * UnionPay#enroll(BraintreeFragment, UnionPayCardBuilder)}.
     * <p/>
     * On completion, returns the {@link UnionPayCapabilities} to
     * {@link com.braintreepayments.api.interfaces.UnionPayListener#onCapabilitiesFetched(UnionPayCapabilities)}
     * <p/>
     * On error, an exception will be passed back to
     * {@link com.braintreepayments.api.interfaces.BraintreeErrorListener#onError(Exception)}
     *
     * @param fragment {@link BraintreeFragment}
     * @param cardNumber The card number to check for Union Pay capabilities.
     */
    public static void fetchCapabilities(final BraintreeFragment fragment, final String cardNumber) {
        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                if (!configuration.getUnionPay().isEnabled()) {
                    fragment.postCallback(new ConfigurationException("UnionPay is not enabled"));
                    return;
                }

                String fetchCapabilitiesUrl = Uri.parse(UNIONPAY_CAPABILITIES_PATH)
                        .buildUpon()
                        .appendQueryParameter("creditCard[number]", cardNumber)
                        .build()
                        .toString();

                fragment.getHttpClient().get(fetchCapabilitiesUrl, new HttpResponseCallback() {
                    @Override
                    public void success(String responseBody) {
                        fragment.postCallback(UnionPayCapabilities.fromJson(responseBody));
                        fragment.sendAnalyticsEvent("union-pay.capabilities-received");
                    }

                    @Override
                    public void failure(Exception exception) {
                        fragment.postCallback(exception);
                        fragment.sendAnalyticsEvent("union-pay.capabilities-failed");
                    }
                });
            }
        });
    }

    /**
     * Enrolls a Union Pay card. Only call this method if the card needs to be enrolled. Check {@link
     * UnionPay#fetchCapabilities(BraintreeFragment, String)} if your card needs to be enrolled.
     * <p/>
     * On completion, returns a enrollmentId to
     * {@link com.braintreepayments.api.interfaces.UnionPayListener#onSmsCodeSent(String)}
     * This enrollmentId needs to be applied to {@link UnionPayCardBuilder} along with the SMS code
     * collected from the merchant before invoking {@link UnionPay#tokenize(BraintreeFragment, UnionPayCardBuilder)}
     * <p/>
     * On error, an exception will be passed back to
     * {@link com.braintreepayments.api.interfaces.BraintreeErrorListener#onError(Exception)}
     *
     * @param fragment {@link BraintreeFragment}
     * @param unionPayCardBuilder {@link UnionPayCardBuilder}
     */
    public static void enroll(final BraintreeFragment fragment, final UnionPayCardBuilder unionPayCardBuilder) {
        fragment.waitForConfiguration(new ConfigurationListener() {
                @Override
                public void onConfigurationFetched(Configuration configuration) {
                    UnionPayConfiguration unionPayConfiguration = configuration.getUnionPay();
                    if (!unionPayConfiguration.isEnabled()) {
                        fragment.postCallback(new ConfigurationException("UnionPay is not enabled"));
                        return;
                    }

                    try {
                        JSONObject enrollmentPayloadJson =
                                unionPayCardBuilder.buildEnrollment();
                        fragment.getHttpClient().post(UNIONPAY_ENROLLMENT_PATH, enrollmentPayloadJson.toString(),
                                new HttpResponseCallback() {
                                    @Override
                                    public void success(String responseBody) {
                                        try {
                                            JSONObject response = new JSONObject(responseBody);
                                            String enrollmentId = response.getString(UNIONPAY_ENROLLMENT_ID_KEY);
                                            boolean smsCodeRequired = response.getBoolean(UNIONPAY_SMS_REQUIRED_KEY);
                                            fragment.postUnionPayCallback(enrollmentId, smsCodeRequired);
                                            fragment.sendAnalyticsEvent("union-pay.enrollment-succeeded");
                                        } catch (JSONException e) {
                                            failure(e);
                                        }
                                    }

                                    @Override
                                    public void failure(Exception exception) {
                                        fragment.postCallback(exception);
                                        fragment.sendAnalyticsEvent("union-pay.enrollment-failed");
                                    }
                                }
                        );
                    } catch (JSONException exception) {
                        fragment.postCallback(exception);
                    }
                }
            }
        );
    }

    /**
     * Create a {@link com.braintreepayments.api.models.CardNonce}. Note that if the card is a UnionPay card,
     * {@link UnionPayCardBuilder#enrollmentId(String)} and {@link UnionPayCardBuilder#smsCode(String)}
     * need to be set for tokenization to succeed.
     * <p/>
     * On completion, returns the {@link com.braintreepayments.api.models.PaymentMethodNonce} to {@link
     * com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener}.
     * <p/>
     * If creation fails validation, {@link com.braintreepayments.api.interfaces.BraintreeErrorListener#onError(Exception)}
     * will be called with the resulting {@link com.braintreepayments.api.exceptions.ErrorWithResponse}.
     * <p/>
     * If an error not due to validation (server error, network issue, etc.) occurs, {@link
     * com.braintreepayments.api.interfaces.BraintreeErrorListener#onError(Exception)} will be called with the {@link
     * Exception} that occurred.
     *
     * @param fragment {@link BraintreeFragment}
     * @param unionPayCardBuilder {@link UnionPayCardBuilder}
     */
    public static void tokenize(final BraintreeFragment fragment, final UnionPayCardBuilder unionPayCardBuilder) {
        TokenizationClient.tokenize(fragment, unionPayCardBuilder, new PaymentMethodNonceCallback() {
            @Override
            public void success(PaymentMethodNonce paymentMethodNonce) {
                fragment.postCallback(paymentMethodNonce);
                fragment.sendAnalyticsEvent("union-pay.nonce-received");
            }

            @Override
            public void failure(Exception exception) {
                fragment.postCallback(exception);
                fragment.sendAnalyticsEvent("union-pay.nonce-failed");
            }
        });
    }
}
