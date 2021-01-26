package com.braintreepayments.api;

import android.content.Context;
import android.content.res.Resources;

import java.io.IOException;
import java.io.InputStream;

public class GraphQLQueryHelper {

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
