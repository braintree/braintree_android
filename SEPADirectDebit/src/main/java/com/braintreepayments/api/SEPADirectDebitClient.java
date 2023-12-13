package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;

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
     * {@link SEPADirectDebitLauncher#launch(FragmentActivity, SEPADirectDebitPaymentAuthRequest.ReadyToLaunch)}
     *
     * @param sepaDirectDebitRequest {@link SEPADirectDebitRequest}
     * @param callback {@link SEPADirectDebitPaymentAuthRequestCallback}
     */
    public void createPaymentAuthRequest(@NonNull final SEPADirectDebitRequest sepaDirectDebitRequest,
                                         @NonNull final SEPADirectDebitPaymentAuthRequestCallback callback) {
        braintreeClient.sendAnalyticsEvent("sepa-direct-debit.selected.started");
        braintreeClient.sendAnalyticsEvent("sepa-direct-debit.create-mandate.requested");
        sepaDirectDebitApi.createMandate(sepaDirectDebitRequest,
                braintreeClient.getReturnUrlScheme(),
                (result, createMandateError) -> {
                    if (result != null) {
                        if (URLUtil.isValidUrl(result.getApprovalUrl())) {
                            braintreeClient.sendAnalyticsEvent(
                                    "sepa-direct-debit.create-mandate.success");
                            try {
                                SEPADirectDebitPaymentAuthRequestParams params =
                                        new SEPADirectDebitPaymentAuthRequestParams(buildBrowserSwitchOptions(result));
                                callback.onResult(new SEPADirectDebitPaymentAuthRequest.ReadyToLaunch(params));
                            } catch (JSONException exception) {
                                braintreeClient.sendAnalyticsEvent(
                                        "sepa-direct-debit.browser-switch.failure");
                                callback.onResult(new SEPADirectDebitPaymentAuthRequest.Failure(exception));
                            }
                        } else if (result.getApprovalUrl().equals("null")) {
                            braintreeClient.sendAnalyticsEvent(
                                    "sepa-direct-debit.create-mandate.success");
                            // Mandate has already been approved
                            braintreeClient.sendAnalyticsEvent(
                                    "sepa-direct-debit.tokenize.requested");
                            sepaDirectDebitApi.tokenize(result.getIbanLastFour(),
                                    result.getCustomerId(), result.getBankReferenceToken(),
                                    result.getMandateType().toString(),
                                    (sepaDirectDebitNonce, tokenizeError) -> {
                                        if (sepaDirectDebitNonce != null) {
                                            braintreeClient.sendAnalyticsEvent(
                                                    "sepa-direct-debit.tokenize.success");
                                            callback.onResult(new SEPADirectDebitPaymentAuthRequest.LaunchNotRequired(sepaDirectDebitNonce));
                                        } else if (tokenizeError != null) {
                                            braintreeClient.sendAnalyticsEvent(
                                                    "sepa-direct-debit.tokenize.failure");
                                            callback.onResult(new SEPADirectDebitPaymentAuthRequest.Failure(tokenizeError));
                                        }
                                    });
                        } else {
                            braintreeClient.sendAnalyticsEvent(
                                    "sepa-direct-debit.create-mandate.failure");
                            callback.onResult(new SEPADirectDebitPaymentAuthRequest.Failure(new BraintreeException("An unexpected error occurred.")));
                        }
                    } else if (createMandateError != null) {
                        braintreeClient.sendAnalyticsEvent(
                                "sepa-direct-debit.create-mandate.failure");
                        callback.onResult(new SEPADirectDebitPaymentAuthRequest.Failure(createMandateError));
                    }
                });
    }

    // TODO: - The wording in this docstring is confusing to me. Let's improve & align across all clients.
    /**
     * After receiving a result from the SEPA mandate web flow via
     * {@link SEPADirectDebitLauncher#handleReturnToAppFromBrowser(Context, Intent)}, pass the
     * {@link SEPADirectDebitPaymentAuthResult} returned to this method to tokenize the SEPA
     * account and receive a {@link SEPADirectDebitNonce} on success.
     *
     * @param paymentAuthResult a {@link SEPADirectDebitPaymentAuthResult} received
     *                                           in the callback of {@link SEPADirectDebitLauncher}
     * @param callback {@link SEPADirectDebitInternalTokenizeCallback}
     */
    public void tokenize(@NonNull SEPADirectDebitPaymentAuthResult paymentAuthResult,
                         @NonNull final SEPADirectDebitTokenizeCallback callback) {
        BrowserSwitchResult browserSwitchResult =
                paymentAuthResult.getBrowserSwitchResult();
        if (browserSwitchResult == null && paymentAuthResult.getError() != null) {
            callback.onSEPADirectDebitResult(new SEPADirectDebitResult.Failure(paymentAuthResult.getError()));
            return;
        }

        if (browserSwitchResult == null) {
            callback.onSEPADirectDebitResult(new SEPADirectDebitResult.Failure(new BraintreeException("An unexpected error occurred.")));
            return;
        }

        int result = browserSwitchResult.getStatus();
        switch (result) {
            case BrowserSwitchStatus.CANCELED:
                braintreeClient.sendAnalyticsEvent("sepa-direct-debit.browser-switch.canceled");
                callback.onSEPADirectDebitResult(SEPADirectDebitResult.Cancel.INSTANCE);
                break;
            case BrowserSwitchStatus.SUCCESS:
                Uri deepLinkUri = browserSwitchResult.getDeepLinkUrl();
                if (deepLinkUri != null) {
                    if (deepLinkUri.getPath().contains("success") &&
                            deepLinkUri.getQueryParameter("success").equals("true")) {
                        braintreeClient.sendAnalyticsEvent(
                                "sepa-direct-debit.browser-switch.success");
                        JSONObject metadata = browserSwitchResult.getRequestMetadata();
                        String ibanLastFour = metadata.optString(IBAN_LAST_FOUR_KEY);
                        String customerId = metadata.optString(CUSTOMER_ID_KEY);
                        String bankReferenceToken = metadata.optString(BANK_REFERENCE_TOKEN_KEY);
                        String mandateType = metadata.optString(MANDATE_TYPE_KEY);

                        braintreeClient.sendAnalyticsEvent("sepa-direct-debit.tokenize.requested");
                        sepaDirectDebitApi.tokenize(ibanLastFour, customerId, bankReferenceToken,
                                mandateType,
                                (sepaDirectDebitNonce, error) -> {
                                    if (sepaDirectDebitNonce != null) {
                                        braintreeClient.sendAnalyticsEvent(
                                                "sepa-direct-debit.tokenize.success");
                                        callback.onSEPADirectDebitResult(new SEPADirectDebitResult.Success(sepaDirectDebitNonce));
                                    } else if (error != null) {
                                        braintreeClient.sendAnalyticsEvent(
                                                "sepa-direct-debit.tokenize.failure");
                                        callback.onSEPADirectDebitResult(new SEPADirectDebitResult.Failure(error));
                                    }
                                });
                    } else if (deepLinkUri.getPath().contains("cancel")) {
                        braintreeClient.sendAnalyticsEvent(
                                "sepa-direct-debit.browser-switch.failure");
                        callback.onSEPADirectDebitResult(new SEPADirectDebitResult.Failure(new BraintreeException("An unexpected error occurred.")));
                    }
                } else {
                    braintreeClient.sendAnalyticsEvent("sepa-direct-debit.browser-switch.failure");
                    callback.onSEPADirectDebitResult(new SEPADirectDebitResult.Failure(new BraintreeException("Unknown error")));
                }
                break;
        }
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
