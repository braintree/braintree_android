package com.paypal.android.sdk.onetouch.core.config;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;

import com.braintreepayments.api.internal.AppHelper;
import com.paypal.android.sdk.onetouch.core.enums.Protocol;
import com.paypal.android.sdk.onetouch.core.enums.RequestTarget;
import com.paypal.android.sdk.onetouch.core.sdk.AppSwitchHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public abstract class Recipe<T extends Recipe<T>> {

    private static final String EXTRA_CUSTOM_TABS_SESSION = "android.support.customtabs.extra.SESSION";

    private List<String> mTargetPackagesInReversePriorityOrder;
    private RequestTarget mTarget;
    private Protocol mProtocol;
    private String mTargetIntentAction;
    private Collection<String> mSupportedLocales;

    public Recipe() {
        mTargetPackagesInReversePriorityOrder = new ArrayList<>();
        mSupportedLocales = new HashSet<>();
    }

    public T target(RequestTarget target) {
        mTarget = target;
        return getThis();
    }

    public T protocol(String protocol) {
        mProtocol = Protocol.getProtocol(protocol);
        return getThis();
    }

    public T targetPackage(String singleTargetPackage) {
        mTargetPackagesInReversePriorityOrder.add(singleTargetPackage);
        return getThis();
    }

    public List<String> getTargetPackagesInReversePriorityOrder() {
        return new ArrayList<>(mTargetPackagesInReversePriorityOrder);
    }

    public T supportedLocale(String supportedLocale) {
        mSupportedLocales.add(supportedLocale);
        return getThis();
    }

    public T targetIntentAction(String targetIntentAction) {
        mTargetIntentAction = targetIntentAction;
        return getThis();
    }

    public String getTargetIntentAction() {
        return mTargetIntentAction;
    }

    public RequestTarget getTarget() {
        return mTarget;
    }

    protected abstract T getThis();

    public boolean isValidAppTarget(Context context) {
        for (String allowedWalletTarget : getTargetPackagesInReversePriorityOrder()) {
            boolean isIntentAvailable = AppHelper.isIntentAvailable(context,
                    AppSwitchHelper.createBaseIntent(getTargetIntentAction(), allowedWalletTarget));

            String locale = Locale.getDefault().toString();
            // if no locales are specified, then presumed to be allowed for all
            boolean isLocaleAllowed =
                    mSupportedLocales.isEmpty() || mSupportedLocales.contains(locale);

            boolean isSignatureValid = AppSwitchHelper.isSignatureValid(context, allowedWalletTarget);

            if (isIntentAvailable && isLocaleAllowed && isSignatureValid) {
                return true;
            }
        }

        return false;
    }

    public boolean isValidBrowserTarget(Context context, String browserSwitchUrl) {
        for (String allowedBrowserPackage : getTargetPackagesInReversePriorityOrder()) {
            boolean canBeResolved =
                    isValidBrowserTarget(context, browserSwitchUrl, allowedBrowserPackage);
            if (canBeResolved) {
                return true;
            }
        }

        return false;
    }

    public static boolean isValidBrowserTarget(Context context, String browserSwitchUrl, String allowedBrowserPackage) {
        Intent intent = getBrowserIntent(browserSwitchUrl, allowedBrowserPackage);
        return (intent.resolveActivity(context.getPackageManager()) != null);
    }

    public static Intent getBrowserIntent(String browserSwitchUrl, String allowedBrowserPackage) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(browserSwitchUrl));

        if (!"*".equals(allowedBrowserPackage)) {
            intent.setPackage(allowedBrowserPackage);
        }

        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR2) {
            Bundle extras = new Bundle();
            extras.putBinder(EXTRA_CUSTOM_TABS_SESSION, null);
            intent.putExtras(extras);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS |
                    Intent.FLAG_ACTIVITY_NO_HISTORY);
        }

        return intent;
    }

    public Protocol getProtocol() {
        return mProtocol;
    }
}
