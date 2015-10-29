package com.braintreepayments.api.dropin.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.braintreepayments.api.dropin.view.PaymentMethodNonceView;
import com.braintreepayments.api.models.PaymentMethodNonce;

import java.util.List;

/**
 * {@link android.widget.BaseAdapter} to handle a {@link java.util.List} of {@link PaymentMethodNonce}s.
 * Used for listing existing {@link PaymentMethodNonce}s for a customer to choose.
 */
public class PaymentMethodNonceListAdapter extends BaseAdapter implements
        DialogInterface.OnClickListener {

    public interface PaymentMethodNonceSelectedListener {
        void onPaymentMethodSelected(int index);
    }

    private Context mContext;
    private PaymentMethodNonceSelectedListener mPaymentMethodNonceSelectedListener;
    private List<PaymentMethodNonce> mPaymentMethodNonces;

    public PaymentMethodNonceListAdapter(Context context, PaymentMethodNonceSelectedListener listener,
            List<PaymentMethodNonce> paymentMethodNonces) {
        mContext = context;
        mPaymentMethodNonceSelectedListener = listener;
        mPaymentMethodNonces = paymentMethodNonces;
    }

    @Override
    public int getCount() {
        return mPaymentMethodNonces.size();
    }

    @Override
    public Object getItem(int position) {
        return mPaymentMethodNonces.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        PaymentMethodNonceView paymentMethodNonceView = new PaymentMethodNonceView(mContext);
        paymentMethodNonceView.setPaymentMethodNonceDetails(mPaymentMethodNonces.get(position));
        return paymentMethodNonceView;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        mPaymentMethodNonceSelectedListener.onPaymentMethodSelected(which);
    }
}
