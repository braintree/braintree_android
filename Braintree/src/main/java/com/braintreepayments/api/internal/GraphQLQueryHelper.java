package com.braintreepayments.api.internal;

import android.content.Context;
import android.content.res.Resources;

import java.io.IOException;
import java.io.InputStream;

public class GraphQLQueryHelper {

    public static final String QUERY_KEY = "query";
    public static final String INPUT_KEY = "input";
    public static final String VARIABLES_KEY = "variables";

    public static String getQuery(Context context, int queryResource) throws Resources.NotFoundException, IOException {
        InputStream inputStream = null;
        try {
            inputStream = context.getResources().openRawResource(queryResource);
            return StreamHelper.getString(inputStream);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }
}
