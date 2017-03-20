package com.paypal.android.sdk.onetouch.core.test;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

import com.paypal.android.sdk.onetouch.core.base.ContextInspector;

import org.robolectric.RuntimeEnvironment;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class TestSetupHelper {

    public static ContextInspector getMockContextInspector() throws NameNotFoundException {
        ApplicationInfo applicationInfoMock = mock(ApplicationInfo.class);
        when(applicationInfoMock.loadLabel(any(PackageManager.class))).thenReturn("application-name");
        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.packageName ="com.test";
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.applicationInfo = applicationInfoMock;
        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.name = "Browser";
        activityInfo.applicationInfo = applicationInfo;
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = activityInfo;
        PackageManager packageManager = mock(PackageManager.class);
        when(packageManager.getPackageInfo(anyString(), anyInt())).thenReturn(packageInfo);
        when(packageManager.resolveActivity(any(Intent.class), eq(PackageManager.MATCH_DEFAULT_ONLY)))
                .thenReturn(resolveInfo);
        TelephonyManager telephonyManager = mock(TelephonyManager.class);
        when(telephonyManager.getSimOperator()).thenReturn("12345");
        Context context = spy(RuntimeEnvironment.application);
        when(context.getPackageManager()).thenReturn(packageManager);
        when(context.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(telephonyManager);

        ContextInspector contextInspector = mock(ContextInspector.class);
        when(contextInspector.getContext()).thenReturn(context);

        RuntimeEnvironment.application.getSharedPreferences("PayPalOTC", Context.MODE_PRIVATE)
                .edit()
                .putString("InstallationGUID", "installation-guid")
                .apply();

        return contextInspector;
    }
}
