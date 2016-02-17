package com.braintreepayments.api.test;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;

public class FragmentTestActivity extends Activity {

    @SuppressWarnings("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout view = new LinearLayout(this);
        view.setId(1);

        setContentView(view);
    }
}
