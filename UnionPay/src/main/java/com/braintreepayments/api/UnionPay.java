package com.braintreepayments.api;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.braintreepayments.api.exceptions.ConfigurationException;
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
 * Used to check, enroll, and tokenize UnionPay cards. For more information see the
 * <a href="https://developers.braintreepayments.com/guides/unionpay/overview">documentation</a>
 */
// TODO: Rename class when API is finalized
public class UnionPay {

    private static final String UNIONPAY_SMS_REQUIRED_KEY = "smsCodeRequired";
    private static final String UNIONPAY_ENROLLMENT_ID_KEY = "unionPayEnrollmentId";

    private static final String UNIONPAY_ENROLLMENT_PATH =
        TokenizationClient.versionedPath("union_pay_enrollments");

    private static final String UNIONPAY_CAPABILITIES_PATH = TokenizationClient.versionedPath(
            "payment_methods/credit_cards/capabilities");

    private final BraintreeClient braintreeClient;
    private final TokenizationClient tokenizationClient;

    UnionPay(BraintreeClient braintreeClient, TokenizationClient tokenizationClient) {
        this.braintreeClient = braintreeClient;
        this.tokenizationClient = tokenizationClient;
    }

    /**
     * Fetches the capabilities of a card. If the card needs to be enrolled use {@link
     * UnionPay#enroll(Context, UnionPayCardBuilder, UnionPayEnrollCallback)}.
     * <p>
     * On completion, returns the {@link UnionPayCapabilities} to
     * {@link com.braintreepayments.api.interfaces.UnionPayListener#onCapabilitiesFetched(UnionPayCapabilities)}
     * <p>
     * On error, an exception will be passed back to
     * {@link com.braintreepayments.api.interfaces.BraintreeErrorListener#onError(Exception)}
     *
     * @param context Android context
     * @param cardNumber The card number to check for Union Pay capabilities.
     * @param callback {@link UnionPayFetchCapabilitiesCallback}
     */
    public void fetchCapabilities(final Context context, final String cardNumber, final UnionPayFetchCapabilitiesCallback callback) {
        braintreeClient.getConfiguration(context, new ConfigurationCallback() {
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
                braintreeClient.sendGET(fetchCapabilitiesUrl, context, new HttpResponseCallback() {
                    @Override
                    public void success(String responseBody) {
                        callback.onResult(UnionPayCapabilities.fromJson(responseBody), null);
                        braintreeClient.sendAnalyticsEvent(context, "union-pay.capabilities-received");
                    }

                    @Override
                    public void failure(Exception exception) {
                        callback.onResult(null, exception);
                        braintreeClient.sendAnalyticsEvent(context, "union-pay.capabilities-failed");
                    }
                });
            }
        });
    }

    /**
     * Enrolls a Union Pay card. Only call this method if the card needs to be enrolled. Check {@link
     * UnionPay#fetchCapabilities(Context, String, UnionPayFetchCapabilitiesCallback)} if your card needs to be enrolled.
     * <p>
     * On completion, returns a enrollmentId to
     * {@link com.braintreepayments.api.interfaces.UnionPayListener#onSmsCodeSent(String, boolean)}
     * This enrollmentId needs to be applied to {@link UnionPayCardBuilder} along with the SMS code
     * collected from the merchant before invoking {@link UnionPay#tokenize(Context, UnionPayCardBuilder, UnionPayTokenizeCallback)}
     * <p>
     * On error, an exception will be passed back to
     * {@link com.braintreepayments.api.interfaces.BraintreeErrorListener#onError(Exception)}
     *
     * @param context Android context
     * @param unionPayCardBuilder {@link UnionPayCardBuilder}
     * @param callback {@link UnionPayEnrollCallback}
     */
    public void enroll(final Context context, final UnionPayCardBuilder unionPayCardBuilder, final UnionPayEnrollCallback callback) {
        braintreeClient.getConfiguration(context, new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                UnionPayConfiguration unionPayConfiguration = configuration.getUnionPay();
                if (!unionPayConfiguration.isEnabled()) {
                    callback.onResult(null, new ConfigurationException("UnionPay is not enabled"));
                    return;
                }

                try {
                    String enrollmentPayload = unionPayCardBuilder.buildEnrollment().toString();
                    braintreeClient.sendPOST(UNIONPAY_ENROLLMENT_PATH, enrollmentPayload, context, new HttpResponseCallback() {
                        @Override
                        public void success(String responseBody) {
                            try {
                                JSONObject response = new JSONObject(responseBody);
                                String enrollmentId = response.getString(UNIONPAY_ENROLLMENT_ID_KEY);
                                boolean smsCodeRequired = response.getBoolean(UNIONPAY_SMS_REQUIRED_KEY);
                                callback.onResult(new UnionPayEnrollment(enrollmentId, smsCodeRequired), null);
                                braintreeClient.sendAnalyticsEvent(context, "union-pay.enrollment-succeeded");
                            } catch (JSONException e) {
                                failure(e);
                            }
                        }

                        @Override
                        public void failure(Exception exception) {
                            callback.onResult(null, exception);
                            braintreeClient.sendAnalyticsEvent(context, "union-pay.enrollment-failed");
                        }
                    });
                } catch (JSONException exception) {
                    callback.onResult(null, exception);
                }
            }
        });
    }

    /**
     * Create a {@link com.braintreepayments.api.models.CardNonce}. Note that if the card is a UnionPay card,
     * {@link UnionPayCardBuilder#enrollmentId(String)} and {@link UnionPayCardBuilder#smsCode(String)}
     * need to be set for tokenization to succeed.
     * <p>
     * On completion, returns the {@link com.braintreepayments.api.models.PaymentMethodNonce} to {@link
     * com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener}.
     * <p>
     * If creation fails validation, {@link com.braintreepayments.api.interfaces.BraintreeErrorListener#onError(Exception)}
     * will be called with the resulting {@link com.braintreepayments.api.exceptions.ErrorWithResponse}.
     * <p>
     * If an error not due to validation (server error, network issue, etc.) occurs, {@link
     * com.braintreepayments.api.interfaces.BraintreeErrorListener#onError(Exception)} will be called with the {@link
     * Exception} that occurred.
     *
     * @param context Android context
     * @param unionPayCardBuilder {@link UnionPayCardBuilder}
     * @param callback {@link UnionPayTokenizeCallback}
     */
    public void tokenize(final Context context, UnionPayCardBuilder unionPayCardBuilder, final UnionPayTokenizeCallback callback) {
        tokenizationClient.tokenize(context, unionPayCardBuilder, new PaymentMethodNonceCallback() {
            @Override
            public void success(PaymentMethodNonce paymentMethodNonce) {
                callback.onResult(paymentMethodNonce, null);
                braintreeClient.sendAnalyticsEvent(context, "union-pay.nonce-received");
            }

            @Override
            public void failure(Exception exception) {
                callback.onResult(null, exception);
                braintreeClient.sendAnalyticsEvent(context, "union-pay.nonce-failed");
            }
        });
    }
}
