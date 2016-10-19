package com.braintreepayments.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.VisaCheckout;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.VisaCheckoutListener;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.visa.checkout.VisaMcomLibrary;
import com.visa.checkout.VisaMerchantInfo;
import com.visa.checkout.VisaMerchantInfo.AcceptedBillingRegions;
import com.visa.checkout.VisaMerchantInfo.AcceptedCardBrands;
import com.visa.checkout.VisaMerchantInfo.AcceptedShippingRegions;
import com.visa.checkout.VisaPaymentInfo;
import com.visa.checkout.VisaPaymentInfo.Currency;
import com.visa.checkout.VisaPaymentInfo.UserReviewAction;
import com.visa.checkout.widget.VisaPaymentButton;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;

public class VisaCheckoutActivity extends BaseActivity implements OnClickListener, VisaCheckoutListener {

    private LinearLayout mVisaCheckoutLayout;

    private VisaMcomLibrary mVisaMComLibrary;
    private View mVisaPaymentButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.visa_checkout_activity);
        mVisaCheckoutLayout = (LinearLayout) findViewById(R.id.visa_checkout_layout);
    }

    @Override
    protected void reset() {
        if (mVisaPaymentButton != null) {
            mVisaPaymentButton.setEnabled(false);
        }
    }

    @Override
    protected void onAuthorizationFetched() {
        try {
            mBraintreeFragment = BraintreeFragment.newInstance(this, mAuthorization);
        } catch (InvalidArgumentException e) {
            e.printStackTrace();
        }
        VisaCheckout.createVisaCheckoutLibrary(mBraintreeFragment);
    }


    @Override
    public void onClick(View view) {
        VisaPaymentInfo visaPaymentInfo = new VisaPaymentInfo();
        visaPaymentInfo.setUsingShippingAddress(true);
        visaPaymentInfo.setCurrency(Currency.USD);
        visaPaymentInfo.setTotal(new BigDecimal("12.34"));
        visaPaymentInfo.setUserReviewAction(UserReviewAction.CONTINUE);
        visaPaymentInfo.setSubtotal(new BigDecimal("12.34"));
        visaPaymentInfo.setTax(new BigDecimal("12.34"));
        visaPaymentInfo.setMisc(new BigDecimal("12.34"));
        visaPaymentInfo.setDiscount(new BigDecimal("12.34"));
        visaPaymentInfo.setGiftWrap(new BigDecimal("12.34"));
        visaPaymentInfo.setDescription("Work");
        visaPaymentInfo.setOrderId("Order1234567890");
        visaPaymentInfo.setMerchantRequestId("121");

        VisaMerchantInfo visaMerchantInfo = new VisaMerchantInfo();
        visaMerchantInfo.setDataLevel(VisaMerchantInfo.MerchantDataLevel.FULL);
        visaMerchantInfo.setLogoResourceId(R.drawable.ic_launcher);
        visaMerchantInfo.setAcceptCanadianVisaDebit(false);

        visaMerchantInfo.setAcceptedCardBrands(new ArrayList<>(
                Arrays.asList(AcceptedCardBrands.values())
        ));
        visaMerchantInfo.setAcceptedShippingRegions(new ArrayList<>(
                Arrays.asList(AcceptedShippingRegions.values())
        ));
        visaMerchantInfo.setAcceptedBillingRegions(new ArrayList<>(
                Arrays.asList(AcceptedBillingRegions.values())
        ));
        visaPaymentInfo.setVisaMerchantInfo(visaMerchantInfo);

        VisaCheckout.authorize(mBraintreeFragment, mVisaMComLibrary, visaPaymentInfo);
    }

    @Override
    public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
        super.onPaymentMethodNonceCreated(paymentMethodNonce);

        Intent intent = new Intent()
                .putExtra(MainActivity.EXTRA_PAYMENT_METHOD_NONCE, paymentMethodNonce);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == VisaCheckout.VISA_CHECKOUT_REQUEST_CODE) {
            mBraintreeFragment.onActivityResult(requestCode, resultCode, data);
        } else {
            onError(new Exception("Request Code: " + requestCode + " Result Code: " + resultCode));
        }
    }

    @Override
    public void onVisaCheckoutLibraryCreated(VisaMcomLibrary visaMcomLibrary) {
        mVisaMComLibrary = visaMcomLibrary;

        // Generate the button once.
        if (mVisaCheckoutLayout.getChildCount() == 0) {
            mVisaPaymentButton = new VisaPaymentButton(this);
            mVisaPaymentButton.setOnClickListener(this);
            mVisaCheckoutLayout.addView(mVisaPaymentButton);
        }
    }
}
