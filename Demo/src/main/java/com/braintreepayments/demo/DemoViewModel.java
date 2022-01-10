package com.braintreepayments.demo;

import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.braintreepayments.api.BraintreeRequestCodes;
import com.braintreepayments.api.BrowserSwitchResult;

public class DemoViewModel extends ViewModel {

    private final MutableLiveData<ActivityResult> googlePayActivityResult = new MutableLiveData<>();
    private final MutableLiveData<ActivityResult> venmoActivityResult = new MutableLiveData<>();

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case BraintreeRequestCodes.GOOGLE_PAY:
                googlePayActivityResult.setValue(new ActivityResult(requestCode, resultCode, data));
                break;
            case BraintreeRequestCodes.VENMO:
                venmoActivityResult.setValue(new ActivityResult(requestCode, resultCode, data));
                break;
        }
    }

    public MutableLiveData<ActivityResult> getGooglePayActivityResult() {
        return googlePayActivityResult;
    }

    public MutableLiveData<ActivityResult> getVenmoActivityResult() {
        return venmoActivityResult;
    }
}
