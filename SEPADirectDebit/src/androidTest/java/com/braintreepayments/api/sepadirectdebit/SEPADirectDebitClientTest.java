package com.braintreepayments.api.sepadirectdebit;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

// TODO: replace with integration tests
@RunWith(AndroidJUnit4.class)
public class SEPADirectDebitClientTest {

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.braintreepayments.api.sepadirectdebit.test", appContext.getPackageName());
    }
}