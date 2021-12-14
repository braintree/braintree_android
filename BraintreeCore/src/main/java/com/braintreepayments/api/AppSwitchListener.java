package com.braintreepayments.api;

import androidx.activity.result.ActivityResult;

public interface AppSwitchListener {

    void onActivityResult(ActivityResult activityResult);
    boolean onBrowserSwitchResult(BrowserSwitchResult browserSwitchResult);
}
