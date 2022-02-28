package com.braintreepayments.api;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

/**
 * Used to integrate with SEPA Debit.
 */
public class SEPADebitClient {

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
    public void tokenize(FragmentActivity activity, SEPADebitRequest sepaDebitRequest) {
        // create mandate request from sepaDebitRequest properties
        // browser switch to show mandate
    }

    void onBrowserSwitchResult(FragmentActivity activity) {
        // get browserSwitchResult from BraintreeClient
        // parse deep link URL from browser switch result
        // call SEPADebitAPI#tokenize method
        // deliver result to listener
    }
}
