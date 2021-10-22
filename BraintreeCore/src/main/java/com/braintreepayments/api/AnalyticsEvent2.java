package com.braintreepayments.api;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "analytics_event")
public class AnalyticsEvent2 {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    public int id;

    @NonNull
    @ColumnInfo(name = "event")
    private final String event;

    @ColumnInfo(name = "timestamp")
    private final long timestamp;

    AnalyticsEvent2(@NonNull String event, long timestamp) {
        this.event = event;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    @NonNull
    public String getEvent() {
        return event;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
