package com.braintreepayments.api.test;

import android.os.Bundle;

import com.braintreepayments.api.R;
import com.braintreepayments.api.interfaces.BraintreeCancelListener;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.interfaces.PaymentMethodNoncesUpdatedListener;
import com.braintreepayments.api.interfaces.UnionPayListener;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.UnionPayCapabilities;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity that implements all listeners used by {@link com.braintreepayments.api.BraintreeFragment}
 * for testing.
 */
public class UnitTestListenerActivity extends AppCompatActivity implements PaymentMethodNonceCreatedListener,
        PaymentMethodNoncesUpdatedListener, BraintreeErrorListener, ConfigurationListener, BraintreeCancelListener,
        UnionPayListener {

    public final List<Configuration> configurations = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_AppCompat);
    }

    @Override
    public void onError(Exception error) {}

    @Override
    public void onConfigurationFetched(Configuration configuration) {
        configurations.add(configuration);
    }

    @Override
    public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {}

    @Override
    public void onPaymentMethodNoncesUpdated(List<PaymentMethodNonce> paymentMethodNonces) {}

    @Override
    public void onCancel(int requestCode) {}

    @Override
    public void onCapabilitiesFetched(UnionPayCapabilities capabilities) {}

    @Override
    public void onSmsCodeSent(String enrollmentId, boolean smsCodeRequired) {}
}
