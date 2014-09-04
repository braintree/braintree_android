package com.braintreepayments.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;

import com.braintreepayments.api.SignatureVerification;

public class MainActivity extends Activity {

    public static final String EXTRA_ENVIRONMENT = "com.braintreepayments.demo.environment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        SignatureVerification.disableAppSwitchSignatureVerification();
    }

    public void customOnClick(View v) {
        startActivity(getIntentForClass(Custom.class));
    }

    public void paypalOnClick(View v) {
        startActivity(getIntentForClass(PayPal.class));
    }

    public void venmoOnClick(View v) {
        if (getEnvironment() == "production") {
            startActivity(getIntentForClass(Venmo.class));
        } else {
            new AlertDialog.Builder(this)
                    .setMessage("Enable production before testing Venmo")
                    .show();
        }
    }

    public void dropinOnClick(View v) {
        startActivity(getIntentForClass(DropIn.class));
    }

    protected Intent getIntentForClass(Class klass) {
        return new Intent(this, klass)
                .putExtra(EXTRA_ENVIRONMENT, getEnvironment());
    }

    protected String getEnvironment() {
        switch (((RadioGroup) findViewById(R.id.environment_selector)).getCheckedRadioButtonId()) {
            case R.id.environment_production:
                return "production";
            case R.id.environment_sandbox:
            default:
                return "sandbox";
        }
    }
}
