package com.braintreepayments.demo;

import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.braintreepayments.api.BraintreeRequestCodes;
import com.braintreepayments.api.BrowserSwitchResult;

public class DemoViewModel extends ViewModel {

    private final MutableLiveData<BrowserSwitchResult> threeDSecureBrowserSwitchResult = new MutableLiveData<>();
    private final MutableLiveData<BrowserSwitchResult> localPaymentBrowserSwitchResult = new MutableLiveData<>();
    private final MutableLiveData<BrowserSwitchResult> payPalBrowserSwitchResult = new MutableLiveData<>();
    private final MutableLiveData<ActivityResult> threeDSecureActivityResult = new MutableLiveData<>();
    private final MutableLiveData<ActivityResult> googlePayActivityResult = new MutableLiveData<>();
    private final MutableLiveData<ActivityResult> venmoActivityResult = new MutableLiveData<>();

    public void onBrowserSwitchResult(BrowserSwitchResult browserSwitchResult) {
        if (browserSwitchResult == null) {
            return;
        }
        switch (browserSwitchResult.getRequestCode()) {
            case BraintreeRequestCodes.LOCAL_PAYMENT:
                localPaymentBrowserSwitchResult.setValue(browserSwitchResult);
                break;
            case BraintreeRequestCodes.PAYPAL:
                payPalBrowserSwitchResult.setValue(browserSwitchResult);
                break;
            case BraintreeRequestCodes.THREE_D_SECURE:
                threeDSecureBrowserSwitchResult.setValue(browserSwitchResult);
                break;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case BraintreeRequestCodes.THREE_D_SECURE:
                threeDSecureActivityResult.setValue(new ActivityResult(requestCode, resultCode, data));
                break;
            case BraintreeRequestCodes.GOOGLE_PAY:
                googlePayActivityResult.setValue(new ActivityResult(requestCode, resultCode, data));
                break;
            case BraintreeRequestCodes.VENMO:
                venmoActivityResult.setValue(new ActivityResult(requestCode, resultCode, data));
                break;
        }
    }

    public MutableLiveData<ActivityResult> getThreeDSecureActivityResult() {
        return threeDSecureActivityResult;
    }

    public MutableLiveData<ActivityResult> getGooglePayActivityResult() {
        return googlePayActivityResult;
    }

    public MutableLiveData<ActivityResult> getVenmoActivityResult() {
        return venmoActivityResult;
    }

    public LiveData<BrowserSwitchResult> getThreeDSecureBrowserSwitchResult() {
        return threeDSecureBrowserSwitchResult;
    }

    public LiveData<BrowserSwitchResult> getLocalPaymentBrowserSwitchResult() {
        return localPaymentBrowserSwitchResult;
    }

    public LiveData<BrowserSwitchResult> getPayPalBrowserSwitchResult() {
        return payPalBrowserSwitchResult;
    }
}
