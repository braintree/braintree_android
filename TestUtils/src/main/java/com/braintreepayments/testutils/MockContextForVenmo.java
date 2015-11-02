package com.braintreepayments.testutils;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.test.mock.MockContentProvider;
import android.test.mock.MockContentResolver;
import android.test.mock.MockContext;
import android.test.mock.MockCursor;

import java.util.Collections;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockContextForVenmo {

    private PackageManager mPackageManager = null;
    private MockCursor mMockCursor = null;
    private MockContentResolver mContentResolver = null;

    public MockContextForVenmo whitelistValue(String whitelistValue) {
        mMockCursor = mockCursor(whitelistValue);
        mContentResolver = new MockContentResolver();
        return this;
    }

    public MockContextForVenmo venmoInstalled() {
        mPackageManager = mockPackageManager();
        return this;
    }

    public MockContext build() {
        MockContext mockContext = new MockContext() {
            @Override
            public ContentResolver getContentResolver() {
                if (mContentResolver == null) {
                    return mock(ContentResolver.class);
                } else {
                    return mContentResolver;
                }
            }

            @Override
            public PackageManager getPackageManager() {
                if (mPackageManager == null) {
                    return mock(PackageManager.class);
                } else {
                    return mPackageManager;
                }
            }
        };

        if (mMockCursor != null) {
            MockContentProvider mockContentProvider = mockContentProvider(mockContext, mMockCursor);
            mContentResolver.addProvider("com.venmo.whitelistprovider", mockContentProvider);
        }

        return mockContext;
    }

    private MockCursor mockCursor(String whitelistValue) {
        final MockCursor mockCursor = mock(MockCursor.class);
        when(mockCursor.getCount()).thenReturn(1);
        when(mockCursor.getString(anyInt())).thenReturn(whitelistValue);
        when(mockCursor.moveToFirst()).thenReturn(true);
        return mockCursor;
    }

    private MockContentProvider mockContentProvider(MockContext mockContext,
            final MockCursor mockCursor) {
        MockContentProvider mockContentProvider = new MockContentProvider(mockContext) {
            @Override
            public Cursor query(Uri uri, String[] projection, String selection,
                    String[] selectionArgs,
                    String sortOrder) {
                return mockCursor;
            }
        };
        mContentResolver.addProvider("com.venmo.whitelistprovider", mockContentProvider);
        return mockContentProvider;
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

