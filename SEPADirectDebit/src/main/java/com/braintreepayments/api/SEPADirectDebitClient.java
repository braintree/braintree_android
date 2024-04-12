package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.URLUtil;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.braintreepayments.api.core.BraintreeClient;
import com.braintreepayments.api.core.BraintreeException;
import com.braintreepayments.api.core.BraintreeRequestCodes;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to integrate with SEPA Direct Debit.
 */
public class SEPADirectDebitClient {

    private static final String IBAN_LAST_FOUR_KEY = "ibanLastFour";
    private static final String CUSTOMER_ID_KEY = "customerId";
    private static final String BANK_REFERENCE_TOKEN_KEY = "bankReferenceToken";
    private static final String MANDATE_TYPE_KEY = "mandateType";

    private final SEPADirectDebitApi sepaDirectDebitApi;
    private final BraintreeClient braintreeClient;

    /**
     * Initializes a new {@link SEPADirectDebitClient} instance
     *
     * @param context an Android Context
     * @param authorization a Tokenization Key or Client Token used to authenticate
     */
    public SEPADirectDebitClient(@NonNull Context context, @NonNull String authorization) {
       this(new BraintreeClient(context, authorization));
    }

    @VisibleForTesting
    SEPADirectDebitClient(@NonNull BraintreeClient braintreeClient) {
        this(braintreeClient, new SEPADirectDebitApi(braintreeClient));
    }

    @VisibleForTesting
    SEPADirectDebitClient(BraintreeClient braintreeClient, SEPADirectDebitApi sepaDirectDebitApi) {
        this.braintreeClient = braintreeClient;
        this.sepaDirectDebitApi = sepaDirectDebitApi;
    }

    /**
     * Starts the SEPA tokenization process by creating a {@link SEPADirectDebitPaymentAuthRequestParams} to be used
     * to launch the SEPA mandate flow in
     * {@link SEPADirectDebitLauncher#launch(ComponentActivity, SEPADirectDebitPaymentAuthRequest.ReadyToLaunch)}
     *
     * @param sepaDirectDebitRequest {@link SEPADirectDebitRequest}
     * @param callback {@link SEPADirectDebitPaymentAuthRequestCallback}
     */
    public void createPaymentAuthRequest(@NonNull final SEPADirectDebitRequest sepaDirectDebitRequest,
                                         @NonNull final SEPADirectDebitPaymentAuthRequestCallback callback) {
        braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.TOKENIZE_STARTED);
        sepaDirectDebitApi.createMandate(sepaDirectDebitRequest,
                braintreeClient.getReturnUrlScheme(),
                (result, createMandateError) -> {
                    if (result != null) {
                        if (URLUtil.isValidUrl(result.getApprovalUrl())) {
                            braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.CREATE_MANDATE_CHALLENGE_REQUIRED);
                            try {
                                SEPADirectDebitPaymentAuthRequestParams params =
                                        new SEPADirectDebitPaymentAuthRequestParams(buildBrowserSwitchOptions(result));
                                braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.CREATE_MANDATE_SUCCEEDED);
                                callback.onSEPADirectDebitPaymentAuthResult(new SEPADirectDebitPaymentAuthRequest.ReadyToLaunch(params));
                            } catch (JSONException exception) {
                                braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.CREATE_MANDATE_FAILED);
                                callbackCreatePaymentAuthFailure(callback, new SEPADirectDebitPaymentAuthRequest.Failure(exception));
                            }
                        } else if (result.getApprovalUrl().equals("null")) {
                            braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.CREATE_MANDATE_SUCCEEDED);
                            // Mandate has already been approved
                            sepaDirectDebitApi.tokenize(result.getIbanLastFour(),
                                    result.getCustomerId(), result.getBankReferenceToken(),
                                    result.getMandateType().toString(),
                                    (sepaDirectDebitNonce, tokenizeError) -> {
                                        if (sepaDirectDebitNonce != null) {
                                            callbackCreatePaymentAuthChallengeNotRequiredSuccess(callback, new SEPADirectDebitPaymentAuthRequest.LaunchNotRequired(sepaDirectDebitNonce));
                                        } else if (tokenizeError != null) {
                                            callbackCreatePaymentAuthFailure(callback, new SEPADirectDebitPaymentAuthRequest.Failure(tokenizeError));
                                        }
                                    });
                        } else {
                            braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.CREATE_MANDATE_FAILED);
                            callbackCreatePaymentAuthFailure(callback, new SEPADirectDebitPaymentAuthRequest.Failure(new BraintreeException("An unexpected error occurred.")));
                        }
                    } else if (createMandateError != null) {
                        braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.CREATE_MANDATE_FAILED);
                        callbackCreatePaymentAuthFailure(callback,
                                new SEPADirectDebitPaymentAuthRequest.Failure(createMandateError));
                    }
                });
    }

    // TODO: - The wording in this docstring is confusing to me. Let's improve & align across all clients.
    /**
     * After receiving a result from the SEPA mandate web flow via
     * {@link SEPADirectDebitLauncher#handleReturnToAppFromBrowser(SEPADirectDebitPendingRequest.Started, Intent)} , pass the
     * {@link SEPADirectDebitPaymentAuthResult.Success} returned to this method to tokenize the SEPA
     * account and receive a {@link SEPADirectDebitNonce} on success.
     *
     * @param paymentAuthResult a {@link SEPADirectDebitPaymentAuthResult.Success} received from
     *                          {@link SEPADirectDebitLauncher#handleReturnToAppFromBrowser(SEPADirectDebitPendingRequest.Started, Intent)}
     * @param callback {@link SEPADirectDebitInternalTokenizeCallback}
     */
    public void tokenize(@NonNull SEPADirectDebitPaymentAuthResult.Success paymentAuthResult,
                         @NonNull final SEPADirectDebitTokenizeCallback callback) {
        BrowserSwitchResultInfo browserSwitchResult =
                paymentAuthResult.getPaymentAuthInfo().getBrowserSwitchResultInfo();

        Uri deepLinkUri = browserSwitchResult.getDeepLinkUrl();
        if (deepLinkUri != null) {
            if (deepLinkUri.getPath().contains("success") &&
                    deepLinkUri.getQueryParameter("success").equals("true")) {
                braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.CHALLENGE_SUCCEEDED);
                JSONObject metadata = browserSwitchResult.getRequestMetadata();
                String ibanLastFour = metadata.optString(IBAN_LAST_FOUR_KEY);
                String customerId = metadata.optString(CUSTOMER_ID_KEY);
                String bankReferenceToken = metadata.optString(BANK_REFERENCE_TOKEN_KEY);
                String mandateType = metadata.optString(MANDATE_TYPE_KEY);

                sepaDirectDebitApi.tokenize(ibanLastFour, customerId, bankReferenceToken,
                        mandateType,
                        (sepaDirectDebitNonce, error) -> {
                            if (sepaDirectDebitNonce != null) {
                                callbackTokenizeSuccess(callback, new SEPADirectDebitResult.Success(sepaDirectDebitNonce));
                            } else if (error != null) {
                                callbackTokenizeFailure(callback, new SEPADirectDebitResult.Failure(error));
                            }
                        });
            } else if (deepLinkUri.getPath().contains("cancel")) {
                callbackTokenizeCancel(callback);
            }
        } else {
            braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.CHALLENGE_FAILED);
            callbackTokenizeFailure(callback, new SEPADirectDebitResult.Failure(new BraintreeException("Unknown error")));
        }
    }

    private void callbackCreatePaymentAuthFailure(SEPADirectDebitPaymentAuthRequestCallback callback, SEPADirectDebitPaymentAuthRequest.Failure result) {
        braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.TOKENIZE_FAILED);
        callback.onSEPADirectDebitPaymentAuthResult(result);
    }

    private void callbackCreatePaymentAuthChallengeNotRequiredSuccess(SEPADirectDebitPaymentAuthRequestCallback callback, SEPADirectDebitPaymentAuthRequest.LaunchNotRequired result) {
        braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.TOKENIZE_SUCCEEDED);
        callback.onSEPADirectDebitPaymentAuthResult(result);
    }

    private void callbackTokenizeCancel(SEPADirectDebitTokenizeCallback callback) {
        braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.CHALLENGE_CANCELED);
        callback.onSEPADirectDebitResult(SEPADirectDebitResult.Cancel.INSTANCE);
    }

    private void callbackTokenizeFailure(SEPADirectDebitTokenizeCallback callback, SEPADirectDebitResult.Failure result) {
        braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.TOKENIZE_FAILED);
        callback.onSEPADirectDebitResult(result);
    }

    private void callbackTokenizeSuccess(SEPADirectDebitTokenizeCallback callback, SEPADirectDebitResult.Success result) {
        braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.TOKENIZE_SUCCEEDED);
        callback.onSEPADirectDebitResult(result);
    }

    private BrowserSwitchOptions buildBrowserSwitchOptions(CreateMandateResult createMandateResult) throws JSONException {
        JSONObject metadata = new JSONObject()
                .put(IBAN_LAST_FOUR_KEY, createMandateResult.getIbanLastFour())
                .put(CUSTOMER_ID_KEY, createMandateResult.getCustomerId())
                .put(BANK_REFERENCE_TOKEN_KEY, createMandateResult.getBankReferenceToken())
                .put(MANDATE_TYPE_KEY, createMandateResult.getMandateType().toString());

        BrowserSwitchOptions browserSwitchOptions = new BrowserSwitchOptions()
                .requestCode(BraintreeRequestCodes.SEPA_DEBIT)
                .url(Uri.parse(createMandateResult.getApprovalUrl()))
                .metadata(metadata)
                .returnUrlScheme(braintreeClient.getReturnUrlScheme());

        return browserSwitchOptions;
    }
}
