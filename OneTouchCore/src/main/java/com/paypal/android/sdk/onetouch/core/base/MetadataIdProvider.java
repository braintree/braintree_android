package com.paypal.android.sdk.onetouch.core.base;

import com.paypal.android.lib.riskcomponent.SourceApp;

import android.content.Context;

import java.util.Map;

interface MetadataIdProvider {
    String init(Context context, String applicationGuid, SourceApp sourceApp, String sourceAppVersion, Map<String, Object> additionalParams);

    void flush();

    String generatePairingId(String pairingId);

    String generatePairingId();

    String getLibraryVersion();
}
