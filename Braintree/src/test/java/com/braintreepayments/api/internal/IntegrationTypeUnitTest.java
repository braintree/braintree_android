package com.braintreepayments.api.internal;

import android.app.Activity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;

import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricGradleTestRunner.class)
public class IntegrationTypeUnitTest {

    @Test
    public void get_returnsCustomByDefault() {
        assertEquals("custom", IntegrationType.get(new Activity()));
    }
}
