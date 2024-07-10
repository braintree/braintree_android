package com.braintreepayments.api

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteTable
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec

// Ref: https://developer.android.com/training/data-storage/room/migrating-db-versions
@Database(
    version = 7,
    entities = [AnalyticsEventBlob::class],
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7, spec = AnalyticsDatabase.DeleteAnalyticsEventTableAutoMigration::class)
    ]
)
internal abstract class AnalyticsDatabase : RoomDatabase() {

    abstract fun analyticsEventBlobDao(): AnalyticsEventBlobDao

    @DeleteTable(tableName = "analytics_event")
    class DeleteAnalyticsEventTableAutoMigration : AutoMigrationSpec

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
