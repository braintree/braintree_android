package com.braintreepayments.api

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
internal interface AnalyticsEventBlobDao {

    @Insert
    fun insertBlob(blob: AnalyticsEventBlob)

    @Query("SELECT * FROM analytics_event_blob")
    fun getAllBlobs(): List<AnalyticsEventBlob>

    @Delete
    fun deleteBlobs(blobs: List<AnalyticsEventBlob>)
}
