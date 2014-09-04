package com.braintreepayments.demo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.SignatureVerification;

public class Venmo extends BaseActivity {

    private static final int VENMO_REQUEST_CODE = 100;

    protected void onCreate(Bundle onSaveInstanceState) {
        super.onCreate(onSaveInstanceState);
        setContentView(R.layout.venmo);
        SignatureVerification.disableAppSwitchSignatureVerification();
    }

    @Override
    public void ready(String clientToken) {
        mBraintree = Braintree.getInstance(this, clientToken);
        mBraintree.addListener(this);

        findViewById(R.id.venmo_button).setVisibility(View.VISIBLE);
    }

    public void startVenmo(View v) {
        if (mBraintree.isVenmoEnabled()) {
            mBraintree.startPayWithVenmo(this, VENMO_REQUEST_CODE);
        } else {
            new AlertDialog.Builder(this)
                    .setMessage("Please install the Venmo app first")
                    .setPositiveButton("Ok", new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store/apps/details?id=com.venmo")));
                        }
                    })
                    .setNegativeButton("Cancel", new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VENMO_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                mBraintree.finishPayWithVenmo(resultCode, data);
                return;
            }
        }

        showDialog("Request code was " + requestCode + ", we were looking for " + VENMO_REQUEST_CODE +
                " Result code was " + resultCode + ", we were looking for " + RESULT_OK);
    }

}
