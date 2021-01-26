package com.braintreepayments.api;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.braintreepayments.api.ClassHelper;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4ClassRunner.class)
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
