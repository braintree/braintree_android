package com.braintreepayments.api.test;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.test.mock.MockContentProvider;
import android.test.mock.MockCursor;

import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowContentResolver;

import java.util.Collections;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VenmoMockContext {

    private PackageManager mPackageManager;
    private MockCursor mMockCursor;

    public VenmoMockContext() {
        mPackageManager = mock(PackageManager.class);
    }

    public VenmoMockContext whitelistValue(String whitelistValue) {
        mMockCursor = mockCursor(whitelistValue);
        return this;
    }

    public VenmoMockContext venmoInstalled() {
        mPackageManager = mockPackageManager();
        return this;
    }

    public Context build() {
        Context context = mock(Context.class);
        when(context.getContentResolver()).thenReturn(RuntimeEnvironment.application.getContentResolver());
        when(context.getPackageManager()).thenReturn(mPackageManager);

        if (mMockCursor != null) {
            MockContentProvider mockContentProvider = new MockContentProvider(context) {
                @Override
                public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
                    return mMockCursor;
                }
            };
            ShadowContentResolver.registerProvider("com.venmo.whitelistprovider", mockContentProvider);
        }

        return context;
    }

    private MockCursor mockCursor(String whitelistValue) {
        final MockCursor mockCursor = mock(MockCursor.class);
        when(mockCursor.getCount()).thenReturn(1);
        when(mockCursor.getString(anyInt())).thenReturn(whitelistValue);
        when(mockCursor.moveToFirst()).thenReturn(true);
        return mockCursor;
    }

    private PackageManager mockPackageManager() {
        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = "com.venmo";
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = activityInfo;
        final PackageManager packageManager = mock(PackageManager.class);
        when(packageManager.queryIntentActivities(any(Intent.class), anyInt()))
                .thenReturn(Collections.singletonList(resolveInfo));
        return packageManager;
    }
}

