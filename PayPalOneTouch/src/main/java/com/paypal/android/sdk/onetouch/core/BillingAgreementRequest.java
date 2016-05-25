package com.paypal.android.sdk.onetouch.core;

import android.content.Context;
import android.os.Parcel;

import com.paypal.android.sdk.onetouch.core.config.BillingAgreementRecipe;
import com.paypal.android.sdk.onetouch.core.config.OtcConfiguration;
import com.paypal.android.sdk.onetouch.core.config.Recipe;
import com.paypal.android.sdk.onetouch.core.enums.RequestTarget;

public class BillingAgreementRequest extends CheckoutRequest {

    private static final String TOKEN_QUERY_PARAM_KEY_BA_TOKEN = "ba_token";

    public BillingAgreementRequest() {}

    @Override
    public BillingAgreementRequest pairingId(String pairingId) {
        super.pairingId(pairingId);
        return this;
    }

    @Override
    public BillingAgreementRequest approvalURL(String approvalURL) {
        super.approvalURL(approvalURL);
        mTokenQueryParamKey = TOKEN_QUERY_PARAM_KEY_BA_TOKEN;
        return this;
    }

    @Override
    public Recipe getRecipeToExecute(Context context, OtcConfiguration config) {
        for (BillingAgreementRecipe recipe : config.getBillingAgreementRecipes()) {
            if (RequestTarget.wallet == recipe.getTarget()) {
                if (recipe.isValidAppTarget(context)) {
                    return recipe;
                }
            } else if (RequestTarget.browser == recipe.getTarget()) {
                String browserSwitchUrl = getBrowserSwitchUrl(context, config);

                if (recipe.isValidBrowserTarget(context, browserSwitchUrl)) {
                    return recipe;
                }
            }
        }
        return null;
    }

    protected BillingAgreementRequest(Parcel source) {
        super(source);
    }

    public static final Creator<BillingAgreementRequest> CREATOR = new Creator<BillingAgreementRequest>() {
        @Override
        public BillingAgreementRequest[] newArray(int size) {
            return new BillingAgreementRequest[size];
        }

        @Override
        public BillingAgreementRequest createFromParcel(Parcel source) {
            return new BillingAgreementRequest(source);
        }
    };
}
