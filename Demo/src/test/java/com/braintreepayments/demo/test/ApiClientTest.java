package com.braintreepayments.demo.test;

import android.test.suitebuilder.annotation.MediumTest;

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
import static junit.framework.Assert.fail;

public class ApiClientTest {

    private CountDownLatch mCountDownLatch;
    private ApiClient mApiClient;

    @Before
    public void setup() {
        mCountDownLatch = new CountDownLatch(1);
        mApiClient = new RestAdapter.Builder()
                .setEndpoint(Settings.getSandboxUrl())
                .setRequestInterceptor(new ApiClientRequestInterceptor())
                .setLogLevel(LogLevel.FULL)
                .build()
                .create(ApiClient.class);
    }

    @Test(timeout = 10000)
    @MediumTest
    public void getClientToken_returnsAClientToken() throws InterruptedException {
        mApiClient.getClientToken(null, null, new Callback<ClientToken>() {
            @Override
            public void success(ClientToken clientToken, Response response) {
                assertNotNull(clientToken.getClientToken());
                mCountDownLatch.countDown();
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                fail(retrofitError.getMessage());
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void getClientToken_returnsAClientTokenForACustomer() throws InterruptedException {
        mApiClient.getClientToken("customer", null, new Callback<ClientToken>() {
            @Override
            public void success(ClientToken clientToken, Response response) {
                assertNotNull(clientToken.getClientToken());
                mCountDownLatch.countDown();
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                fail(retrofitError.getMessage());
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void getClientToken_returnsAClientTokenForAMerchantAccount() throws InterruptedException {
        mApiClient.getClientToken(null, "test_AIB", new Callback<ClientToken>() {
            @Override
            public void success(ClientToken clientToken, Response response) {
                assertNotNull(clientToken.getClientToken());
                mCountDownLatch.countDown();
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                fail(retrofitError.getMessage());
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void createTransaction_createsATransaction() throws InterruptedException {
        mApiClient.createTransaction("fake-valid-nonce", null, false, new Callback<Transaction>() {
            @Override
            public void success(Transaction transaction, Response response) {
                assertNotNull(transaction.getMessage());
                mCountDownLatch.countDown();
            }

            @Override
            public void failure(RetrofitError error) {
                fail(error.getMessage());
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void createTransaction_failsWhenNonceIsAlreadyConsumed() throws InterruptedException {
        mApiClient.createTransaction("fake-consumed-nonce", null, false, new Callback<Transaction>() {
            @Override
            public void success(Transaction transaction, Response response) {
                assertEquals("Cannot use a payment_method_nonce more than once.", transaction.getMessage());
                mCountDownLatch.countDown();
            }

            @Override
            public void failure(RetrofitError error) {
                fail(error.getMessage());
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void createTransaction_failsWhenThreeDSecureIsRequired() throws InterruptedException {
        mApiClient.createTransaction("fake-valid-nonce", "test_AIB", true,
                new Callback<Transaction>() {
                    @Override
                    public void success(Transaction transaction, Response response) {
                        assertEquals("Gateway Rejected: three_d_secure", transaction.getMessage());
                        mCountDownLatch.countDown();
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        fail(error.getMessage());
                    }
                });

        mCountDownLatch.await();
    }
}
