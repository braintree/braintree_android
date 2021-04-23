package com.braintreepayments.api;

import android.content.res.Resources;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(AndroidJUnit4ClassRunner.class)
public class GraphQLQueryHelperTest {

    @Test(expected = Resources.NotFoundException.class)
    public void getQuery_throwsResourcesNotFoundExceptionForInvalidResources() throws IOException {
        GraphQLQueryHelper.getQuery(ApplicationProvider.getApplicationContext(), -1);
    }
}
