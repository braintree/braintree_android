package com.braintreepayments.demo;

import android.content.Context;
import android.content.SharedPreferences;

import com.braintreepayments.api.localpayment.LocalPaymentPendingRequest;
import com.braintreepayments.api.paypal.PayPalPendingRequest;
import com.braintreepayments.api.paypal.vaultedit.PayPalVaultEditPendingRequest;
import com.braintreepayments.api.sepadirectdebit.SEPADirectDebitPendingRequest;
import com.braintreepayments.api.venmo.VenmoPendingRequest;

public class PendingRequestStore {

    static final String PREFERENCES_KEY = "PENDING_REQUEST_SHARED_PREFERENCES";

    static final String PAYPAL_PENDING_REQUEST_KEY = "PAYPAL_PENDING_REQUEST";
    static final String LOCAL_PAYMENT_PENDING_REQUEST_KEY = "LOCAL_PAYMENT_PENDING_REQUEST";
    static final String SEPA_DIRECT_DEBIT_PENDING_REQUEST_KEY = "SEPA_DIRECT_DEBIT_PENDING_REQUEST";
    static final String VENMO_PENDING_REQUEST_KEY = "VENMO_PENDING_REQUEST";
    static final String EDIT_FI_PENDING_REQUEST_KEY = "EDIT_FI_PENDING_REQUEST";

    private static final PendingRequestStore INSTANCE = new PendingRequestStore();

    static PendingRequestStore getInstance() {
        return INSTANCE;
    }

    public void putVenmoPendingRequest(Context context,
                                       VenmoPendingRequest.Started pendingRequest) {
        put(VENMO_PENDING_REQUEST_KEY, pendingRequest.getPendingRequestString(), context);
    }

    public VenmoPendingRequest.Started getVenmoPendingRequest(Context context) {
        String requestString = get(VENMO_PENDING_REQUEST_KEY, context);
        if (requestString != null) {
            return new VenmoPendingRequest.Started(requestString);
        }
        return null;
    }

    public void clearVenmoPendingRequest(Context context) {
        remove(VENMO_PENDING_REQUEST_KEY, context);
    }

    public void putPayPalPendingRequest(Context context,
                                        PayPalPendingRequest.Started pendingRequest) {
        put(PAYPAL_PENDING_REQUEST_KEY, pendingRequest.getPendingRequestString(), context);
    }

    public void putPayPalPendingRequestEditFi(Context context,
                                              PayPalVaultEditPendingRequest.Started pendingRequest) {
        put(EDIT_FI_PENDING_REQUEST_KEY, pendingRequest.getPendingRequestString(), context);
    }

    public PayPalPendingRequest.Started getPayPalPendingRequest(Context context) {
        String requestString = get(PAYPAL_PENDING_REQUEST_KEY, context);
        if (requestString != null) {
            return new PayPalPendingRequest.Started(requestString);
        }
        return null;
    }

    public PayPalVaultEditPendingRequest.Started getPayPalPendingRequestEditFi(Context context) {
        String requestString = get(EDIT_FI_PENDING_REQUEST_KEY, context);
        if (requestString != null) {
            return new PayPalVaultEditPendingRequest.Started(requestString);
        }
        return null;
    }

    public void clearPayPalPendingRequest(Context context) {
        remove(PAYPAL_PENDING_REQUEST_KEY, context);
    }

    public void clearPayPalPendingRequestEditFi(Context context) {
        remove(EDIT_FI_PENDING_REQUEST_KEY, context);
    }

    public void putLocalPaymentPendingRequest(Context context,
                                              LocalPaymentPendingRequest.Started pendingRequest) {
        put(LOCAL_PAYMENT_PENDING_REQUEST_KEY, pendingRequest.getPendingRequestString(), context);
    }

    public LocalPaymentPendingRequest.Started getLocalPaymentPendingRequest(Context context) {
        String requestString = get(LOCAL_PAYMENT_PENDING_REQUEST_KEY, context);
        if (requestString != null) {
            return new LocalPaymentPendingRequest.Started(requestString);
        }
        return null;
    }

    public void clearLocalPaymentPendingRequest(Context context) {
        remove(LOCAL_PAYMENT_PENDING_REQUEST_KEY, context);
    }

    public void putSEPADirectDebitPendingRequest(Context context,
                                                 SEPADirectDebitPendingRequest.Started pendingRequest) {
        put(SEPA_DIRECT_DEBIT_PENDING_REQUEST_KEY, pendingRequest.getPendingRequestString(), context);
    }

    public SEPADirectDebitPendingRequest.Started getSEPADirectDebitPendingRequest(Context context) {
        String requestString = get(SEPA_DIRECT_DEBIT_PENDING_REQUEST_KEY, context);
        if (requestString != null) {
            return new SEPADirectDebitPendingRequest.Started(requestString);
        }
        return null;
    }

    public void clearSEPADirectDebitPendingRequest(Context context) {
        remove(SEPA_DIRECT_DEBIT_PENDING_REQUEST_KEY, context);
    }

    static void put(String key, String value, Context context) {
        Context applicationContext = context.getApplicationContext();
        SharedPreferences sharedPreferences =
            applicationContext.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(key, value).apply();
    }

    static String get(String key, Context context) {
        Context applicationContext = context.getApplicationContext();
        SharedPreferences sharedPreferences =
            applicationContext.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
        return sharedPreferences.getString(key, null);
    }

    static void remove(String key, Context context) {
        Context applicationContext = context.getApplicationContext();
        SharedPreferences sharedPreferences =
            applicationContext.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
        sharedPreferences.edit().remove(key).apply();
    }

}
