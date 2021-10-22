package com.braintreepayments.api;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {AnalyticsEvent2.class}, version = 1)
abstract class AnalyticsDatabase2 extends RoomDatabase {

    abstract AnalyticsEventDao analyticsEventDao();

    private static volatile AnalyticsDatabase2 INSTANCE;

    static AnalyticsDatabase2 getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AnalyticsDatabase2.class) {
                if (INSTANCE == null) {
                    String dbName = "analytics_database";
                    Context appContext = context.getApplicationContext();
                    INSTANCE =
                        Room.databaseBuilder(appContext, AnalyticsDatabase2.class, dbName).build();
                }
            }
        }
        return INSTANCE;
    }
}
