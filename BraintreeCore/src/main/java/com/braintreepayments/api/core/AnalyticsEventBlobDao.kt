package com.braintreepayments.api.core

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
internal interface AnalyticsEventBlobDao {

    @Insert
    fun insertEventBlob(eventBlob: AnalyticsEventBlob)

    @Query("SELECT * FROM analytics_event_blob")
    fun getAllEventBlobs(): List<AnalyticsEventBlob>

    @Delete
    fun deleteEventBlobs(blobs: List<AnalyticsEventBlob>)
}
