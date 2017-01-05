package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.visa.checkout.VisaMcomLibrary;
import com.visa.checkout.VisaPaymentInfo;
import com.visa.checkout.utils.VisaEnvironmentConfig;

/**
 * Helper Activity that captures the response when Visa Checkout completes.
 */
public class VisaCheckoutResultActivity extends Activity {

    static VisaPaymentInfo sVisaPaymentInfo;
    static VisaEnvironmentConfig sVisaEnvironmentConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VisaMcomLibrary visaMcomLibrary = VisaMcomLibrary.getLibrary(this, sVisaEnvironmentConfig);
        visaMcomLibrary.checkoutWithPayment(sVisaPaymentInfo, BraintreeRequestCodes.VISA_CHECKOUT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        setResult(resultCode, data);
        finish();
    }
}
