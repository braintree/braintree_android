package com.braintreepayments.api.internal;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;

import com.braintreepayments.api.interfaces.BraintreeResponseListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AnalyticsDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "braintree-analytics.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "analytics";

    static final String ID = "_id";
    static final String EVENT = "event";
    static final String TIMESTAMP = "timestamp";
    static final String META_JSON = "meta_json";

    protected final Set<AsyncTask> mTaskSet = new HashSet<>();

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
        db.execSQL("create table " + TABLE_NAME + "(" +
                ID + " integer primary key autoincrement, " +
                EVENT + " text not null, " +
                TIMESTAMP + " long not null, " +
                META_JSON + " text not null);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists " + TABLE_NAME);
        onCreate(db);
    }

    public void addEvent(AnalyticsEvent request) {
        final ContentValues values = new ContentValues();
        values.put(EVENT, request.event);
        values.put(TIMESTAMP, request.timestamp);
        values.put(META_JSON, request.metadata.toString());

        DatabaseTask task = new DatabaseTask(new Runnable() {
            @Override
            public void run() {
                SQLiteDatabase db = null;
                try {
                    db = getWritableDatabase();
                    db.insert(TABLE_NAME, null, values);
                } finally {
                    if (db != null) {
                        db.close();
                    }
                }
            }
        });

        queueTask(task);
    }

    public void removeEvents(List<AnalyticsEvent> events) {
        final StringBuilder where = new StringBuilder(ID).append(" in (");
        final String[] whereArgs = new String[events.size()];

        for (int i = 0; i < events.size(); i++) {
            whereArgs[i] = Integer.toString(events.get(i).id);

            where.append("?");
            if (i < events.size() - 1) {
                where.append(",");
            } else {
                where.append(")");
            }
        }

        DatabaseTask task = new DatabaseTask(new Runnable() {
            @Override
            public void run() {
                SQLiteDatabase db = null;
                try {
                    db = getWritableDatabase();
                    db.delete(TABLE_NAME, where.toString(), whereArgs);
                } finally {
                    if (db != null) {
                        db.close();
                    }
                }
            }
        });

        queueTask(task);
    }

    public List<List<AnalyticsEvent>> getPendingRequests() {
        SQLiteDatabase db = getReadableDatabase();

        String[] columns = {"group_concat(" + ID + ")", "group_concat(" + EVENT + ")", "group_concat(" + TIMESTAMP + ")",
                META_JSON};
        Cursor cursor = db.query(false, TABLE_NAME, columns, null, null, META_JSON, null, "_id asc", null);

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
                try {
                    request = new AnalyticsEvent();
                    request.id = Integer.valueOf(ids[i]);
                    request.event = events[i];
                    request.timestamp = Long.valueOf(timestamps[i]);
                    request.metadata = new JSONObject(cursor.getString(cursor.getColumnIndex(META_JSON)));
                    innerList.add(request);
                } catch (JSONException ignored) {}
            }

            analyticsRequests.add(innerList);
        }

        cursor.close();
        db.close();

        return analyticsRequests;
    }

    private void queueTask(final DatabaseTask task) {
        task.setFinishedCallback(new BraintreeResponseListener<Void>() {
            @Override
            public void onResponse(Void aVoid) {
                synchronized (mTaskSet) {
                    mTaskSet.remove(task);
                }
            }
        });

        synchronized (mTaskSet) {
            mTaskSet.add(task);
        }

        task.execute();
    }

    private static class DatabaseTask extends AsyncTask<Void, Void, Void> {

        private Runnable mDatabaseAction;
        private BraintreeResponseListener<Void> mFinishedCallback;

        public DatabaseTask(Runnable action) {
            mDatabaseAction = action;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            mDatabaseAction.run();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (mFinishedCallback != null) {
                mFinishedCallback.onResponse(null);
            }
        }

        private void setFinishedCallback(BraintreeResponseListener<Void> callback) {
            mFinishedCallback = callback;
        }
    }
}
