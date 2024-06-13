package com.braintreepayments.api

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.serialization.json.Json.Default.decodeFromString
import kotlinx.serialization.json.Json.Default.encodeToString
import kotlinx.serialization.serializer

// Ref: https://developer.android.com/training/data-storage/room/migrating-db-versions
@Database(
        version = 6,
        entities = [AnalyticsEvent::class],
        autoMigrations = [
            AutoMigration(from = 1, to = 2),
            AutoMigration(from = 2, to = 3),
            AutoMigration(from = 3, to = 4),
            AutoMigration(from = 4, to = 5)
        ]
)
@TypeConverters(MyTypeConverters::class)
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

class MyTypeConverters {
    @TypeConverter
    fun analyticsEventToString(event: AnalyticsEvent): String = encodeToString(serializer(), event)

    @TypeConverter
    fun stringToEvent(string: String): AnalyticsEvent = decodeFromString(string)
}
