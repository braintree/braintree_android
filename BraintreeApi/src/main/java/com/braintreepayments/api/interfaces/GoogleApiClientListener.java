package com.braintreepayments.api.interfaces;

import com.google.android.gms.common.api.GoogleApiClient;

/**
 * Interface used to receive a {@link GoogleApiClient}.
 */
public interface GoogleApiClientListener {

    /**
     * @param googleApiClient An instance of a {@link GoogleApiClient} that is connected or
     *        connecting to be used for Android Pay. This instance will be automatically
     *        disconnected in {@link android.app.Fragment#onPause()} and automatically connected in
     *        {@link android.app.Fragment#onResume()}. Connection failed and connection suspended
     *        errors will be sent to {@link BraintreeErrorListener#onError(Exception)}.
     */
    void onResult(GoogleApiClient googleApiClient);
}
