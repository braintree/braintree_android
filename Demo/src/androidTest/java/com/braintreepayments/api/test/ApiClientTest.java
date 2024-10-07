package com.braintreepayments.api.test;

import com.braintreepayments.demo.Settings;
import com.braintreepayments.demo.internal.ApiClient;
import com.braintreepayments.demo.internal.ApiClientRequestInterceptor;
import com.braintreepayments.demo.models.ClientToken;
import com.braintreepayments.demo.models.Transaction;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import androidx.annotation.NonNull;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClientTest {

    private CountDownLatch countDownLatch;
    private ApiClient apiClient;

    @Before
    public void setup() {
        countDownLatch = new CountDownLatch(1);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new ApiClientRequestInterceptor())
                .build();

        apiClient = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(Settings.getSandboxUrl())
                .client(okHttpClient)
                .build()
                .create(ApiClient.class);
    }

    @Test(timeout = 10000)
    public void getClientToken_returnsAClientToken() throws InterruptedException {
        apiClient.getClientToken(null, null).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ClientToken> call, @NonNull Response<ClientToken> response) {
                assert response.body() != null;
                assertNotNull(response.body().getClientToken());
                countDownLatch.countDown();
            }

            @Override
            public void onFailure(@NonNull Call<ClientToken> call, @NonNull Throwable throwable) {
                fail(throwable.getMessage());
            }
        });
        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void getClientToken_returnsAClientTokenForACustomer() throws InterruptedException {
        apiClient.getClientToken("customer", null).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ClientToken> call, @NonNull Response<ClientToken> response) {
                assert response.body() != null;
                assertNotNull(response.body().getClientToken());
                countDownLatch.countDown();
            }

            @Override
            public void onFailure(@NonNull Call<ClientToken> call, @NonNull Throwable throwable) {
                fail(throwable.getMessage());
            }
        });
        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void getClientToken_returnsAClientTokenForAMerchantAccount() throws InterruptedException {
        apiClient.getClientToken(null, "fake_switch_usd").enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ClientToken> call, @NonNull Response<ClientToken> response) {
                assert response.body() != null;
                assertNotNull(response.body().getClientToken());
                countDownLatch.countDown();
            }

            @Override
            public void onFailure(@NonNull Call<ClientToken> call, @NonNull Throwable throwable) {
                fail(throwable.getMessage());
            }
        });
        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void createTransaction_createsATransaction() throws InterruptedException {
        apiClient.createTransaction("fake-valid-nonce", "123").enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Transaction> call, @NonNull Response<Transaction> response) {
                assert response.body() != null;
                assertTrue(response.body().getMessage().contains("created") &&
                        response.body().getMessage().contains("authorized"));
                countDownLatch.countDown();
            }

            @Override
            public void onFailure(@NonNull Call<Transaction> call, @NonNull Throwable throwable) {
                fail(throwable.getMessage());
            }
        });
        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void createTransaction_createsATransactionWhenMerchantAccountIsNull() throws InterruptedException {
        apiClient.createTransaction("fake-valid-nonce", "123", null).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Transaction> call, @NonNull Response<Transaction> response) {
                assert response.body() != null;
                assertTrue(response.body().getMessage().contains("created") &&
                        response.body().getMessage().contains("authorized"));
                countDownLatch.countDown();
            }

            @Override
            public void onFailure(@NonNull Call<Transaction> call, @NonNull Throwable throwable) {
                fail(throwable.getMessage());
            }
        });
        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void createTransaction_createsATransactionWhenMerchantAccountIsEmpty() throws InterruptedException {
        apiClient.createTransaction("fake-valid-nonce", "123", "").enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Transaction> call, @NonNull Response<Transaction> response) {
                assert response.body() != null;
                assertTrue(response.body().getMessage().contains("created") &&
                        response.body().getMessage().contains("authorized"));
                countDownLatch.countDown();
            }

            @Override
            public void onFailure(@NonNull Call<Transaction> call, @NonNull Throwable throwable) {
                fail(throwable.getMessage());
            }
        });
        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void createTransaction_failsWhenNonceIsAlreadyConsumed() throws InterruptedException {
        apiClient.createTransaction("fake-consumed-nonce", "123").enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Transaction> call, @NonNull Response<Transaction> response) {
                assert response.body() != null;
                assertEquals("Cannot use a payment_method_nonce more than once.", response.body().getMessage());
                countDownLatch.countDown();
            }

            @Override
            public void onFailure(@NonNull Call<Transaction> call, @NonNull Throwable throwable) {
                fail(throwable.getMessage());
            }
        });
        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void createTransaction_failsWhenThreeDSecureIsRequired() throws InterruptedException {
        apiClient.createTransaction("fake-valid-nonce", "123", null, true).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Transaction> call, @NonNull Response<Transaction> response) {
                assert response.body() != null;
                assertEquals("Gateway Rejected: three_d_secure", response.body().getMessage());
                countDownLatch.countDown();
            }

            @Override
            public void onFailure(@NonNull Call<Transaction> call, @NonNull Throwable throwable) {
                fail(throwable.getMessage());
            }
        });
        countDownLatch.await();
    }
}
