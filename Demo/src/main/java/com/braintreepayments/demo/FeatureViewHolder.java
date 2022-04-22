package com.braintreepayments.demo;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class FeatureViewHolder extends RecyclerView.ViewHolder {
    public FeatureViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public void bind(DemoFeature demoFeature) {
        String featureText = null;
        switch (demoFeature) {
            case CREDIT_OR_DEBIT_CARDS:
                featureText = "Credit or Debit Cards";
                break;
            case PAYPAL:
                featureText = "PayPal";
                break;
            case VENMO:
                featureText = "Venmo";
                break;
            case GOOGLE_PAY:
                featureText = "Google Pay";
                break;
            case SAMSUNG_PAY:
                featureText = "Samsung Pay";
                break;
            case VISA_CHECKOUT:
                featureText = "Visa Checkout";
                break;
            case LOCAL_PAYMENT:
                featureText = "Local Payment";
                break;
            case PREFERRED_PAYMENT_METHODS:
                featureText = "Preferred Payment Methods";
                break;
        }

        TextView textView = itemView.findViewById(R.id.title);
        textView.setText(featureText);
    }
}
