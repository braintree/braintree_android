package com.braintreepayments.demo.internal;

import com.braintreepayments.demo.models.ClientToken;
import com.braintreepayments.demo.models.Nonce;
import com.braintreepayments.demo.models.PaymentMethodToken;
import com.braintreepayments.demo.models.Transaction;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiClient {

    @GET("/client_token")
    Call<ClientToken> getClientToken(@Query("customer_id") String customerId, @Query("merchant_account_id") String merchantAccountId);

    @FormUrlEncoded
    @POST("/nonce/transaction")
    Call<Transaction> createTransaction(@Field("nonce") String nonce, @Field("amount") String amount);

    @FormUrlEncoded
    @POST("/nonce/transaction")
    Call<Transaction> createTransaction(@Field("nonce") String nonce, @Field("amount") String amount, @Field("merchant_account_id") String merchantAccountId);

    @FormUrlEncoded
    @POST("/nonce/transaction")
    Call<Transaction> createTransaction(@Field("nonce") String nonce, @Field("amount") String amount, @Field("merchant_account_id") String merchantAccountId, @Field("three_d_secure_required") boolean requireThreeDSecure);

    @FormUrlEncoded
    @POST("/customers/{id}/vault")
    void createPaymentMethodToken(@Path("id") String customerId, @Field("nonce") String nonce, Callback<PaymentMethodToken> callback);

    @FormUrlEncoded
    @POST("/payment_method_nonce")
    void createPaymentMethodNonce(@Field("token") String token, Callback<Nonce> callback);
}
