package com.braintreepayments.demo.internal;

import android.app.Application;
import android.util.Log;

import com.squareup.leakcanary.AnalysisResult;
import com.squareup.leakcanary.AndroidExcludedRefs;
import com.squareup.leakcanary.DisplayLeakService;
import com.squareup.leakcanary.HeapDump;
import com.squareup.leakcanary.LeakCanary;

public class LeakLoggerService extends DisplayLeakService {

    public static void setupLeakCanary(Application application) {
        LeakCanary.install(application, LeakLoggerService.class,
                AndroidExcludedRefs.createAppDefaults().build());
    }

    @Override
    protected void afterDefaultHandling(HeapDump heapDump, AnalysisResult result, String leakInfo) {
        if (!result.leakFound || result.excludedLeak) {
            return;
        }

        Log.w("LeakCanary", leakInfo);
    }
}
