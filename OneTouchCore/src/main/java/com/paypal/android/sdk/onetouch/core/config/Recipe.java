package com.paypal.android.sdk.onetouch.core.config;

import com.paypal.android.sdk.onetouch.core.Protocol;
import com.paypal.android.sdk.onetouch.core.RequestTarget;
import com.paypal.android.sdk.onetouch.core.sdk.WalletAppHelper;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public abstract class Recipe<T extends Recipe<T>> {
    private static final String TAG = Recipe.class.getSimpleName();

    protected List<String> targetPackagesInReversePriorityOrder;
    private RequestTarget target;
    private Protocol protocol;
    private String targetComponent;
    private String targetIntentAction;
    private Collection<String> supportedLocales;

    public Recipe() {
        targetPackagesInReversePriorityOrder = new ArrayList<>();
        supportedLocales = new HashSet<>();
    }

    public T target(RequestTarget target) {
        this.target = target;
        return getThis();
    }

    public T protocol(String protocol) {
        switch(protocol){
            case "0":
                this.protocol = Protocol.v0;
                break;

            case "1":
                this.protocol = Protocol.v1;
                break;

            case "2":
                this.protocol = Protocol.v2;
                break;

            case "3":
                this.protocol = Protocol.v3;
                break;

            default:
                throw new IllegalArgumentException("invalid protocol");

        }

        return getThis();
    }

    public T targetPackage(String singleTargetPackage) {
        this.targetPackagesInReversePriorityOrder.add(singleTargetPackage);
        return getThis();
    }

    public List<String> getTargetPackagesInReversePriorityOrder() {
        return new ArrayList<>(targetPackagesInReversePriorityOrder);
    }


    public T supportedLocale(String supportedLocale) {
        this.supportedLocales.add(supportedLocale);
        return getThis();
    }

    public T targetComponent(String targetComponent) {
        this.targetComponent = targetComponent;
        return getThis();
    }

    public T targetIntentAction(String targetIntentAction) {
        this.targetIntentAction = targetIntentAction;
        return getThis();
    }

    public String getTargetComponent() {
        return targetComponent;
    }

    public String getTargetIntentAction() {
        return targetIntentAction;
    }

    public RequestTarget getTarget() {
        return target;
    }

    public abstract T getThis();

    public boolean isValidAppTarget(Context context, boolean isSecurityEnabled) {
        for(String allowedWalletTarget: getTargetPackagesInReversePriorityOrder()) {
            boolean isConfiguredToAcceptIntent = new WalletAppHelper().isWalletIntentSafe(
                    context,
                    getTargetIntentAction(),
                    getTargetComponent());

            String locale = Locale.getDefault().toString();

            // if no locales are specified, then presumed to be allowed for all
            boolean isLocaleAllowed = supportedLocales.isEmpty() || supportedLocales.contains(locale);

            boolean isValidTarget = new WalletAppHelper().isValidGenericAuthenticatorInstalled(context, isSecurityEnabled, allowedWalletTarget)
                    && isConfiguredToAcceptIntent && isLocaleAllowed;

            if(isValidTarget){
                return true;
            }
        }

        return false;
    }

    public boolean isValidBrowserTarget(Context context, String browserSwitchUrl) {
        for(String allowedBrowserPackage: getTargetPackagesInReversePriorityOrder()) {
            boolean canBeResolved = isValidBrowserTarget(context, browserSwitchUrl, allowedBrowserPackage);
            if(canBeResolved){
                return true;
            }
        }

        return false;
    }

    public boolean isValidBrowserTarget(Context context, String browserSwitchUrl, String allowedBrowserPackage){
        Intent intent = getBrowserIntent(browserSwitchUrl, allowedBrowserPackage);
        boolean canIntentBeResolved = intent.resolveActivity(context.getPackageManager()) != null;

        Log.d(TAG, "browser intent with package:" + intent.getPackage() + " can " + (canIntentBeResolved ? "" : "NOT ") + "be resolved");

        if(canIntentBeResolved){
            return true;
        }

        return false;
    }

    public static Intent getBrowserIntent(String browserSwitchUrl, String allowedBrowserPackage) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(browserSwitchUrl));
        if ("*".equals(allowedBrowserPackage)) {
            // don't set package - any is allowed at this point
        } else {
            intent.setPackage(allowedBrowserPackage);
        }
        return intent;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    @Override
    public String toString() {
        return "Recipe(target=" + target + ", protocol=" + protocol + ", packages=" + targetPackagesInReversePriorityOrder +
                ", targetComponent=" + targetComponent + ", targetIntentAction=" + targetIntentAction + ", supportedLocales=" + supportedLocales +")";
    }
}
