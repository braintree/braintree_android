package com.braintreepayments.demo;

import android.content.Intent;

import androidx.annotation.Nullable;

public class ActivityResult {

    private int requestCode;
    private int resultCode;
    private Intent data;

    public int getRequestCode() {
        return requestCode;
    }

    public int getResultCode() {
        return resultCode;
    }

    public Intent getData() {
        return data;
    }

    public ActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        this.requestCode = requestCode;
        this.resultCode = resultCode;
        this.data = data;
    }
}
