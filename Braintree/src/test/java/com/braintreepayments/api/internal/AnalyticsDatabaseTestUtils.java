package com.braintreepayments.api.internal;

import android.content.Context;
import android.database.Cursor;

public class AnalyticsDatabaseTestUtils {

    public static void clearAllEvents(Context context) {
        AnalyticsDatabase database = AnalyticsDatabase.getInstance(context.getApplicationContext());
        database.getWritableDatabase().delete("analytics", null, null);
        database.close();
    }

    public static boolean verifyAnalyticsEvent(Context context, String eventFragment) {
        AnalyticsDatabase database = AnalyticsDatabase.getInstance(context.getApplicationContext());
        Cursor c = database.getReadableDatabase().query("analytics", new String[]{"event"}, "event like ?",
                new String[]{eventFragment}, null, null, null);
        return c.getCount() == 1;
    }
}
