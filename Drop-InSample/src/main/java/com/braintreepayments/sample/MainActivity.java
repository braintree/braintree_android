package com.braintreepayments.sample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    public void customOnClick(View v) {
        startActivity(new Intent(this, Custom.class));
    }

    public void paypalOnClick(View v) {
        startActivity(new Intent(this, PayPal.class));
    }

    public void dropinOnClick(View v) {
        startActivity(new Intent(this, DropIn.class));
    }
}
