package com.braintreepayments.api.test;

import com.braintreepayments.demo.Settings;
import com.braintreepayments.demo.internal.ApiClient;
import com.braintreepayments.demo.internal.ApiClientRequestInterceptor;
import com.braintreepayments.demo.models.ClientToken;
import com.braintreepayments.demo.models.Transaction;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RestAdapter.LogLevel;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class ApiClientTest {

    private CountDownLatch countDownLatch;
    private ApiClient apiClient;

    @Before
    public void setup() {
        countDownLatch = new CountDownLatch(1);
        apiClient = new RestAdapter.Builder()
                .setEndpoint(Settings.getSandboxUrl())
                .setRequestInterceptor(new ApiClientRequestInterceptor())
                .setLogLevel(LogLevel.FULL)
                .build()
                .create(ApiClient.class);
    }

    @Test(timeout = 10000)
    public void getClientToken_returnsAClientToken() throws InterruptedException {
        apiClient.getClientToken(null, null, new Callback<ClientToken>() {
            @Override
            public void success(ClientToken clientToken, Response response) {
                assertNotNull(clientToken.getClientToken());
                countDownLatch.countDown();
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                fail(retrofitError.getMessage());
            }
        });

        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void getClientToken_returnsAClientTokenForACustomer() throws InterruptedException {
        apiClient.getClientToken("customer", null, new Callback<ClientToken>() {
            @Override
            public void success(ClientToken clientToken, Response response) {
                assertNotNull(clientToken.getClientToken());
                countDownLatch.countDown();
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                fail(retrofitError.getMessage());
            }
        });

        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void getClientToken_returnsAClientTokenForAMerchantAccount() throws InterruptedException {
        apiClient.getClientToken(null, "fake_switch_usd", new Callback<ClientToken>() {
            @Override
            public void success(ClientToken clientToken, Response response) {
                assertNotNull(clientToken.getClientToken());
                countDownLatch.countDown();
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                fail(retrofitError.getMessage());
            }
        });

        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void createTransaction_createsATransaction() throws InterruptedException {
        apiClient.createTransaction("fake-valid-nonce", new Callback<Transaction>() {
            @Override
            public void success(Transaction transaction, Response response) {
                assertTrue(transaction.getMessage().contains("created") && transaction.getMessage().contains("authorized"));
                countDownLatch.countDown();
            }

            @Override
            public void failure(RetrofitError error) {
                fail(error.getMessage());
            }
        });

        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void createTransaction_createsATransactionWhenMerchantAccountIsNull() throws InterruptedException {
        apiClient.createTransaction("fake-valid-nonce", null, new Callback<Transaction>() {
            @Override
            public void success(Transaction transaction, Response response) {
                assertTrue(transaction.getMessage().contains("created") && transaction.getMessage().contains("authorized"));
                countDownLatch.countDown();
            }

            @Override
            public void failure(RetrofitError error) {
                fail(error.getMessage());
            }
        });

        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void createTransaction_createsATransactionWhenMerchantAccountIsEmpty() throws InterruptedException {
        apiClient.createTransaction("fake-valid-nonce", "", new Callback<Transaction>() {
            @Override
            public void success(Transaction transaction, Response response) {
                assertTrue(transaction.getMessage().contains("created") && transaction.getMessage().contains("authorized"));
                countDownLatch.countDown();
            }

            @Override
            public void failure(RetrofitError error) {
                fail(error.getMessage());
            }
        });

        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void createTransaction_failsWhenNonceIsAlreadyConsumed() throws InterruptedException {
        apiClient.createTransaction("fake-consumed-nonce", new Callback<Transaction>() {
            @Override
            public void success(Transaction transaction, Response response) {
                assertEquals("Cannot use a payment_method_nonce more than once.", transaction.getMessage());
                countDownLatch.countDown();
            }

            @Override
            public void failure(RetrofitError error) {
                fail(error.getMessage());
            }
        });

        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void createTransaction_failsWhenThreeDSecureIsRequired() throws InterruptedException {
        apiClient.createTransaction("fake-valid-nonce", null, true, new Callback<Transaction>() {
            @Override
            public void success(Transaction transaction, Response response) {
                assertEquals("Gateway Rejected: three_d_secure", transaction.getMessage());
                countDownLatch.countDown();
            }

            @Override
            public void failure(RetrofitError error) {
                fail(error.getMessage());
            }
        });

        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void createTransaction_createsAUnionPayTransaction() throws InterruptedException {
        apiClient.createTransaction("fake-valid-unionpay-credit-nonce", "fake_switch_usd",
                new Callback<Transaction>() {
                    @Override
                    public void success(Transaction transaction, Response response) {
                        assertTrue(transaction.getMessage().contains("created") && transaction.getMessage().contains("authorized"));
                        countDownLatch.countDown();
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        fail(error.getMessage());
                    }
                });

        countDownLatch.await();
    }
}
