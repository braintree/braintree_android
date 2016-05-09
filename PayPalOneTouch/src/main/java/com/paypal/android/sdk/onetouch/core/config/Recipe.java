package com.paypal.android.sdk.onetouch.core.config;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.IBinder;

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
        Intent intent = getBrowserIntent(context, browserSwitchUrl, allowedBrowserPackage);
        return (intent.resolveActivity(context.getPackageManager()) != null);
    }

    public static Intent getBrowserIntent(Context context, String browserSwitchUrl, String allowedBrowserPackage) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(browserSwitchUrl));

        if (!"*".equals(allowedBrowserPackage)) {
            intent.setPackage(allowedBrowserPackage);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR2 && isChromeCustomTabsAvailable(context)) {
            Bundle extras = new Bundle();
            extras.putBinder("android.support.customtabs.extra.SESSION", null);
            intent.putExtras(extras);
            intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        }

        return intent;
    }

    public Protocol getProtocol() {
        return mProtocol;
    }

    private static boolean isChromeCustomTabsAvailable(Context context) {
        Intent serviceIntent = new Intent("android.support.customtabs.action.CustomTabsService")
                .setPackage("com.android.chrome");
        ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {}

            @Override
            public void onServiceDisconnected(ComponentName name) {}
        };

        boolean chromeCustomTabsAvailable = context.bindService(serviceIntent, connection,
                Context.BIND_AUTO_CREATE | Context.BIND_WAIVE_PRIORITY);
        context.unbindService(connection);

        return chromeCustomTabsAvailable;
    }
}
