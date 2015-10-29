package com.braintreepayments.api.dropin.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.braintreepayments.api.dropin.R;
import com.braintreepayments.api.dropin.utils.PaymentMethodType;
import com.braintreepayments.api.models.AndroidPayCardNonce;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.PaymentMethodNonce;

/**
 * Layout that includes an image and text representation of a PaymentMethod
 */
public class PaymentMethodNonceView extends RelativeLayout {

    private PaymentMethodType mType = PaymentMethodType.UNKNOWN;
    private ImageView mIconView;
    private TextView mDescriptorView;
    private TextView mDescriptionView;
    private CharSequence mDescription;

    public PaymentMethodNonceView(Context context) {
        super(context);
        init(context);
    }

    public PaymentMethodNonceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PaymentMethodNonceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.bt_payment_method, this);
        mIconView = (ImageView) findViewById(R.id.bt_payment_method_icon);
        mDescriptorView = (TextView) findViewById(R.id.bt_payment_method_type);
        mDescriptionView = (TextView) findViewById(R.id.bt_payment_method_description);
        refresh();
    }

    public void setPaymentMethodNonceDetails(PaymentMethodNonce paymentMethodNonce) {
        mType = PaymentMethodType.forType(paymentMethodNonce.getTypeLabel());
        if (paymentMethodNonce instanceof CardNonce) {
            mDescription = String.format(getResources().getString(R.string.bt_card_descriptor),
                    ((CardNonce) paymentMethodNonce).getLastTwo());
        } else if (paymentMethodNonce instanceof AndroidPayCardNonce) {
            mDescription = String.format(getResources().getString(R.string.bt_card_descriptor),
                    ((AndroidPayCardNonce) paymentMethodNonce).getLastTwo());
        } else {
            mDescription = paymentMethodNonce.getDescription();
        }
        refresh();
    }

    private void refresh() {
        if (mType != PaymentMethodType.UNKNOWN) {
            mIconView.setImageResource(mType.getDrawable());
            mIconView.setVisibility(View.VISIBLE);
        } else {
            mIconView.setVisibility(View.GONE);
        }
        mDescriptorView.setText(getContext().getString(mType.getLocalizedName()));
        mDescriptionView.setText(mDescription);
    }
}
