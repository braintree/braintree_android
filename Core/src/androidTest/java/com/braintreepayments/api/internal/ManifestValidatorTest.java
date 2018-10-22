package com.braintreepayments.api.internal;

import android.content.pm.ActivityInfo;
import androidx.test.runner.AndroidJUnit4;

import com.braintreepayments.api.test.ManifestTestActivity;
import com.braintreepayments.api.test.MissingManifestTestActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ManifestValidatorTest {

    @Test(timeout = 1000)
    public void isActivityDeclaredInAndroidManifest_returnsFalseForUndeclaredActivity() {
        assertFalse(ManifestValidator.isActivityDeclaredInAndroidManifest(getTargetContext(),
                MissingManifestTestActivity.class));
    }

    @Test(timeout = 1000)
    public void isActivityDeclaredInAndroidManifest_returnsTrueForDeclaredActivity() {
        assertTrue(ManifestValidator.isActivityDeclaredInAndroidManifest(getTargetContext(),
                ManifestTestActivity.class));
    }

    @Test(timeout = 1000)
    public void getActivityInfo_returnsNullForNonExistantActivity() {
        assertNull(ManifestValidator.getActivityInfo(getTargetContext(), MissingManifestTestActivity.class));
    }

    @Test(timeout = 1000)
    public void getActivityInfo_returnsActivityInfoForExistingActivity() {
        ActivityInfo activityInfo = ManifestValidator.getActivityInfo(getTargetContext(), ManifestTestActivity.class);

        assertNotNull(activityInfo);
        assertEquals(ManifestTestActivity.class.getName(), activityInfo.name);
    }
}
