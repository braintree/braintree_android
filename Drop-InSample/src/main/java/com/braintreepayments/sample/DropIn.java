package com.braintreepayments.sample;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.braintreepayments.api.dropin.BraintreePaymentActivity;
import com.braintreepayments.api.dropin.Customization;
import com.braintreepayments.api.dropin.Customization.CustomizationBuilder;

public class DropIn extends BaseActivity {

    private static final int DROP_IN_REQUEST = 200;

    private String mClientToken;

    protected void onCreate(Bundle onSaveInstanceState) {
        super.onCreate(onSaveInstanceState);
        setContentView(R.layout.dropin);
    }

    public void launch(View v) {
        Intent intent = new Intent(this, BraintreePaymentActivity.class);
        intent.putExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN, mClientToken);

        Customization customization = new CustomizationBuilder()
                .primaryDescription("Cart")
                .secondaryDescription("3 Items")
                .amount("$35")
                .submitButtonText("Buy")
                .build();
        intent.putExtra(BraintreePaymentActivity.EXTRA_CUSTOMIZATION, customization);

        startActivityForResult(intent, DROP_IN_REQUEST);
    }

    @Override
    public void ready(String clientToken) {
        mClientToken = clientToken;
        findViewById(R.id.launch_dropin).setEnabled(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DROP_IN_REQUEST) {
            if (resultCode == RESULT_OK) {
                String paymentMethodNonce = data.getStringExtra(BraintreePaymentActivity.EXTRA_PAYMENT_METHOD_NONCE);
                postNonceToServer(paymentMethodNonce);
                return;
            }
        }

        showDialog("Request code was " + requestCode + ", we were looking for " + DROP_IN_REQUEST +
            " resultCode was " + resultCode + ", we were looking for " + RESULT_OK);
    }
}
