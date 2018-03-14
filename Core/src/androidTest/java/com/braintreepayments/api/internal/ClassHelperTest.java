package com.braintreepayments.api.internal;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ClassHelperTest {

    @Test
    public void isClassAvailable_returnsTrueWhenClassOnClasspath() {
        assertTrue(ClassHelper.isClassAvailable("java.lang.String"));
    }

    @Test
    public void isClassAvailable_returnsFalseWhenClassNotOnClasspath() {
        assertFalse(ClassHelper.isClassAvailable("java.lang.NotAClass"));
    }
}
