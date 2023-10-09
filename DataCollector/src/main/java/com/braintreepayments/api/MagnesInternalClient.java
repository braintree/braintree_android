package com.braintreepayments.api;

import android.content.Context;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.VisibleForTesting;

import lib.android.paypal.com.magnessdk.Environment;
import lib.android.paypal.com.magnessdk.InvalidInputException;
import lib.android.paypal.com.magnessdk.MagnesResult;
import lib.android.paypal.com.magnessdk.MagnesSDK;
import lib.android.paypal.com.magnessdk.MagnesSettings;
import lib.android.paypal.com.magnessdk.MagnesSource;

class MagnesInternalClient {

    private final MagnesSDK magnesSDK;

    MagnesInternalClient() {
        this(MagnesSDK.getInstance());
    }

    @VisibleForTesting
    MagnesInternalClient(MagnesSDK magnesSDK) {
        this.magnesSDK = magnesSDK;
    }

    @MainThread
    String getClientMetadataId(Context context, Configuration configuration, PayPalDataCollectorRequest request) {
        if (context == null) {
            return "";
        }

        String btEnvironment = configuration.getEnvironment();
        Environment magnesEnvironment = btEnvironment.equalsIgnoreCase("sandbox")
                ? Environment.SANDBOX : Environment.LIVE;

        MagnesSettings.Builder magnesSettingsBuilder = null;
        try {
            magnesSettingsBuilder = new MagnesSettings.Builder(context.getApplicationContext())
                    .setMagnesSource(MagnesSource.BRAINTREE)
                    .disableBeacon(request.isDisableBeacon())
                    .setMagnesEnvironment(magnesEnvironment)
                    .setAppGuid(request.getApplicationGuid());

            magnesSDK.setUp(magnesSettingsBuilder.build());

            MagnesResult result = magnesSDK.collectAndSubmit(context.getApplicationContext(), request.getClientMetadataId(), request.getAdditionalData());
            return result.getPaypalClientMetaDataId();
        } catch (InvalidInputException e) {
            // Either clientMetadataId or appGuid exceeds their character limit
            Log.e("Exception", "Error fetching client metadata ID. Contact Braintree Support for assistance.", e);
            return "";
        }
    }
}
