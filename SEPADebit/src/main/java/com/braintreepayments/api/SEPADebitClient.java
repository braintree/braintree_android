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
        // get browserSwitchResult from BraintreeClient
        // parse deep link URL from browser switch result
        // call SEPADebitAPI#tokenize method
        // deliver result to listener

        // TODO: find out what metadata is needed
        JSONObject metadata = browserSwitchResult.getRequestMetadata();

        int result = browserSwitchResult.getStatus();
        switch (result) {
            case BrowserSwitchStatus.CANCELED:
                listener.onSEPADebitFailure(new UserCanceledException("User canceled SEPA Debit."));
                break;
            case BrowserSwitchStatus.SUCCESS:
                Uri deepLinkUri = browserSwitchResult.getDeepLinkUrl();
                if (deepLinkUri != null) {
                    parseUrl(deepLinkUri);
                }
                break;
        }
    }

    private void parseUrl(Uri deeplinkUrl) {
        if (deeplinkUrl.getPath().contains("success")) {

        }
    }

    BrowserSwitchResult getBrowserSwitchResult(FragmentActivity activity) {
        return braintreeClient.getBrowserSwitchResult(activity);
    }

    private void startBrowserSwitch(FragmentActivity activity, CreateMandateResult createMandateResult) throws JSONException, BrowserSwitchException {
        // TODO: figure out what metadata we need

        BrowserSwitchOptions browserSwitchOptions = new BrowserSwitchOptions()
                .requestCode(BraintreeRequestCodes.SEPA)
                .url(Uri.parse(createMandateResult.getApprovalUrl()))
                .returnUrlScheme(braintreeClient.getReturnUrlScheme());

        braintreeClient.startBrowserSwitch(activity, browserSwitchOptions);
    }
}
