package com.braintreepayments.demo;

import android.app.Application;

import com.facebook.stetho.Stetho;

public class DeveloperTools {

    public static void setup(Application application) {
        Stetho.initializeWithDefaults(application);
    }
}
