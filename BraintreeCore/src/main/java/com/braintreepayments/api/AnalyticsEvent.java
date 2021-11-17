package com.braintreepayments.api;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "analytics_event")
public class AnalyticsEvent {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    public int id;

    @NonNull
    @ColumnInfo(name = "name")
    private final String name;

    @ColumnInfo(name = "timestamp")
    private final long timestamp;

    AnalyticsEvent(@NonNull String name, long timestamp) {
        this.name = name;
        this.timestamp = timestamp;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
