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
    @ColumnInfo(name = "json_string") val jsonString: String
) {
    // NOTE: default 0 value for _id is replaced when key is auto generated
    @JvmField
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0L
}
