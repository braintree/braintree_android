package com.braintreepayments.api;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.visacheckout.R;
import com.visa.checkout.CheckoutButton;
import com.visa.checkout.Profile;
import com.visa.checkout.PurchaseInfo;
import com.visa.checkout.VisaCheckoutSdk;

// TODO: use this class to wrap visa internal types (will be integrated in separate PR)
public class VisaCheckoutButton extends FrameLayout {

    public VisaCheckoutButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        prepareView();
    }

    public VisaCheckoutButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        prepareView();
    }

    public VisaCheckoutButton(Context context) {
        super(context);
        prepareView();
    }

    private void prepareView() {
        inflate(getContext(), R.layout.visa_checkout_button_wrapper, this);
    }

    public void init(FragmentActivity activity, Profile.ProfileBuilder profileBuilder, PurchaseInfo.PurchaseInfoBuilder purchaseInfo, VisaCheckoutSdk.VisaCheckoutResultListener visaCheckoutResultListener) {
        CheckoutButton checkoutButton = findViewById(R.id.internal_visa_checkout_button);

        checkoutButton.init(activity, profileBuilder.build(), purchaseInfo.build(), visaCheckoutResultListener);
    }
}