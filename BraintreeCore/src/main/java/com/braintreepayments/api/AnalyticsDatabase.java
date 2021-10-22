package com.braintreepayments.api;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {AnalyticsEvent.class}, version = 1)
abstract class AnalyticsDatabase extends RoomDatabase {

    abstract AnalyticsEventDao analyticsEventDao();

    private static volatile AnalyticsDatabase INSTANCE;

    static AnalyticsDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AnalyticsDatabase.class) {
                // double check that instance was not created in another thread
                if (INSTANCE == null) {
                    String dbName = "analytics_database";
                    Context appContext = context.getApplicationContext();
                    INSTANCE =
                        Room.databaseBuilder(appContext, AnalyticsDatabase.class, dbName).build();
                }
            }
        }
        return INSTANCE;
    }
}
