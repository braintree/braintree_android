package com.braintreepayments.demo.internal;

import com.braintreepayments.demo.models.ClientToken;
import com.braintreepayments.demo.models.Nonce;
import com.braintreepayments.demo.models.PayPalUAT;
import com.braintreepayments.demo.models.PaymentMethodToken;
import com.braintreepayments.demo.models.Transaction;

import retrofit.Callback;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Query;

public interface ApiClient {

    @GET("/client_token")
    void getClientToken(@Query("customer_id") String customerId, @Query("merchant_account_id") String merchantAccountId, Callback<ClientToken> callback);

    @GET("/id-token")
    void getPayPalUAT(@Query("countryCode") String countryCode, Callback<PayPalUAT> callback);

    @FormUrlEncoded
    @POST("/nonce/transaction")
    void createTransaction(@Field("nonce") String nonce, Callback<Transaction> callback);

    @FormUrlEncoded
    @POST("/nonce/transaction")
    void createTransaction(@Field("nonce") String nonce, @Field("merchant_account_id") String merchantAccountId, Callback<Transaction> callback);

    @FormUrlEncoded
    @POST("/nonce/transaction")
    void createTransaction(@Field("nonce") String nonce, @Field("merchant_account_id") String merchantAccountId, @Field("three_d_secure_required") boolean requireThreeDSecure, Callback<Transaction> callback);

    @FormUrlEncoded
    @POST("/customers/{id}/vault")
    void createPaymentMethodToken(@Path("id") String customerId, @Field("nonce") String nonce, Callback<PaymentMethodToken> callback);

    @FormUrlEncoded
    @POST("/payment_method_nonce")
    void createPaymentMethodNonce(@Field("token") String token, Callback<Nonce> callback);
}
