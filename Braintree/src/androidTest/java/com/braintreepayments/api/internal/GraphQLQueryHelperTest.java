package com.braintreepayments.api.internal;

import android.content.res.Resources;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static androidx.test.InstrumentationRegistry.getTargetContext;

@RunWith(AndroidJUnit4.class)
public class GraphQLQueryHelperTest {

    @Test(expected = Resources.NotFoundException.class)
    public void getQuery_throwsResourcesNotFoundExceptionForInvalidResources() throws IOException {
        GraphQLQueryHelper.getQuery(getTargetContext(), -1);
    }
}
