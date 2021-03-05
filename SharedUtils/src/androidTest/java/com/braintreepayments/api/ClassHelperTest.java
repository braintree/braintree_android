package com.braintreepayments.api;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4ClassRunner.class)
public class ClassHelperTest {

    @Test
    public void isClassAvailable_returnsTrueWhenClassOnClasspath() {
        ClassHelper sut = new ClassHelper();
        assertTrue(sut.isClassAvailable("java.lang.String"));
    }

    @Test
    public void isClassAvailable_returnsFalseWhenClassNotOnClasspath() {
        ClassHelper sut = new ClassHelper();
        assertFalse(sut.isClassAvailable("java.lang.NotAClass"));
    }
}
