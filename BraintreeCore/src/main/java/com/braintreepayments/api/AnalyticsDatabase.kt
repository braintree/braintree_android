package com.braintreepayments.api

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AnalyticsEvent::class], version = 1)
internal abstract class AnalyticsDatabase : RoomDatabase() {

    abstract fun analyticsEventDao(): AnalyticsEventDao

    companion object {

        @Volatile
        private var INSTANCE: AnalyticsDatabase? = null

        // Ref: https://developer.android.com/codelabs/android-room-with-a-view-kotlin#7
        @JvmStatic
        fun getInstance(context: Context): AnalyticsDatabase =
            INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AnalyticsDatabase::class.java,
                    "analytics_database"
                ).build()
                INSTANCE = instance
                // return instance
                instance
            }
    }
}
