package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;

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
     * Create a new instance of {@link SEPADirectDebitClient} using a {@link BraintreeClient}.
     *
     * @param braintreeClient a {@link BraintreeClient}
     */
    public SEPADirectDebitClient(@NonNull BraintreeClient braintreeClient) {
        this(braintreeClient, new SEPADirectDebitApi(braintreeClient));
    }

    @VisibleForTesting
    SEPADirectDebitClient(BraintreeClient braintreeClient, SEPADirectDebitApi sepaDirectDebitApi) {
        this.braintreeClient = braintreeClient;
        this.sepaDirectDebitApi = sepaDirectDebitApi;
    }

    /**
     * Starts the SEPA tokenization process by creating a {@link SEPADirectDebitResponse} to be used
     * to launch the SEPA mandate flow in
     * {@link SEPADirectDebitLauncher#launch(FragmentActivity, SEPADirectDebitResponse)}
     *
     * @param activity Android FragmentActivity
     * @param sepaDirectDebitRequest {@link SEPADirectDebitRequest}
     * @param callback {@link SEPADirectDebitFlowStartedCallback}
     */
    public void tokenize(@NonNull final FragmentActivity activity,
                         @NonNull final SEPADirectDebitRequest sepaDirectDebitRequest,
                         @NonNull final SEPADirectDebitFlowStartedCallback callback) {
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
                                SEPADirectDebitResponse sepaDirectDebitResponse =
                                        new SEPADirectDebitResponse(buildBrowserSwitchOptions(result), null);
                                callback.onResult(sepaDirectDebitResponse, null);
                            } catch (JSONException exception) {
                                braintreeClient.sendAnalyticsEvent(
                                        "sepa-direct-debit.browser-switch.failure");
                                callback.onResult(null, exception);
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
                                            SEPADirectDebitResponse sepaDirectDebitResponse =
                                                    new SEPADirectDebitResponse(null, sepaDirectDebitNonce);
                                            callback.onResult(sepaDirectDebitResponse, null);
                                        } else if (tokenizeError != null) {
                                            braintreeClient.sendAnalyticsEvent(
                                                    "sepa-direct-debit.tokenize.failure");
                                            callback.onResult(null, tokenizeError);
                                        }
                                    });
                        } else {
                            braintreeClient.sendAnalyticsEvent(
                                    "sepa-direct-debit.create-mandate.failure");
                            callback.onResult(null, new BraintreeException("An unexpected error occurred."));
                        }
                    } else if (createMandateError != null) {
                        braintreeClient.sendAnalyticsEvent(
                                "sepa-direct-debit.create-mandate.failure");
                        callback.onResult(null, createMandateError);
                    }
                });
    }

    // TODO: - The wording in this docstring is confusing to me. Let's improve & align across all clients.
    /**
     * After receiving a result from the SEPA mandate web flow via
     * {@link SEPADirectDebitLauncher#handleReturnToAppFromBrowser(Context, Intent)}, pass the
     * {@link SEPADirectDebitBrowserSwitchResult} returned to this method to tokenize the SEPA
     * account and receive a {@link SEPADirectDebitNonce} on success.
     *
     * @param sepaDirectDebitBrowserSwitchResult a {@link SEPADirectDebitBrowserSwitchResult} received
     *                                           in the callback of {@link SEPADirectDebitLauncher}
     * @param callback {@link SEPADirectDebitBrowserSwitchResultCallback}
     */
    public void onBrowserSwitchResult(@NonNull SEPADirectDebitBrowserSwitchResult sepaDirectDebitBrowserSwitchResult,
                                      @NonNull final SEPADirectDebitBrowserSwitchResultCallback callback) {
        BrowserSwitchResult browserSwitchResult =
                sepaDirectDebitBrowserSwitchResult.getBrowserSwitchResult();
        if (browserSwitchResult == null && sepaDirectDebitBrowserSwitchResult.getError() != null) {
            callback.onResult(null, sepaDirectDebitBrowserSwitchResult.getError());
            return;
        }

        if (browserSwitchResult == null) {
            callback.onResult(null, new BraintreeException("An unexpected error occurred."));
        }

        int result = browserSwitchResult.getStatus();
        switch (result) {
            case BrowserSwitchStatus.CANCELED:
                braintreeClient.sendAnalyticsEvent("sepa-direct-debit.browser-switch.canceled");
                callback.onResult(null, new UserCanceledException("User canceled SEPA Debit."));
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
                                        callback.onResult(sepaDirectDebitNonce, null);
                                    } else if (error != null) {
                                        braintreeClient.sendAnalyticsEvent(
                                                "sepa-direct-debit.tokenize.failure");
                                        callback.onResult(null, error);
                                    }
                                });
                    } else if (deepLinkUri.getPath().contains("cancel")) {
                        braintreeClient.sendAnalyticsEvent(
                                "sepa-direct-debit.browser-switch.failure");
                        callback.onResult(null, new BraintreeException("An unexpected error occurred."));
                    }
                } else {
                    braintreeClient.sendAnalyticsEvent("sepa-direct-debit.browser-switch.failure");
                    callback.onResult(null, new BraintreeException("Unknown error"));
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
