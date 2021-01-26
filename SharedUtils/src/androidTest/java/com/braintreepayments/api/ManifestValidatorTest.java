package com.braintreepayments.api;

import android.content.pm.ActivityInfo;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.braintreepayments.api.ManifestValidator;
import com.braintreepayments.api.ManifestTestActivity;
import com.braintreepayments.api.MissingManifestTestActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4ClassRunner.class)
public class ManifestValidatorTest {

    @Test(timeout = 1000)
    public void isActivityDeclaredInAndroidManifest_returnsFalseForUndeclaredActivity() {
        ManifestValidator sut = new ManifestValidator();
        assertFalse(sut.isActivityDeclaredInAndroidManifest(ApplicationProvider.getApplicationContext(),
                MissingManifestTestActivity.class));
    }

    @Test(timeout = 1000)
    public void isActivityDeclaredInAndroidManifest_returnsTrueForDeclaredActivity() {
        ManifestValidator sut = new ManifestValidator();
        assertTrue(sut.isActivityDeclaredInAndroidManifest(ApplicationProvider.getApplicationContext(),
                ManifestTestActivity.class));
    }

    @Test(timeout = 1000)
    public void getActivityInfo_returnsNullForNonExistantActivity() {
        ManifestValidator sut = new ManifestValidator();
        assertNull(sut.getActivityInfo(ApplicationProvider.getApplicationContext(), MissingManifestTestActivity.class));
    }

    @Test(timeout = 1000)
    public void getActivityInfo_returnsActivityInfoForExistingActivity() {
        ManifestValidator sut = new ManifestValidator();
        ActivityInfo activityInfo = sut.getActivityInfo(ApplicationProvider.getApplicationContext(), ManifestTestActivity.class);

        assertNotNull(activityInfo);
        assertEquals(ManifestTestActivity.class.getName(), activityInfo.name);
    }
}
