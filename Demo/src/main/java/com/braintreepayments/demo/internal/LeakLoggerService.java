package com.braintreepayments.demo.internal;

import com.squareup.leakcanary.AnalysisResult;
import com.squareup.leakcanary.DisplayLeakService;
import com.squareup.leakcanary.HeapDump;

import org.slf4j.LoggerFactory;

public class LeakLoggerService extends DisplayLeakService {

    @Override
    protected void afterDefaultHandling(HeapDump heapDump, AnalysisResult result, String leakInfo) {
        if (!result.leakFound || result.excludedLeak) {
            return;
        }

        LoggerFactory.getLogger("LeakCanary").warn(leakInfo);
    }
}
