package com.braintreepayments.api.internal;

import android.content.Context;
import android.database.Cursor;
import android.os.SystemClock;

import java.util.concurrent.TimeUnit;

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

    /**
     * Waits for the AnalyticsDatabase AsyncTask queue to empty before continuing.
     * @param database the database we are awaiting operations on
     * @throws InterruptedException
     */
    public static void awaitTasksFinished(AnalyticsDatabase database) throws InterruptedException {
        long timeoutTimestamp = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5);

        while (database.mTaskSet.size() > 0) {
            if (System.currentTimeMillis() > timeoutTimestamp) {
                throw new InterruptedException("Timeout exceeded waiting for async task queue to complete");
            }

            SystemClock.sleep(5);
        }
    }
}
