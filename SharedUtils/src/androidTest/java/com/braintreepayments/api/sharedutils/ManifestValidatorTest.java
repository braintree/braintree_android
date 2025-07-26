package com.braintreepayments.api.sharedutils;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

import android.content.pm.ActivityInfo;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4ClassRunner.class)
public class ManifestValidatorTest {

    @Test(timeout = 1000)
    public void getActivityInfo_returnsNullForNonExistantActivity() {
        ManifestValidator sut = new ManifestValidator();
        assertNull(sut.getActivityInfo(ApplicationProvider.getApplicationContext(),
                MissingManifestTestActivity.class));
    }

    @Test(timeout = 1000)
    public void getActivityInfo_returnsActivityInfoForExistingActivity() {
        ManifestValidator sut = new ManifestValidator();
        ActivityInfo activityInfo = sut.getActivityInfo(ApplicationProvider.getApplicationContext(),
                ManifestTestActivity.class);

        assertNotNull(activityInfo);
        assertEquals(ManifestTestActivity.class.getName(), activityInfo.name);
    }
}
