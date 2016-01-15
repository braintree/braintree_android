package com.braintreepayments.api.internal;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import com.braintreepayments.api.test.ManifestTestActivity;
import com.braintreepayments.api.test.MissingManifestTestActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@SmallTest
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
}
