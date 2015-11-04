package com.paypal.android.sdk.onetouch.core.base;

import com.paypal.android.lib.riskcomponent.RiskComponent;
import com.paypal.android.lib.riskcomponent.SourceApp;

import android.content.Context;

import java.util.Map;

public class MetadataIdProviderImpl implements MetadataIdProvider {
    private RiskComponent mRiskComponent = RiskComponent.getInstance();

    @Override
    public String init(Context context, String applicationGuid, SourceApp sourceApp, String sourceAppVersion, Map<String, Object> additionalParams) {
        return mRiskComponent.init(context,
                applicationGuid,
                sourceApp,
                sourceAppVersion,
                additionalParams);
    }

    @Override
    public void flush() {
        mRiskComponent.sendRiskPayload();
    }

    @Override
    public String generatePairingId(String pairingId) {
        return mRiskComponent.generatePairingId(pairingId);
    }

    @Override
    public String generatePairingId() {
        return mRiskComponent.generatePairingId();
    }

    @Override
    public String getLibraryVersion() {
        return mRiskComponent.getLibraryVersion();
    }

}
