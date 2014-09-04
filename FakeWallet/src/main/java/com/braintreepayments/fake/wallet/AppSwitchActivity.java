package com.braintreepayments.fake.wallet;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class AppSwitchActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_switch);
    }

    public void finishAppSwitch(View v) {
        finish();
    }

}
