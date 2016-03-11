package com.braintreepayments.demo;

import android.app.Application;

import com.braintreepayments.demo.internal.LeakLoggerService;
import com.facebook.stetho.Stetho;

public class DeveloperTools {

    public static void setup(Application application) {
        LeakLoggerService.setupLeakCanary(application);
        Stetho.initializeWithDefaults(application);
    }
}
