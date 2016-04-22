package com.braintreepayments.api.internal;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class AnalyticsDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "braintree-analytics.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "analytics";

    private static final String ID = "_id";
    private static final String EVENT = "event";
    private static final String SESSION_ID = "session_id";
    private static final String TIMESTAMP = "timestamp";
    private static final String NETWORK_TYPE = "network_type";
    private static final String INTERFACE_ORIENTATION = "interface_orientation";
    private static final String MERCHANT_APP_VERSION = "merchant_app_version";
    private static final String PAYPAL_INSTALLED = "paypal_installed";
    private static final String VENMO_INSTALLED = "venmo_installed";

    public static AnalyticsDatabase getInstance(Context context) {
        return new AnalyticsDatabase(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public AnalyticsDatabase(Context context, String name, CursorFactory factory, int version) {
        super(context, DATABASE_NAME, factory, DATABASE_VERSION);
    }

    public AnalyticsDatabase(Context context, String name, CursorFactory factory, int version,
            DatabaseErrorHandler errorHandler) {
        super(context, DATABASE_NAME, factory, DATABASE_VERSION, errorHandler);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + TABLE_NAME + "(" + ID + " integer primary key autoincrement, " +
                EVENT + " text not null, " + SESSION_ID + " text not null, " + TIMESTAMP + " long not null, " +
                NETWORK_TYPE + " text not null, " + INTERFACE_ORIENTATION + " text not null, " +
                MERCHANT_APP_VERSION + " text not null, " + PAYPAL_INSTALLED + " integer not null, " +
                VENMO_INSTALLED + " integer not null);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists " + TABLE_NAME);
        onCreate(db);
    }

    public void addEvent(AnalyticsEvent request) {
        ContentValues values = new ContentValues();
        values.put(EVENT, request.event);
        values.put(SESSION_ID, request.sessionId);
        values.put(TIMESTAMP, request.timestamp);
        values.put(NETWORK_TYPE, request.networkType);
        values.put(INTERFACE_ORIENTATION, request.interfaceOrientation);
        values.put(MERCHANT_APP_VERSION, request.merchantAppVersion);
        values.put(PAYPAL_INSTALLED, request.paypalInstalled);
        values.put(VENMO_INSTALLED, request.venmoInstalled);

        SQLiteDatabase db = getWritableDatabase();
        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    public void removeEvents(List<AnalyticsEvent> events) {
        StringBuilder where = new StringBuilder(ID).append(" in (");
        String[] whereArgs = new String[events.size()];

        for (int i = 0; i < events.size(); i++) {
            whereArgs[i] = Integer.toString(events.get(i).id);

            where.append("?");
            if (i < events.size() - 1) {
                where.append(",");
            } else {
                where.append(")");
            }
        }

        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_NAME, where.toString(), whereArgs);
        db.close();
    }

    public List<List<AnalyticsEvent>> getPendingRequests() {
        SQLiteDatabase db = getReadableDatabase();

        String[] columns = {"group_concat(" + ID + ")", "group_concat(" + EVENT + ")", "group_concat(" + TIMESTAMP + ")", SESSION_ID, NETWORK_TYPE, INTERFACE_ORIENTATION, MERCHANT_APP_VERSION,
                PAYPAL_INSTALLED, VENMO_INSTALLED};
        String groupBy = "session_id, network_type, interface_orientation, merchant_app_version, paypal_installed, venmo_installed";
        Cursor cursor = db.query(false, TABLE_NAME, columns, null, null, groupBy, null, "_id asc", null);

        List<List<AnalyticsEvent>> analyticsRequests = new ArrayList<>();

        List<AnalyticsEvent> innerList;
        String[] ids;
        String[] events;
        String[] timestamps;
        AnalyticsEvent request;
        while (cursor.moveToNext()) {
            innerList = new ArrayList<>();
            ids = cursor.getString(0).split(",");
            events = cursor.getString(1).split(",");
            timestamps = cursor.getString(2).split(",");
            for (int i = 0; i < events.length; i++) {
                request = new AnalyticsEvent();
                request.id = Integer.valueOf(ids[i]);
                request.event = events[i];
                request.timestamp = Long.valueOf(timestamps[i]);
                request.sessionId = cursor.getString(cursor.getColumnIndex(SESSION_ID));
                request.networkType = cursor.getString(cursor.getColumnIndex(NETWORK_TYPE));
                request.interfaceOrientation = cursor.getString(cursor.getColumnIndex(INTERFACE_ORIENTATION));
                request.merchantAppVersion = cursor.getString(cursor.getColumnIndex(MERCHANT_APP_VERSION));
                request.paypalInstalled = cursor.getInt(cursor.getColumnIndex(PAYPAL_INSTALLED)) == 1;
                request.venmoInstalled = cursor.getInt(cursor.getColumnIndex(VENMO_INSTALLED)) == 1;

                innerList.add(request);
            }

            analyticsRequests.add(innerList);
        }

        cursor.close();
        db.close();

        return analyticsRequests;
    }
}
