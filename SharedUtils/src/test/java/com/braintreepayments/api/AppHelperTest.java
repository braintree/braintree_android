package com.braintreepayments.api;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import com.braintreepayments.api.AppHelper;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AppHelperTest {

    private Context context;
    private PackageManager packageManager;

    @Before
    public void beforeEach() {
        context = mock(Context.class);
        packageManager = mock(PackageManager.class);
    }

    @Test
    public void isAppInstalled_whenAppInfoExistsForPackageName_returnsTrue() throws NameNotFoundException {
        when(context.getPackageManager()).thenReturn(packageManager);
        when(packageManager.getApplicationInfo("package.name", 0)).thenReturn(mock(ApplicationInfo.class));

        AppHelper sut = new AppHelper();
        assertTrue(sut.isAppInstalled(context, "package.name"));
    }

    @Test
    public void isAppInstalled_whenAppInfoIsNullForPackageName_returnsTrue() throws NameNotFoundException {
        when(context.getPackageManager()).thenReturn(packageManager);
        when(packageManager.getApplicationInfo("package.name", 0)).thenReturn(null);

        AppHelper sut = new AppHelper();
        assertFalse(sut.isAppInstalled(context, "package.name"));
    }

    @Test
    public void isAppInstalled_whenAppInfoNotFoundForPackageName_returnsTrue() throws NameNotFoundException {
        when(context.getPackageManager()).thenReturn(packageManager);
        when(packageManager.getApplicationInfo("package.name", 0)).thenThrow(new NameNotFoundException());

        AppHelper sut = new AppHelper();
        assertFalse(sut.isAppInstalled(context, "package.name"));
    }
}