package com.braintreepayments.api.interfaces;

public interface BraintreeResponseListener<T> {

    void onResponse(T t);
}
