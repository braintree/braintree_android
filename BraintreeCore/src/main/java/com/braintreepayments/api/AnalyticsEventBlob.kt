package com.braintreepayments.api

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Store Analytics as a JSON string. The schema of the Analytics data is enforced JSON
 * at the JSON level. JSON encoded events can be sent directly to the analytics server.
 */
@Entity(tableName = "analytics_event_blob")
data class AnalyticsEventBlob(
    @ColumnInfo(name = "json_string") val jsonString: String,
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") val id: Long = 0L
)
