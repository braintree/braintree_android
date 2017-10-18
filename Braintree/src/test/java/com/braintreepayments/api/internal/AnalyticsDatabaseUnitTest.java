package com.braintreepayments.api.internal;

import android.database.Cursor;

import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.braintreepayments.api.internal.AnalyticsDatabaseTestUtils.awaitThreadPoolFinished;
import static com.braintreepayments.api.internal.AnalyticsDatabaseTestUtils.clearAllEvents;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class AnalyticsDatabaseUnitTest {

    private AnalyticsDatabase mAnalyticsDatabase;

    @Before
    public void setup() {
        mAnalyticsDatabase = AnalyticsDatabase.getInstance(RuntimeEnvironment.application);
        clearAllEvents(RuntimeEnvironment.application);
    }

    @After
    public void teardown() {
        clearAllEvents(RuntimeEnvironment.application);
    }

    @Test
    public void addEvent_persistsEvent() throws Exception {
        AnalyticsEvent request = new AnalyticsEvent(RuntimeEnvironment.application, "sessionId",
                "custom", "started.client-token");

        mAnalyticsDatabase.addEvent(request);

        awaitThreadPoolFinished(mAnalyticsDatabase);

        Cursor cursor = mAnalyticsDatabase.getReadableDatabase().query(false, "analytics", null, null, null,
                null, null, "_id desc", "1");

        assertTrue(cursor.moveToFirst());
        assertEquals(request.event, cursor.getString(cursor.getColumnIndex(AnalyticsDatabase.EVENT)));
        assertEquals(request.timestamp, cursor.getLong(cursor.getColumnIndex(AnalyticsDatabase.TIMESTAMP)));
        assertEquals(request.metadata.toString(), cursor.getString(cursor.getColumnIndex(AnalyticsDatabase.META_JSON)));
    }

    @Test
    public void removeEvents_removesEventsFromDb() throws InterruptedException {
        AnalyticsEvent event1 = new AnalyticsEvent(RuntimeEnvironment.application, "sessionId",
                "custom", "started.client-token");
        AnalyticsEvent event2 = new AnalyticsEvent(RuntimeEnvironment.application, "sessionId",
                "custom", "finished.client-token");

        mAnalyticsDatabase.addEvent(event1);
        mAnalyticsDatabase.addEvent(event2);

        awaitThreadPoolFinished(mAnalyticsDatabase);
        mAnalyticsDatabase = AnalyticsDatabase.getInstance(RuntimeEnvironment.application);

        Cursor idCursor = mAnalyticsDatabase.getReadableDatabase().query(false, "analytics", new String[]{"_id"},
                null, null, null, null, "_id asc", null);

        List<AnalyticsEvent> fetchedEvents = new ArrayList<>();
        while (idCursor.moveToNext()) {
            AnalyticsEvent event = new AnalyticsEvent();
            event.id = idCursor.getInt(0);
            fetchedEvents.add(event);
        }

        assertEquals(2, fetchedEvents.size());

        mAnalyticsDatabase.removeEvents(fetchedEvents);

        awaitThreadPoolFinished(mAnalyticsDatabase);

        idCursor = mAnalyticsDatabase.getReadableDatabase().query(false, "analytics", new String[]{"_id"},
                null, null, null, null, "_id asc", null);

        assertEquals(idCursor.getCount(), 0);
    }

    @Test
    public void getPendingRequests_returnsCorrectGroupingsOfMetadata() throws Exception {
        AnalyticsEvent request1 = new AnalyticsEvent(RuntimeEnvironment.application, "sessionId",
                "custom", "started.client-token");
        AnalyticsEvent request2 = new AnalyticsEvent(RuntimeEnvironment.application, "sessionId",
                "custom", "finished.client-token");

        AnalyticsEvent request3 = new AnalyticsEvent(RuntimeEnvironment.application, "anotherSessionId",
                "custom", "started.client-token");
        AnalyticsEvent request4 = new AnalyticsEvent(RuntimeEnvironment.application, "anotherSessionId",
                "custom", "finished.client-token");

        mAnalyticsDatabase.addEvent(request1);
        mAnalyticsDatabase.addEvent(request2);

        awaitThreadPoolFinished(mAnalyticsDatabase);
        mAnalyticsDatabase = AnalyticsDatabase.getInstance(RuntimeEnvironment.application);

        mAnalyticsDatabase.addEvent(request3);
        mAnalyticsDatabase.addEvent(request4);

        awaitThreadPoolFinished(mAnalyticsDatabase);
        mAnalyticsDatabase = AnalyticsDatabase.getInstance(RuntimeEnvironment.application);

        List<List<AnalyticsEvent>> analyticsRequests = mAnalyticsDatabase.getPendingRequests();

        assertEquals(2, analyticsRequests.size());

        assertEquals(1, analyticsRequests.get(0).get(0).id);
        assertEquals(request1.event, analyticsRequests.get(0).get(0).event);
        assertEquals(request1.metadata.getString("sessionId"),
                analyticsRequests.get(0).get(0).metadata.getString("sessionId"));

        assertEquals(2, analyticsRequests.get(0).get(1).id);
        assertEquals(request2.event, analyticsRequests.get(0).get(1).event);
        assertEquals(request2.metadata.getString("sessionId"),
                analyticsRequests.get(0).get(1).metadata.getString("sessionId"));

        assertEquals(3, analyticsRequests.get(1).get(0).id);
        assertEquals(request3.event, analyticsRequests.get(1).get(0).event);
        assertEquals(request3.metadata.getString("sessionId"),
                analyticsRequests.get(1).get(0).metadata.getString("sessionId"));

        assertEquals(4, analyticsRequests.get(1).get(1).id);
        assertEquals(request4.event, analyticsRequests.get(1).get(1).event);
        assertEquals(request4.metadata.getString("sessionId"),
                analyticsRequests.get(1).get(1).metadata.getString("sessionId"));
    }
}
