package com.braintreepayments.api;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "analytics_event")
public class AnalyticsEvent2 {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    private int id;

    @NonNull
    @ColumnInfo(name = "event")
    private final String event;

    @NonNull
    @ColumnInfo(name = "meta_json")
    private final String metadataJSON;

    @ColumnInfo(name = "timestamp")
    private final long timestamp;

    AnalyticsEvent2(@NonNull String event, @NonNull String metadataJSON, long timestamp) {
        this.event = event;
        this.metadataJSON = metadataJSON;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    @NonNull
    public String getEvent() {
        return event;
    }

    @NonNull
    public String getMetadataJSON() {
        return metadataJSON;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
