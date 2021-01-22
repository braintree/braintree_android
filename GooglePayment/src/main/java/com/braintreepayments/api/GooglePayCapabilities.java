package com.braintreepayments.api;

import android.content.Context;

import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.models.GooglePaymentConfiguration;
import com.braintreepayments.api.models.ReadyForGooglePaymentRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.wallet.Wallet;

public class GooglePayCapabilities {

    /**
     * @return {@code true} if Google Payment is enabled and supported in the current environment,
     *         {@code false} otherwise. Note: this value only pertains to the Braintree configuration, to check if
     *         the user has Google Payment setup use
     *         {@link com.braintreepayments.api.GooglePaymentClient#isReadyToPay(FragmentActivity, ReadyForGooglePaymentRequest, GooglePaymentIsReadyToPayCallback)}
     */
    public static boolean isGooglePayEnabled(Context context, GooglePaymentConfiguration googlePaymentConfiguration) {
        try {
            Class.forName(Wallet.class.getName());

            return googlePaymentConfiguration.isEnabled() && GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) ==
                    ConnectionResult.SUCCESS;
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            return false;
        }
    }
}
