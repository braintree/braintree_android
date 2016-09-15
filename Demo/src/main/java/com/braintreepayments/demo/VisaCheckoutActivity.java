package com.braintreepayments.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.VisaCheckout;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.visa.checkout.VisaMerchantInfo;
import com.visa.checkout.VisaMerchantInfo.AcceptedBillingRegions;
import com.visa.checkout.VisaMerchantInfo.AcceptedCardBrands;
import com.visa.checkout.VisaMerchantInfo.AcceptedShippingRegions;
import com.visa.checkout.VisaPaymentInfo;
import com.visa.checkout.VisaPaymentInfo.Currency;
import com.visa.checkout.VisaPaymentInfo.UserReviewAction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;

public class VisaCheckoutActivity extends BaseActivity implements OnClickListener {

    private View mVisaCheckoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.visa_checkout_activity);
        mVisaCheckoutButton = findViewById(R.id.visa_checkout_button);
        mVisaCheckoutButton.setOnClickListener(this);
    }

    @Override
    protected void reset() {
        mVisaCheckoutButton.setEnabled(false);
    }

    @Override
    protected void onAuthorizationFetched() {
        try {
            mBraintreeFragment = BraintreeFragment.newInstance(this, mAuthorization);
        } catch (InvalidArgumentException e) {
            e.printStackTrace();
        }
        mVisaCheckoutButton.setEnabled(true);
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

        VisaMerchantInfo visaMerchantInfo = new VisaMerchantInfo();
        visaMerchantInfo.setDataLevel(VisaMerchantInfo.MerchantDataLevel.SUMMARY);
        visaMerchantInfo.setLogoResourceId(R.drawable.ic_launcher);
//        visaMerchantInfo.setExternalProfileId("wat");
        visaMerchantInfo.setMerchantId("121");
        visaMerchantInfo.setAcceptCanadianVisaDebit(false);

        visaMerchantInfo.setAcceptedCardBrands(new ArrayList<AcceptedCardBrands>(
                Arrays.asList(AcceptedCardBrands.values())
        ));
        visaMerchantInfo.setAcceptedShippingRegions(new ArrayList<AcceptedShippingRegions>(
                Arrays.asList(AcceptedShippingRegions.values())
        ));
        visaMerchantInfo.setAcceptedBillingRegions(new ArrayList<AcceptedBillingRegions>(
                Arrays.asList(AcceptedBillingRegions.values())
        ));
        visaPaymentInfo.setVisaMerchantInfo(visaMerchantInfo);

        VisaCheckout.authorize(mBraintreeFragment, visaPaymentInfo);
    }

    @Override
    public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
        super.onPaymentMethodNonceCreated(paymentMethodNonce);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            mBraintreeFragment.onActivityResult(requestCode, resultCode, data);
        } else {
            onError(new Exception("Request Code: " + requestCode + " Result Code: " + resultCode));
        }
    }
}
