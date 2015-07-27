package com.braintreepayments.api.test;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.braintreepayments.api.BraintreePaymentTestActivity;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.BraintreeHttpClient;

import static com.braintreepayments.api.BraintreePaymentTestActivity.CONFIGURATION_ERROR;
import static com.braintreepayments.api.BraintreePaymentTestActivity.GET_PAYMENT_METHODS;
import static com.braintreepayments.api.BraintreePaymentTestActivity.GET_PAYMENT_METHODS_ERROR;
import static com.braintreepayments.api.BraintreePaymentTestActivity.MOCK_CONFIGURATION;
import static com.braintreepayments.api.BraintreePaymentTestActivity.TOKENIZE_CREDIT_CARD;
import static com.braintreepayments.api.BraintreePaymentTestActivity.TOKENIZE_CREDIT_CARD_ERROR;

public class BraintreeTestHttpClient extends BraintreeHttpClient {

    private Intent mIntent;
    private long mDelay;

    public BraintreeTestHttpClient(String authorizationFingerprint, Intent intent) {
        super(authorizationFingerprint);

        BraintreeHttpClient.DEBUG = true;

        mIntent = intent;
        mDelay = mIntent.getLongExtra(BraintreePaymentTestActivity.EXTRA_DELAY, 0);
    }

    @Override
    public void get(final String path, final HttpResponseCallback callback) {
        mThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                if (!(path.contains("configuration") && mIntent.hasExtra(MOCK_CONFIGURATION))) {
                    SystemClock.sleep(mDelay);
                }

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        callGet(path, callback);
                    }
                });
            }
        });
    }

    private void callGet(String path, HttpResponseCallback callback) {
        if (path.contains("configuration") && (mIntent.hasExtra(MOCK_CONFIGURATION) ||
                mIntent.hasExtra(CONFIGURATION_ERROR))) {
            if (mIntent.hasExtra(MOCK_CONFIGURATION)) {
                callback.success(mIntent.getStringExtra(MOCK_CONFIGURATION));
            } else if (mIntent.hasExtra(CONFIGURATION_ERROR)) {
                callback.failure(
                        (Exception) mIntent.getSerializableExtra(CONFIGURATION_ERROR));
            }
        } else if (path.equals(GET_PAYMENT_METHODS) && mIntent.hasExtra(GET_PAYMENT_METHODS_ERROR)) {
            callback.failure(
                    (Exception) mIntent.getSerializableExtra(GET_PAYMENT_METHODS_ERROR));
        } else if (mIntent.hasExtra(path)) {
            callback.success(mIntent.getStringExtra(path));
        } else {
            super.get(path, callback);
        }
    }

    @Override
    public void post(final String path, final String data,
            final HttpResponseCallback callback) {
        mThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                SystemClock.sleep(mDelay);

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        callPost(path, data, callback);
                    }
                });
            }
        });
    }

    private void callPost(String path, String data, HttpResponseCallback callback) {
        if (path.equals(TOKENIZE_CREDIT_CARD) && mIntent.hasExtra(TOKENIZE_CREDIT_CARD_ERROR)) {
            callback.failure(
                    (Exception) mIntent.getSerializableExtra(TOKENIZE_CREDIT_CARD_ERROR));
        } else if (mIntent.hasExtra(path)) {
            callback.success(mIntent.getStringExtra(path));
        } else {
            super.post(path, data, callback);
        }
    }
}
