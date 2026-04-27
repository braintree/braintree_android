package com.braintreepayments.api.sharedutils

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import javax.net.ssl.SSLSocket

@RunWith(AndroidJUnit4ClassRunner::class)
class TLSSocketFactoryTest {

    private lateinit var sut: TLSSocketFactory

    @Before
    fun setUp() {
        sut = TLSSocketFactory()
    }

    @Test
    fun constructor_withRealPinnedCertificates_initializesSuccessfully() {
        assertNotNull(sut)
        assertNotNull(sut.trustManager)
    }

    @Test
    fun trustManager_hasAcceptedIssuers() {
        val issuers = sut.trustManager.acceptedIssuers
        assertNotNull(issuers)
        assertTrue(issuers.isNotEmpty())
    }

    @Test
    fun defaultCipherSuites_returnsNonEmptyArray() {
        val cipherSuites = sut.defaultCipherSuites
        assertNotNull(cipherSuites)
        assertTrue(cipherSuites.isNotEmpty())
    }

    @Test
    fun supportedCipherSuites_returnsNonEmptyArray() {
        val cipherSuites = sut.supportedCipherSuites
        assertNotNull(cipherSuites)
        assertTrue(cipherSuites.isNotEmpty())
    }

    @Test
    fun createSocket_returnsSocketWithOnlyTLS12AndTLS13Enabled() {
        val serverSocket = javax.net.ServerSocketFactory.getDefault().createServerSocket(0)
        val port = serverSocket.localPort

        try {
            val rawSocket = java.net.Socket("localhost", port)
            val socket = sut.createSocket(rawSocket, "localhost", port, true)

            assertTrue(socket is SSLSocket)
            val enabledProtocols = (socket as SSLSocket).enabledProtocols.toList()
            assertTrue(enabledProtocols.all { it == "TLSv1.2" || it == "TLSv1.3" })
            assertTrue(enabledProtocols.contains("TLSv1.2") || enabledProtocols.contains("TLSv1.3"))

            socket.close()
        } finally {
            serverSocket.close()
        }
    }
}
