package com.braintreepayments.api.models;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class CoinbaseAccountBuilder implements PaymentMethod.Builder<CoinbaseAccount> {

    private String mCode;
    private PaymentMethodOptions mOptions;
    private String mIntegration = "custom";
    private String mRedirectUri;
    private String mSource;

    public CoinbaseAccountBuilder code(String code) {
        mCode = code;
        return this;
    }

    public CoinbaseAccountBuilder redirectUri(String redirectUri) {
        mRedirectUri = redirectUri;
        return this;
    }

    public CoinbaseAccountBuilder storeInVault(boolean storeInVault) {
        if (mOptions == null) {
            mOptions = new PaymentMethodOptions();
        }
        mOptions.setStoreInVault(storeInVault);
        return this;
    }

    @Override
    public CoinbaseAccountBuilder integration(String integration) {
        mIntegration = integration;
        return this;
    }

    @Override
    public CoinbaseAccountBuilder source(String source) {
        mSource = source;
        return this;
    }

    @Override
    public CoinbaseAccount build() {
        CoinbaseAccount account = new CoinbaseAccount();
        account.setAccessCode(mCode);
        account.setOptions(mOptions);
        account.setSource(mSource);
        account.setRedirectUri(mRedirectUri);
        return account;
    }

    @Override
    public Map<String, Object> toJson() {
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("coinbaseAccount", build());
        params.put(PaymentMethod.Builder.METADATA_KEY, new Metadata(mIntegration, mSource));
        return params;
    }

    @Override
    public String toJsonString() {
        return new Gson().toJson(toJson());
    }

    @Override
    public CoinbaseAccountBuilder validate(boolean validate) {
        if (mOptions == null) {
            mOptions = new PaymentMethodOptions();
        }
        mOptions.setValidate(validate);
        return this;
    }

    @Override
    public CoinbaseAccount fromJson(String json) {
        return CoinbaseAccount.fromJson(json);
    }

    @Override
    public String getApiPath() {
        return "coinbase_accounts";
    }

    @Override
    public String getApiResource() {
        return "coinbaseAccounts";
    }
}

