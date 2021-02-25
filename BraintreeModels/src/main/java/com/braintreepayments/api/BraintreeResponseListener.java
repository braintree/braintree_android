package com.braintreepayments.api;

interface BraintreeResponseListener<T> {

    void onResponse(T t);
}
