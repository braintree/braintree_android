package com.braintreepayments.api.internal;

import android.content.pm.ActivityInfo;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.braintreepayments.api.test.ManifestTestActivity;
import com.braintreepayments.api.test.MissingManifestTestActivity;

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
        assertFalse(ManifestValidator.isActivityDeclaredInAndroidManifest(ApplicationProvider.getApplicationContext(),
                MissingManifestTestActivity.class));
    }

    @Test(timeout = 1000)
    public void isActivityDeclaredInAndroidManifest_returnsTrueForDeclaredActivity() {
        assertTrue(ManifestValidator.isActivityDeclaredInAndroidManifest(ApplicationProvider.getApplicationContext(),
                ManifestTestActivity.class));
    }

    @Test(timeout = 1000)
    public void getActivityInfo_returnsNullForNonExistantActivity() {
        assertNull(ManifestValidator.getActivityInfo(ApplicationProvider.getApplicationContext(), MissingManifestTestActivity.class));
    }

    @Test(timeout = 1000)
    public void getActivityInfo_returnsActivityInfoForExistingActivity() {
        ActivityInfo activityInfo = ManifestValidator.getActivityInfo(ApplicationProvider.getApplicationContext(), ManifestTestActivity.class);

        assertNotNull(activityInfo);
        assertEquals(ManifestTestActivity.class.getName(), activityInfo.name);
    }
}
