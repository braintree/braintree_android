package com.braintreepayments.api.sharedutils

import android.os.Looper
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4ClassRunner::class)
class HttpClientTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var localhostCertificate: HeldCertificate
    private lateinit var clientCertificates: HandshakeCertificates

    @Before
    fun setUp() {
        localhostCertificate = HeldCertificate.Builder()
            .addSubjectAlternativeName("localhost")
            .build()

        val serverCertificates = HandshakeCertificates.Builder()
            .heldCertificate(localhostCertificate)
            .build()

        clientCertificates = HandshakeCertificates.Builder()
            .addTrustedCertificate(localhostCertificate.certificate)
            .build()

        mockWebServer = MockWebServer()
        mockWebServer.useHttps(serverCertificates.sslSocketFactory(), false)
        mockWebServer.start()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun sendRequest_dispatchesNetworkCallOffCallingThread() = runTest {
        var networkThreadName = ""
        val callingThreadName = Thread.currentThread().name

        val okHttpClient = OkHttpClient.Builder()
            .sslSocketFactory(
                clientCertificates.sslSocketFactory(),
                clientCertificates.trustManager
            )
            .addInterceptor { chain ->
                networkThreadName = Thread.currentThread().name
                chain.proceed(chain.request())
            }
            .build()

        val sut = HttpClient(
            okHttpSynchronousHttpClient = OkHttpSynchronousHttpClient(okHttpClient = okHttpClient)
        )

        mockWebServer.enqueue(MockResponse().setBody("threaded").setResponseCode(200))
        val url = mockWebServer.url("/thread").toString()
        val request = OkHttpRequest(url, Method.Get)

        sut.sendRequest(request)

        assertFalse(
            "Network call should execute on a different thread",
            networkThreadName == callingThreadName
        )
        assertFalse(
            "Network call should not execute on main thread",
            Looper.getMainLooper().thread.name == networkThreadName
        )
    }

    @Test
    fun defaultConstructor_createsWorkingInstance() {
        val client = HttpClient()
        assertNotNull(client)
    }
}
