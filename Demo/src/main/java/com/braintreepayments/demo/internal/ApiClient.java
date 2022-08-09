package com.braintreepayments.demo.internal;

import com.braintreepayments.demo.ClientTokenRequest;
import com.braintreepayments.demo.TransactionRequest;
import com.braintreepayments.demo.models.ClientToken;
import com.braintreepayments.demo.models.Nonce;
import com.braintreepayments.demo.models.PaymentMethodToken;
import com.braintreepayments.demo.models.Transaction;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Query;

public interface ApiClient {

    @POST("/client_tokens")
    void getClientToken(@Body ClientTokenRequest request, Callback<ClientToken> callback);

    @POST("/transactions")
    void createTransaction(@Body TransactionRequest request, Callback<Transaction> callback);
}
