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
    private SEPADirectDebitListener listener;

    /**
     * Create a new instance of {@link SEPADirectDebitClient} from within an Activity using a {@link BraintreeClient}.
     *
     * @param activity an Android FragmentActivity
     * @param braintreeClient a {@link BraintreeClient}
     */
    public SEPADirectDebitClient(@NonNull FragmentActivity activity, @NonNull BraintreeClient braintreeClient) {
        this(activity, activity.getLifecycle(), braintreeClient, new SEPADirectDebitApi(braintreeClient));
    }

    /**
     * Create a new instance of {@link SEPADirectDebitClient} from within a Fragment using a {@link BraintreeClient}.
     *
     * @param fragment an Android Fragment
     * @param braintreeClient a {@link BraintreeClient}
     */
    public SEPADirectDebitClient(@NonNull Fragment fragment, @NonNull BraintreeClient braintreeClient) {
        this(fragment.getActivity(), fragment.getLifecycle(), braintreeClient, new SEPADirectDebitApi(braintreeClient));
    }

    @VisibleForTesting
    SEPADirectDebitClient(FragmentActivity activity, Lifecycle lifecycle, BraintreeClient braintreeClient, SEPADirectDebitApi sepaDirectDebitApi) {
        this.sepaDirectDebitApi = sepaDirectDebitApi;
        this.braintreeClient = braintreeClient;
        if (activity != null && lifecycle != null) {
            SEPADirectDebitLifecycleObserver observer = new SEPADirectDebitLifecycleObserver(this);
            lifecycle.addObserver(observer);
        }
    }

    /**
     * Add a {@link SEPADirectDebitListener} to your client to receive results or errors from the SEPA Direct Debit flow.
     *
     * @param listener a {@link SEPADirectDebitListener}
     */
    public void setListener(SEPADirectDebitListener listener) {
        this.listener = listener;
    }

    /**
     * Initiates a browser switch to display a mandate to the user. Upon successful mandate creation,
     * tokenizes the payment method and returns a result to the {@link SEPADirectDebitListener}.
     *
     * @param activity an Android FragmentActivity
     * @param sepaDirectDebitRequest the {@link SEPADirectDebitRequest}.
     */
    public void tokenize(final FragmentActivity activity, final SEPADirectDebitRequest sepaDirectDebitRequest) {
        braintreeClient.sendAnalyticsEvent("sepa-direct-debit.selected.started");
        braintreeClient.sendAnalyticsEvent("sepa-direct-debit.create-mandate.requested");
        sepaDirectDebitApi.createMandate(sepaDirectDebitRequest, braintreeClient.getReturnUrlScheme(), new CreateMandateCallback() {
            @Override
            public void onResult(@Nullable CreateMandateResult result, @Nullable Exception createMandateError) {
                if (result != null) {
                    if (URLUtil.isValidUrl(result.getApprovalUrl())) {
                        braintreeClient.sendAnalyticsEvent("sepa-direct-debit.create-mandate.success");
                        try {
                            startBrowserSwitch(activity, result);
                        } catch (JSONException | BrowserSwitchException exception) {
                            braintreeClient.sendAnalyticsEvent("sepa-direct-debit.browser-switch.failure");
                            listener.onSEPADirectDebitFailure(exception);
                        }
                    } else if (result.getApprovalUrl().equals("null")) {
                        braintreeClient.sendAnalyticsEvent("sepa-direct-debit.create-mandate.success");
                        // Mandate has already been approved
                        braintreeClient.sendAnalyticsEvent("sepa-direct-debit.tokenize.requested");
                        sepaDirectDebitApi.tokenize(result.getIbanLastFour(), result.getCustomerId(), result.getBankReferenceToken(), result.getMandateType().toString(), new SEPADirectDebitTokenizeCallback() {
                            @Override
                            public void onResult(@Nullable SEPADirectDebitNonce sepaDirectDebitNonce, @Nullable Exception tokenizeError) {
                                if (sepaDirectDebitNonce != null) {
                                    braintreeClient.sendAnalyticsEvent("sepa-direct-debit.tokenize.success");
                                    listener.onSEPADirectDebitSuccess(sepaDirectDebitNonce);
                                } else if (tokenizeError != null) {
                                    braintreeClient.sendAnalyticsEvent("sepa-direct-debit.tokenize.failure");
                                    listener.onSEPADirectDebitFailure(tokenizeError);
                                }
                            }
                        });
                    } else {
                        braintreeClient.sendAnalyticsEvent("sepa-direct-debit.create-mandate.failure");
                        listener.onSEPADirectDebitFailure(new BraintreeException("An unexpected error occurred."));
                    }
                } else if (createMandateError != null) {
                    braintreeClient.sendAnalyticsEvent("sepa-direct-debit.create-mandate.failure");
                    listener.onSEPADirectDebitFailure(createMandateError);
                }
            }
        });
    }

    void onBrowserSwitchResult(FragmentActivity activity) {
        BrowserSwitchResult browserSwitchResult = braintreeClient.deliverBrowserSwitchResult(activity);

        int result = browserSwitchResult.getStatus();
        switch (result) {
            case BrowserSwitchStatus.CANCELED:
                braintreeClient.sendAnalyticsEvent("sepa-direct-debit.browser-switch.canceled");
                listener.onSEPADirectDebitFailure(new UserCanceledException("User canceled SEPA Debit."));
                break;
            case BrowserSwitchStatus.SUCCESS:
                Uri deepLinkUri = browserSwitchResult.getDeepLinkUrl();
                if (deepLinkUri != null) {
                    if (deepLinkUri.getPath().contains("success") && deepLinkUri.getQueryParameter("success").equals("true")) {
                        braintreeClient.sendAnalyticsEvent("sepa-direct-debit.browser-switch.success");
                        JSONObject metadata = browserSwitchResult.getRequestMetadata();
                        String ibanLastFour = metadata.optString(IBAN_LAST_FOUR_KEY);
                        String customerId = metadata.optString(CUSTOMER_ID_KEY);
                        String bankReferenceToken = metadata.optString(BANK_REFERENCE_TOKEN_KEY);
                        String mandateType = metadata.optString(MANDATE_TYPE_KEY);

                        braintreeClient.sendAnalyticsEvent("sepa-direct-debit.tokenize.requested");
                        sepaDirectDebitApi.tokenize(ibanLastFour, customerId, bankReferenceToken, mandateType, new SEPADirectDebitTokenizeCallback() {
                            @Override
                            public void onResult(@Nullable SEPADirectDebitNonce sepaDirectDebitNonce, @Nullable Exception error) {
                                if (sepaDirectDebitNonce != null) {
                                    braintreeClient.sendAnalyticsEvent("sepa-direct-debit.tokenize.success");
                                    listener.onSEPADirectDebitSuccess(sepaDirectDebitNonce);
                                } else if (error != null) {
                                    braintreeClient.sendAnalyticsEvent("sepa-direct-debit.tokenize.failure");
                                    listener.onSEPADirectDebitFailure(error);
                                }
                            }
                        });
                    } else if (deepLinkUri.getPath().contains("cancel")) {
                        braintreeClient.sendAnalyticsEvent("sepa-direct-debit.browser-switch.failure");
                        listener.onSEPADirectDebitFailure(new BraintreeException("An unexpected error occurred."));
                    }
                } else {
                    braintreeClient.sendAnalyticsEvent("sepa-direct-debit.browser-switch.failure");
                    listener.onSEPADirectDebitFailure(new BraintreeException("Unknown error"));
                }
                break;
        }
    }

    BrowserSwitchResult getBrowserSwitchResult(FragmentActivity activity) {
        return braintreeClient.getBrowserSwitchResult(activity);
    }

    private void startBrowserSwitch(FragmentActivity activity, CreateMandateResult createMandateResult) throws JSONException, BrowserSwitchException {
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
