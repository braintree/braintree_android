package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.wallet.Wallet;

/**
 * Class representing Google Pay payment capabilities
 */
public class GooglePayCapabilities {

    /**
     * @return {@code true} if Google Pay is enabled and supported in the current environment,
     *         {@code false} otherwise. Note: this value only pertains to the Braintree configuration, to check if
     *         the user has Google Pay setup use {@link GooglePayClient#isReadyToPay(FragmentActivity, GooglePayIsReadyToPayCallback)}
     */
    public static boolean isGooglePayEnabled(@NonNull Context context, @NonNull Configuration configuration) {
        try {
            Class.forName(Wallet.class.getName());

            return configuration.isGooglePayEnabled() && GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) ==
                    ConnectionResult.SUCCESS;
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            return false;
        }
    }
}
