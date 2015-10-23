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
import com.braintreepayments.api.models.AndroidPayCard;
import com.braintreepayments.api.models.Card;
import com.braintreepayments.api.models.PaymentMethod;

/**
 * Layout that includes an image and text representation of a PaymentMethod
 */
public class PaymentMethodView extends RelativeLayout {

    private PaymentMethodType mType = PaymentMethodType.UNKNOWN;
    private ImageView mIconView;
    private TextView mDescriptorView;
    private TextView mDescriptionView;
    private CharSequence mDescription;

    public PaymentMethodView(Context context) {
        super(context);
        init(context);
    }

    public PaymentMethodView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PaymentMethodView(Context context, AttributeSet attrs, int defStyle) {
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

    public void setPaymentMethodDetails(PaymentMethod paymentMethod) {
        mType = PaymentMethodType.forType(paymentMethod.getTypeLabel());
        if (paymentMethod instanceof Card) {
            mDescription = String.format(getResources().getString(R.string.bt_card_descriptor),
                    ((Card) paymentMethod).getLastTwo());
        } else if (paymentMethod instanceof AndroidPayCard) {
            mDescription = String.format(getResources().getString(R.string.bt_card_descriptor),
                    ((AndroidPayCard) paymentMethod).getLastTwo());
        } else {
            mDescription = paymentMethod.getDescription();
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
