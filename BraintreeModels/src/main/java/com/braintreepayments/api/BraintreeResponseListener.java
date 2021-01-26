package com.braintreepayments.api;

public interface BraintreeResponseListener<T> {

    void onResponse(T t);
}
