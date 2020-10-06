package com.braintreepayments.api.test;

import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.R;

public class FragmentTestActivity extends FragmentActivity {

    @SuppressWarnings("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_AppCompat);
        LinearLayout view = new LinearLayout(this);
        view.setId(1);

        setContentView(view);
    }
}
