package com.braintreepayments.api;

import android.app.Activity;

import com.braintreepayments.api.IntegrationType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class IntegrationTypeUnitTest {

    @Test
    public void get_returnsCustomByDefault() {
        assertEquals("custom", IntegrationType.get(new Activity()));
    }
}
