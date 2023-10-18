package com.braintreepayments.api

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
internal interface AnalyticsEventDao {

    @Insert
    fun insertEvent(event: AnalyticsEvent)

    @Query("SELECT * FROM analytics_event")
    fun getAllEvents(): List<AnalyticsEvent>

    @Delete
    fun deleteEvents(events: List<AnalyticsEvent>)
}
