package com.braintreepayments.demo.internal;

import com.braintreepayments.demo.ClientTokenRequest;
import com.braintreepayments.demo.TransactionRequest;
import com.braintreepayments.demo.models.ClientToken;
import com.braintreepayments.demo.models.Transaction;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiClient {

    @POST("client_tokens")
    Call<ClientToken> getClientToken(@Body ClientTokenRequest request);

    @POST("transactions")
    Call<Transaction> createTransaction(@Body TransactionRequest request);
}
