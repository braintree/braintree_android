package com.braintreepayments.api;

import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to check, enroll, and tokenize UnionPay cards. For more information see the
 * <a href="https://developers.braintreepayments.com/guides/unionpay/overview">documentation</a>
 */
public class UnionPayClient {

    private static final String UNIONPAY_SMS_REQUIRED_KEY = "smsCodeRequired";
    private static final String UNIONPAY_ENROLLMENT_ID_KEY = "unionPayEnrollmentId";

    private static final String UNIONPAY_ENROLLMENT_PATH =
        TokenizationClient.versionedPath("union_pay_enrollments");

    private static final String UNIONPAY_CAPABILITIES_PATH =
        TokenizationClient.versionedPath("payment_methods/credit_cards/capabilities");

    private final BraintreeClient braintreeClient;
    private final TokenizationClient tokenizationClient;

    public UnionPayClient(BraintreeClient braintreeClient) {
        this(braintreeClient, new TokenizationClient(braintreeClient));
    }

    @VisibleForTesting
    UnionPayClient(BraintreeClient braintreeClient, TokenizationClient tokenizationClient) {
        this.braintreeClient = braintreeClient;
        this.tokenizationClient = tokenizationClient;
    }

    /**
     * Fetches the capabilities of a card. If the card needs to be enrolled use {@link
     * UnionPayClient#enroll(UnionPayCardBuilder, UnionPayEnrollCallback)}.
     * <p>
     * On completion, returns the {@link UnionPayCapabilities} to
     * {@link UnionPayListener#onCapabilitiesFetched(UnionPayCapabilities)}
     * <p>
     * On error, an exception will be passed back to
     * {@link BraintreeErrorListener#onError(Exception)}
     * @param cardNumber The card number to check for Union Pay capabilities.
     * @param callback {@link UnionPayFetchCapabilitiesCallback}
     */
    public void fetchCapabilities(final String cardNumber, final UnionPayFetchCapabilitiesCallback callback) {
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (!configuration.getUnionPay().isEnabled()) {
                    callback.onResult(null, new ConfigurationException("UnionPay is not enabled"));
                    return;
                }

                String fetchCapabilitiesUrl = Uri.parse(UNIONPAY_CAPABILITIES_PATH)
                        .buildUpon()
                        .appendQueryParameter("creditCard[number]", cardNumber)
                        .build()
                        .toString();
                braintreeClient.sendGET(fetchCapabilitiesUrl, new HttpResponseCallback() {
                    @Override
                    public void success(String responseBody) {
                        callback.onResult(UnionPayCapabilities.fromJson(responseBody), null);
                        braintreeClient.sendAnalyticsEvent("union-pay.capabilities-received");
                    }

                    @Override
                    public void failure(Exception exception) {
                        callback.onResult(null, exception);
                        braintreeClient.sendAnalyticsEvent("union-pay.capabilities-failed");
                    }
                });
            }
        });
    }

    /**
     * Enrolls a Union Pay card. Only call this method if the card needs to be enrolled. Check {@link
     * UnionPayClient#fetchCapabilities(String, UnionPayFetchCapabilitiesCallback)} if your card needs to be enrolled.
     * <p>
     * On completion, returns a enrollmentId to
     * {@link UnionPayListener#onSmsCodeSent(String, boolean)}
     * This enrollmentId needs to be applied to {@link UnionPayCardBuilder} along with the SMS code
     * collected from the merchant before invoking {@link UnionPayClient#tokenize(UnionPayCardBuilder, UnionPayTokenizeCallback)}
     * <p>
     * On error, an exception will be passed back to
     * {@link BraintreeErrorListener#onError(Exception)}
     * @param unionPayCardBuilder {@link UnionPayCardBuilder}
     * @param callback {@link UnionPayEnrollCallback}
     */
    public void enroll(final UnionPayCardBuilder unionPayCardBuilder, final UnionPayEnrollCallback callback) {
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                UnionPayConfiguration unionPayConfiguration = configuration.getUnionPay();
                if (!unionPayConfiguration.isEnabled()) {
                    callback.onResult(null, new ConfigurationException("UnionPay is not enabled"));
                    return;
                }

                try {
                    String enrollmentPayload = unionPayCardBuilder.buildEnrollment().toString();
                    braintreeClient.sendPOST(UNIONPAY_ENROLLMENT_PATH, enrollmentPayload, new HttpResponseCallback() {
                        @Override
                        public void success(String responseBody) {
                            try {
                                JSONObject response = new JSONObject(responseBody);
                                String enrollmentId = response.getString(UNIONPAY_ENROLLMENT_ID_KEY);
                                boolean smsCodeRequired = response.getBoolean(UNIONPAY_SMS_REQUIRED_KEY);
                                callback.onResult(new UnionPayEnrollment(enrollmentId, smsCodeRequired), null);
                                braintreeClient.sendAnalyticsEvent("union-pay.enrollment-succeeded");
                            } catch (JSONException e) {
                                failure(e);
                            }
                        }

                        @Override
                        public void failure(Exception exception) {
                            callback.onResult(null, exception);
                            braintreeClient.sendAnalyticsEvent("union-pay.enrollment-failed");
                        }
                    });
                } catch (JSONException exception) {
                    callback.onResult(null, exception);
                }
            }
        });
    }

    /**
     * Create a {@link CardNonce}. Note that if the card is a UnionPay card,
     * {@link UnionPayCardBuilder#enrollmentId(String)} and {@link UnionPayCardBuilder#smsCode(String)}
     * need to be set for tokenization to succeed.
     * <p>
     * On completion, returns the {@link PaymentMethodNonce} to {@link
     * PaymentMethodNonceCreatedListener}.
     * <p>
     * If creation fails validation, {@link BraintreeErrorListener#onError(Exception)}
     * will be called with the resulting {@link ErrorWithResponse}.
     * <p>
     * If an error not due to validation (server error, network issue, etc.) occurs, {@link
     * BraintreeErrorListener#onError(Exception)} will be called with the {@link
     * Exception} that occurred.
     * @param unionPayCardBuilder {@link UnionPayCardBuilder}
     * @param callback {@link UnionPayTokenizeCallback}
     */
    public void tokenize(UnionPayCardBuilder unionPayCardBuilder, final UnionPayTokenizeCallback callback) {
        tokenizationClient.tokenize(unionPayCardBuilder, new PaymentMethodNonceCallback() {
            @Override
            public void success(PaymentMethodNonce paymentMethodNonce) {
                callback.onResult((CardNonce) paymentMethodNonce, null);
                braintreeClient.sendAnalyticsEvent("union-pay.nonce-received");
            }

            @Override
            public void failure(Exception exception) {
                callback.onResult(null, exception);
                braintreeClient.sendAnalyticsEvent("union-pay.nonce-failed");
            }
        });
    }
}
