package com.braintreepayments.api;

import android.net.Uri;

import androidx.annotation.NonNull;
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
        ApiClient.versionedPath("union_pay_enrollments");

    private static final String UNIONPAY_CAPABILITIES_PATH =
        ApiClient.versionedPath("payment_methods/credit_cards/capabilities");

    private final BraintreeClient braintreeClient;
    private final ApiClient apiClient;

    public UnionPayClient(@NonNull BraintreeClient braintreeClient) {
        this(braintreeClient, new ApiClient(braintreeClient));
    }

    @VisibleForTesting
    UnionPayClient(BraintreeClient braintreeClient, ApiClient apiClient) {
        this.braintreeClient = braintreeClient;
        this.apiClient = apiClient;
    }

    /**
     * Fetches the capabilities of a card. If the card needs to be enrolled use {@link
     * UnionPayClient#enroll(UnionPayCard, UnionPayEnrollCallback)}.
     * <p>
     * On completion, returns the {@link UnionPayCapabilities} to
     * {@link UnionPayFetchCapabilitiesCallback#onResult(UnionPayCapabilities, Exception)}
     * <p>
     * On error, an exception will be passed back to
     * {@link UnionPayFetchCapabilitiesCallback#onResult(UnionPayCapabilities, Exception)}
     *
     * @param cardNumber The card number to check for Union Pay capabilities.
     * @param callback {@link UnionPayFetchCapabilitiesCallback}
     */
    public void fetchCapabilities(@NonNull final String cardNumber, @NonNull final UnionPayFetchCapabilitiesCallback callback) {
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (!configuration.isUnionPayEnabled()) {
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
                    public void onResult(String responseBody, Exception httpError) {
                        if (responseBody != null) {
                            callback.onResult(UnionPayCapabilities.fromJson(responseBody), null);
                            braintreeClient.sendAnalyticsEvent("union-pay.capabilities-received");
                        } else {
                            callback.onResult(null, httpError);
                            braintreeClient.sendAnalyticsEvent("union-pay.capabilities-failed");
                        }
                    }
                });
            }
        });
    }

    /**
     * Enrolls a Union Pay card. Only call this method if the card needs to be enrolled. Check {@link
     * UnionPayClient#fetchCapabilities(String, UnionPayFetchCapabilitiesCallback)} if your card needs to be enrolled.
     * <p>
     * On completion, returns an enrollmentId to {@link UnionPayEnrollCallback#onResult(UnionPayEnrollment, Exception)}
     * This enrollmentId needs to be applied to {@link UnionPayCard} along with the SMS code
     * collected from the merchant before invoking {@link UnionPayClient#tokenize(UnionPayCard, UnionPayTokenizeCallback)}
     * <p>
     * On error, an exception will be passed back to {@link UnionPayEnrollCallback#onResult(UnionPayEnrollment, Exception)}
     *
     * @param unionPayCard {@link UnionPayCard}
     * @param callback {@link UnionPayEnrollCallback}
     */
    public void enroll(@NonNull final UnionPayCard unionPayCard, @NonNull final UnionPayEnrollCallback callback) {
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (!configuration.isUnionPayEnabled()) {
                    callback.onResult(null, new ConfigurationException("UnionPay is not enabled"));
                    return;
                }

                try {
                    String enrollmentPayload = unionPayCard.buildEnrollment().toString();
                    braintreeClient.sendPOST(UNIONPAY_ENROLLMENT_PATH, enrollmentPayload, new HttpResponseCallback() {

                        @Override
                        public void onResult(String responseBody, Exception httpError) {
                            if (responseBody != null) {
                                try {
                                    JSONObject response = new JSONObject(responseBody);
                                    String enrollmentId = response.getString(UNIONPAY_ENROLLMENT_ID_KEY);
                                    boolean smsCodeRequired = response.getBoolean(UNIONPAY_SMS_REQUIRED_KEY);
                                    callback.onResult(new UnionPayEnrollment(enrollmentId, smsCodeRequired), null);
                                    braintreeClient.sendAnalyticsEvent("union-pay.enrollment-succeeded");
                                } catch (JSONException e) {
                                    callback.onResult(null, e);
                                    braintreeClient.sendAnalyticsEvent("union-pay.enrollment-failed");
                                }
                            } else {
                                callback.onResult(null, httpError);
                                braintreeClient.sendAnalyticsEvent("union-pay.enrollment-failed");
                            }
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
     * {@link UnionPayCard#setEnrollmentId(String)} and {@link UnionPayCard#setSmsCode(String)}
     * need to be set for tokenization to succeed.
     * <p>
     * On completion, returns the {@link CardNonce} to {@link UnionPayTokenizeCallback#onResult(CardNonce, Exception)}
     * <p>
     * If creation fails validation, {@link UnionPayTokenizeCallback#onResult(CardNonce, Exception)}
     * will be called with the resulting {@link ErrorWithResponse}.
     * <p>
     * If an error not due to validation (server error, network issue, etc.) occurs,
     * {@link UnionPayTokenizeCallback#onResult(CardNonce, Exception)} will be called with the {@link
     * Exception} that occurred.
     *
     * @param unionPayCard {@link UnionPayCard}
     * @param callback {@link UnionPayTokenizeCallback}
     */
    public void tokenize(@NonNull UnionPayCard unionPayCard, @NonNull final UnionPayTokenizeCallback callback) {
        apiClient.tokenizeREST(unionPayCard, new TokenizeCallback() {
            @Override
            public void onResult(JSONObject tokenizationResponse, Exception exception) {
                if (tokenizationResponse != null) {
                    try {
                        CardNonce cardNonce = CardNonce.fromJSON(tokenizationResponse);
                        callback.onResult(cardNonce, null);
                        braintreeClient.sendAnalyticsEvent("union-pay.nonce-received");
                    } catch (JSONException e) {
                        callback.onResult(null, e);
                    }
                } else {
                    callback.onResult(null, exception);
                    braintreeClient.sendAnalyticsEvent("union-pay.nonce-failed");
                }
            }
        });
    }
}
