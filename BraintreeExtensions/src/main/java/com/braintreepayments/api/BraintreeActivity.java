package com.braintreepayments.api;

import android.content.Intent;
import android.net.Uri;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.PreferredPaymentMethodsCallback;
import com.braintreepayments.api.interfaces.ThreeDSecureLookupCallback;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.GooglePaymentRequest;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.ReadyForGooglePaymentRequest;
import com.braintreepayments.api.models.ThreeDSecureLookup;
import com.braintreepayments.api.models.ThreeDSecureRequest;
import com.braintreepayments.api.models.VenmoAccountNonce;
import com.visa.checkout.VisaPaymentSummary;

import org.json.JSONException;

// TODO: unit test when API is finalized
public abstract class BraintreeActivity extends AppCompatActivity implements BrowserSwitchCallback {

    private static final String EXTRA_WAS_BROWSER_SWITCH_RESULT = "com.braintreepayments.api.WAS_BROWSER_SWITCH_RESULT";

    private BraintreeFullClient braintreeFullClient;

    @Override
    protected void onResume() {
        super.onResume();
        if (braintreeFullClient != null) {
            braintreeFullClient.deliverBrowserSwitchResult(this);
        }
    }

    @CallSuper
    protected void initializeBraintree(String authorization, String returnUrlScheme) {
        try {
            braintreeFullClient = new BraintreeFullClient(authorization, this, returnUrlScheme);
            onBraintreeInitialized();
        } catch (InvalidArgumentException e) {
            onBraintreeError(e);
        }
    }

    protected void getConfiguration(ConfigurationCallback callback) {
        braintreeFullClient.getConfiguration(this, callback);
    }

    protected void collectDeviceData(BraintreeDataCollectorCallback callback) {
        braintreeFullClient.collectDeviceData(this, callback);
    }

    protected void performThreeDSecureVerification(ThreeDSecureRequest request, ThreeDSecureLookupCallback callback) {
        braintreeFullClient.performThreeDSecureVerification(this, request, callback);
    }

    protected void continuePerformVerification(ThreeDSecureRequest request, ThreeDSecureLookup lookup, ThreeDSecureVerificationCallback callback) {
        braintreeFullClient.continuePerformVerification(this, request, lookup, callback);
    }

    protected void fetchPreferredPaymentMethods(PreferredPaymentMethodsCallback callback) {
        braintreeFullClient.fetchPreferredPaymentMethods(this, callback);
    }

    protected void createVisaCheckoutProfile(VisaCheckoutCreateProfileBuilderCallback callback) {
        braintreeFullClient.createVisaCheckoutProfile(this, callback);
    }

    protected void tokenizeVisaCheckout(VisaPaymentSummary visaPaymentSummary, VisaCheckoutTokenizeCallback callback) {
        braintreeFullClient.tokenizeVisaCheckout(this, visaPaymentSummary, callback);
    }

    protected void googlePayIsReadyToPay(ReadyForGooglePaymentRequest request, GooglePaymentIsReadyToPayCallback callback) {
        braintreeFullClient.googlePayIsReadyToPay(this, request, callback);
    }

    protected void googleRequestPayment(GooglePaymentRequest request, GooglePaymentRequestPaymentCallback callback) {
        braintreeFullClient.googleRequestPayment(this, request, callback);
    }

    @Override
    public void onResult(int requestCode, BrowserSwitchResult browserSwitchResult, @Nullable Uri uri) {
        switch (requestCode) {
            case BraintreeRequestCodes.THREE_D_SECURE:
                braintreeFullClient.onThreeDSecureBrowserSwitchResult(this, browserSwitchResult, uri, new ThreeDSecureResultCallback() {
                    @Override
                    public void onResult(@Nullable PaymentMethodNonce paymentMethodNonce, @Nullable Exception error) {
                        onThreeDSecureResult(paymentMethodNonce, error);
                    }
                });
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case BraintreeRequestCodes.THREE_D_SECURE:
                braintreeFullClient.onThreeDSecureActivityResult(this, resultCode, data, new ThreeDSecureResultCallback() {
                    @Override
                    public void onResult(@Nullable PaymentMethodNonce paymentMethodNonce, @Nullable Exception error) {
                        onThreeDSecureResult(paymentMethodNonce, error);
                    }
                });
                break;
            case BraintreeRequestCodes.VISA_CHECKOUT:
                braintreeFullClient.onVisaCheckoutActivityResult(this, resultCode, data, new VisaCheckoutOnActivityResultCallback() {
                    @Override
                    public void onResult(PaymentMethodNonce paymentMethodNonce, Exception e) {
                        onVisaCheckoutResult(paymentMethodNonce, e);
                    }
                });
                break;
            case BraintreeRequestCodes.GOOGLE_PAYMENT:
                braintreeFullClient.onGooglePayActivityResult(this, resultCode, data, new GooglePaymentOnActivityResultCallback() {
                    @Override
                    public void onResult(PaymentMethodNonce paymentMethodNonce, Exception e) {
                        onGooglePayResult(paymentMethodNonce, e);
                    }
                });
                break;
        }
    }

    // optional methods
    protected void onBraintreeInitialized() {
    }

    protected void onBraintreeError(Exception error) {
    }

    protected void onThreeDSecureResult(PaymentMethodNonce paymentMethodNonce, Exception error) {
    }

    protected void onGooglePayResult(PaymentMethodNonce paymentMethodNonce, Exception error) {
    }

    protected void onVisaCheckoutResult(PaymentMethodNonce paymentMethodNonce, Exception error) {
    }
}
