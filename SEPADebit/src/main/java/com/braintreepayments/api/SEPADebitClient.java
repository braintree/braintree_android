package com.braintreepayments.api;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;

import org.json.JSONException;

/**
 * Used to integrate with SEPA Debit.
 */
public class SEPADebitClient {

    private SEPADebitApi sepaDebitAPI;
    private BraintreeClient braintreeClient;

    /**
     * Create a new instance of {@link SEPADebitClient} from within an Activity using a {@link BraintreeClient}.
     *
     * @param activity an Android FragmentActivity
     * @param braintreeClient a {@link BraintreeClient}
     */
    public SEPADebitClient(@NonNull FragmentActivity activity, @NonNull BraintreeClient braintreeClient) {

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
    }

    /**
     * Add a {@link SEPADebitListener} to your client to receive results or errors from the SEPA Debit flow.
     *
     * @param listener a {@link SEPADebitListener}
     */
    public void setListener(SEPADebitListener listener) {

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
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (configuration != null) {
                    sepaDebitAPI.createMandate(sepaDebitRequest, configuration, new CreateMandateCallback() {
                        @Override
                        public void onResult(@Nullable CreateMandateResult result, @Nullable Exception error) {
                            if (result != null) {
                                Log.d("GOT A RESULT", result.getApprovalUrl());
                                try {
                                    startBrowserSwitch(activity, result);
                                } catch (JSONException | BrowserSwitchException exception) {
                                    // TODO: return error to listener
                                }
                            }
                        }
                    });
                } else {
                    // TODO: handle error
                }
            }
        });
    }

    void onBrowserSwitchResult(FragmentActivity activity) {
        // get browserSwitchResult from BraintreeClient
        // parse deep link URL from browser switch result
        // call SEPADebitAPI#tokenize method
        // deliver result to listener
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
