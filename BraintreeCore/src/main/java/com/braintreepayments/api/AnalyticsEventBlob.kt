package com.braintreepayments.api

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// Ref: https://www.reddit.com/r/javascript/comments/10st04/what_is_a_json_blob/
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
