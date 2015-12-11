package com.braintreepayments.demo.internal;

import android.app.Application;

import com.squareup.leakcanary.AnalysisResult;
import com.squareup.leakcanary.AndroidExcludedRefs;
import com.squareup.leakcanary.DisplayLeakService;
import com.squareup.leakcanary.HeapDump;
import com.squareup.leakcanary.LeakCanary;

import org.slf4j.LoggerFactory;

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

        LoggerFactory.getLogger("LeakCanary").warn(leakInfo);
    }
}
