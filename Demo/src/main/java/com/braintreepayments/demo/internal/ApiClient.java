package com.braintreepayments.demo.internal;

import com.braintreepayments.demo.ClientTokenRequest;
import com.braintreepayments.demo.TransactionRequest;
import com.braintreepayments.demo.models.ClientToken;
import com.braintreepayments.demo.models.Transaction;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.POST;

public interface ApiClient {

    @POST("/client_tokens")
    void getClientToken(@Body ClientTokenRequest request, Callback<ClientToken> callback);

    @POST("/transactions")
    void createTransaction(@Body TransactionRequest request, Callback<Transaction> callback);
}
