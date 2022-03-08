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
 * Used to integrate with SEPA Debit.
 */
public class SEPADebitClient {

    private static final String IBAN_LAST_FOUR_KEY = "ibanLastFour";
    private static final String CUSTOMER_ID_KEY = "customerId";
    private static final String BANK_REFERENCE_TOKEN_KEY = "bankReferenceToken";
    private static final String MANDATE_TYPE_KEY = "mandateType";

    private final SEPADebitApi sepaDebitAPI;
    private final BraintreeClient braintreeClient;
    private SEPADebitListener listener;

    /**
     * Create a new instance of {@link SEPADebitClient} from within an Activity using a {@link BraintreeClient}.
     *
     * @param activity an Android FragmentActivity
     * @param braintreeClient a {@link BraintreeClient}
     */
    public SEPADebitClient(@NonNull FragmentActivity activity, @NonNull BraintreeClient braintreeClient) {
        this(activity, activity.getLifecycle(), braintreeClient, new SEPADebitApi());
    }

    /**
     * Create a new instance of {@link SEPADebitClient} from within a Fragment using a {@link BraintreeClient}.
     *
     * @param fragment an Android Fragment
     * @param braintreeClient a {@link BraintreeClient}
     */
    public SEPADebitClient(@NonNull Fragment fragment, @NonNull BraintreeClient braintreeClient) {
        this(fragment.getActivity(), fragment.getLifecycle(), braintreeClient, new SEPADebitApi());
    }

    @VisibleForTesting
    SEPADebitClient(FragmentActivity activity, Lifecycle lifecycle, BraintreeClient braintreeClient, SEPADebitApi sepaDebitAPI) {
        this.sepaDebitAPI = sepaDebitAPI;
        this.braintreeClient = braintreeClient;
        if (activity != null && lifecycle != null) {
            SEPADebitLifecycleObserver observer = new SEPADebitLifecycleObserver(this);
            lifecycle.addObserver(observer);
        }
    }

    /**
     * Add a {@link SEPADebitListener} to your client to receive results or errors from the SEPA Debit flow.
     *
     * @param listener a {@link SEPADebitListener}
     */
    public void setListener(SEPADebitListener listener) {
        // TODO: handle delivering pending browser switch result when listener is set
        this.listener = listener;
    }

    /**
     * Initiates a browser switch to display a mandate to the user. Upon successful mandate creation,
     * tokenizes the payment method and returns a result to the {@link SEPADebitListener}.
     *
     * @param activity an Android FragmentActivity
     * @param sepaDebitRequest the {@link SEPADebitRequest}.
     */
    public void tokenize(final FragmentActivity activity, final SEPADebitRequest sepaDebitRequest) {
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception configError) {
                if (configuration != null) {
                    sepaDebitAPI.createMandate(sepaDebitRequest, configuration, braintreeClient.getReturnUrlScheme(), new CreateMandateCallback() {
                        @Override
                        public void onResult(@Nullable CreateMandateResult result, @Nullable Exception error) {
                            if (result != null) {
                                if (URLUtil.isValidUrl(result.getApprovalUrl())) {
                                    try {
                                        startBrowserSwitch(activity, result);
                                    } catch (JSONException | BrowserSwitchException exception) {
                                        listener.onSEPADebitFailure(exception);
                                    }
                                } else if (result.getApprovalUrl().equals("null")) {
                                    // TODO: call SEPADebitApi#tokenize - null means the mandate is already approved
                                } else {
                                    listener.onSEPADebitFailure(new BraintreeException("An unexpected error occurred."));
                                }
                            } else if (error != null) {
                                listener.onSEPADebitFailure(error);
                            }
                        }
                    });
                } else if (configError != null) {
                    listener.onSEPADebitFailure(configError);
                }
            }
        });
    }

    void onBrowserSwitchResult(FragmentActivity activity) {
        BrowserSwitchResult browserSwitchResult = braintreeClient.deliverBrowserSwitchResult(activity);
        // deliver result to listener

        int result = browserSwitchResult.getStatus();
        switch (result) {
            case BrowserSwitchStatus.CANCELED:
                listener.onSEPADebitFailure(new UserCanceledException("User canceled SEPA Debit."));
                break;
            case BrowserSwitchStatus.SUCCESS:
                Uri deepLinkUri = browserSwitchResult.getDeepLinkUrl();
                if (deepLinkUri != null) {
                    if (deepLinkUri.getPath().contains("success") && deepLinkUri.getQueryParameter("success").equals("true")) {
                        JSONObject metadata = browserSwitchResult.getRequestMetadata();
                        String ibanLastFour = metadata.optString(IBAN_LAST_FOUR_KEY);
                        String customerId = metadata.optString(CUSTOMER_ID_KEY);
                        String bankReferenceToken = metadata.optString(BANK_REFERENCE_TOKEN_KEY);
                        String mandateType = metadata.optString(MANDATE_TYPE_KEY);
                        // TODO: call SEPADebitApi#tokenize with metadata params
                    } else if (deepLinkUri.getPath().contains("cancel")) {
                        // TODO: return unexpected error
                    }
                }
                // error
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
                .put(MANDATE_TYPE_KEY, createMandateResult.getMandateType());

        BrowserSwitchOptions browserSwitchOptions = new BrowserSwitchOptions()
                .requestCode(BraintreeRequestCodes.SEPA)
                .url(Uri.parse(createMandateResult.getApprovalUrl()))
                .metadata(metadata)
                .returnUrlScheme(braintreeClient.getReturnUrlScheme());

        braintreeClient.startBrowserSwitch(activity, browserSwitchOptions);
    }
}
