package com.braintreepayments.demo;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

public class DemoApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        LeakCanary.install(this);
    }
}
