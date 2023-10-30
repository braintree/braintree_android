package com.braintreepayments.api;

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

    public SEPADirectDebitClient(@NonNull BraintreeClient braintreeClient) {
        this(braintreeClient, new SEPADirectDebitApi(braintreeClient));
    }

    @VisibleForTesting
    SEPADirectDebitClient(BraintreeClient braintreeClient, SEPADirectDebitApi sepaDirectDebitApi) {
        this.braintreeClient = braintreeClient;
        this.sepaDirectDebitApi = sepaDirectDebitApi;
    }

    /**
     * Initiates a browser switch to display a mandate to the user. Upon successful mandate
     * creation, tokenizes the payment method and returns a result to the
     * {@link SEPADirectDebitListener}.
     *
     * @param activity               an Android FragmentActivity
     * @param sepaDirectDebitRequest the {@link SEPADirectDebitRequest}.
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
                            SEPADirectDebitResponse sepaDirectDebitResponse = new SEPADirectDebitResponse(); // Does this need any data on it?
                            // TODO: - Need to provide browser switch options to avoid crash
                            callback.onResult(sepaDirectDebitResponse, null);
                        // TODO: - For the cases where we don't need a web-flow mandate, when do we want to notify the merchant of success
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
                                            // listener.onSEPADirectDebitSuccess(sepaDirectDebitNonce);
                                        } else if (tokenizeError != null) {
                                            braintreeClient.sendAnalyticsEvent(
                                                    "sepa-direct-debit.tokenize.failure");
                                            // listener.onSEPADirectDebitFailure(tokenizeError);
                                        }
                                    });
                        } else {
                            braintreeClient.sendAnalyticsEvent(
                                    "sepa-direct-debit.create-mandate.failure");
                            // listener.onSEPADirectDebitFailure(
                                    new BraintreeException("An unexpected error occurred.");
                        }
                    } else if (createMandateError != null) {
                        braintreeClient.sendAnalyticsEvent(
                                "sepa-direct-debit.create-mandate.failure");
                        // listener.onSEPADirectDebitFailure(createMandateError);
                    }
                });
    }

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

    BrowserSwitchResult getBrowserSwitchResult(FragmentActivity activity) {
        return braintreeClient.getBrowserSwitchResult(activity);
    }

    private void startBrowserSwitch(FragmentActivity activity,
                                    CreateMandateResult createMandateResult)
            throws JSONException, BrowserSwitchException {
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

        braintreeClient.startBrowserSwitch(activity, browserSwitchOptions);
        braintreeClient.sendAnalyticsEvent("sepa-direct-debit.browser-switch.started");
    }
}
