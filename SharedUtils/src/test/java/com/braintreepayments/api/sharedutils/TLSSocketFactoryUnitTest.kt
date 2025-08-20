package com.braintreepayments.api.sharedutils

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test
import java.io.InputStream
import java.net.InetAddress
import java.net.Socket
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class TLSSocketFactoryUnitTest {

    private val inputStream: InputStream = mockk(relaxed = true)
    private val keyStore: KeyStore = mockk(relaxed = true)
    private val certificateFactory: CertificateFactory = mockk(relaxed = true)
    private val trustManagerFactory: TrustManagerFactory = mockk(relaxed = true)
    private val sslContext: SSLContext = mockk(relaxed = true)

    private val internalSocketFactory: SSLSocketFactory = mockk(relaxed = true)
    private val expectedSocket = mockk<SSLSocket>(relaxed = true)
    private val trustManagers: Array<TrustManager> = arrayOf(mockk<X509TrustManager>(relaxed = true))

    private lateinit var subject: TLSSocketFactory

    private fun initializeSubject(
        trustManagers: Array<TrustManager>? = this.trustManagers
    ) {
        every { sslContext.socketFactory } returns internalSocketFactory
        every { expectedSocket.supportedProtocols } returns arrayOf("TLSv1.2", "TLSv1.3", "TLSv1.1")

        every { trustManagerFactory.trustManagers } returns trustManagers

        subject = TLSSocketFactory(
            certificateStream = inputStream,
            keyStore = keyStore,
            certificateFactory = certificateFactory,
            trustManagerFactory = trustManagerFactory,
            sslContext = sslContext
        )
    }

    @Test
    fun `on init, keystore is loaded`() {
        initializeSubject()

        verify { keyStore.load(null, null) }
    }

    @Test
    fun `on init, keystore sets X509Certificate entries from the certificateFactory`() {
        val cert1Name = "cert 1 name"
        val cert2Name = "cert 2 name"
        val cert1 = mockk<X509Certificate>().apply {
            every { subjectDN.name } returns cert1Name
        }
        val cert2 = mockk<X509Certificate>().apply {
            every { subjectDN.name } returns cert2Name
        }
        every { certificateFactory.generateCertificates(inputStream) } returns listOf(cert1, cert2)

        initializeSubject()

        verify { keyStore.setCertificateEntry(cert1Name, cert1) }
        verify { keyStore.setCertificateEntry(cert2Name, cert2) }
    }

    @Test
    fun `on init, trustManagerFactory is initialized`() {
        initializeSubject()

        verify { trustManagerFactory.init(keyStore) }
    }

    @Test
    fun `on init, sslContext is initialized`() {
        initializeSubject()

        verify { sslContext.init(null, trustManagers, null) }
    }

    @Test(expected = SSLException::class)
    fun `when there's an exception thrown in init, the exception is wrapped with a SSLException`() {
        val exception = Exception("expected exception")
        every { keyStore.load(any(), any()) } throws exception

        initializeSubject()
    }

    @Test
    fun `on init, the inputStream is closed`() {
        initializeSubject()

        verify { inputStream.close() }
    }

    @Test
    fun `getDefaultCipherSuites calls the internal socket factory`() {
        val expectedArray = arrayOf("string1", "string2")
        every { internalSocketFactory.defaultCipherSuites } returns expectedArray
        initializeSubject()

        val result = subject.defaultCipherSuites

        assertEquals(expectedArray, result)
    }

    @Test
    fun `getSupportedCipherSuites calls the internal socket factory`() {
        val expectedArray = arrayOf("string1", "string2")
        every { internalSocketFactory.supportedCipherSuites } returns expectedArray
        initializeSubject()

        val result = subject.supportedCipherSuites

        assertEquals(expectedArray, result)
    }

    @Test
    fun `createSocket with socket, host, port, and autoClose, calls internalSSLSocketFactory createSocket`() {
        val socket = mockk<Socket>()
        val host = "host"
        val port = 1
        val autoClose = true
        every { internalSocketFactory.createSocket(socket, host, port, autoClose) } returns expectedSocket
        initializeSubject()

        val result = subject.createSocket(
            s = socket,
            host = host,
            port = port,
            autoClose = autoClose
        )

        verifyCorrectTLSProtocols()
        assertEquals(expectedSocket, result)
    }

    @Test
    fun `createSocket with host and port, calls internalSSLSocketFactory createSocket`() {
        val host = "host"
        val port = 1
        every { internalSocketFactory.createSocket(host, port) } returns expectedSocket
        initializeSubject()

        val result = subject.createSocket(
            host = host,
            port = port,
        )

        verifyCorrectTLSProtocols()
        assertEquals(expectedSocket, result)
    }

    @Test
    fun `createSocket with host, port, localHost, and localPort, calls internalSSLSocketFactory createSocket`() {
        val host = "host"
        val port = 1
        val localHost = mockk<InetAddress>()
        val localPort = 2
        every { internalSocketFactory.createSocket(host, port, localHost, localPort) } returns expectedSocket
        initializeSubject()

        val result = subject.createSocket(
            host = host,
            port = port,
            localHost = localHost,
            localPort = localPort
        )

        verifyCorrectTLSProtocols()
        assertEquals(expectedSocket, result)
    }

    @Test
    fun `createSocket with InetAddress and port, calls internalSSLSocketFactory createSocket`() {
        val host = mockk<InetAddress>()
        val port = 1
        every { internalSocketFactory.createSocket(host, port) } returns expectedSocket
        initializeSubject()

        val result = subject.createSocket(
            host = host,
            port = port,
        )

        verifyCorrectTLSProtocols()
        assertEquals(expectedSocket, result)
    }

    @Test
    fun `createSocket with address, port, localAddress, and localPort, calls internalSSLSocketFactory createSocket`() {
        val address = mockk<InetAddress>()
        val port = 1
        val localAddress = mockk<InetAddress>()
        val localPort = 2
        every { internalSocketFactory.createSocket(address, port, localAddress, localPort) } returns expectedSocket
        initializeSubject()

        val result = subject.createSocket(
            address = address,
            port = port,
            localAddress = localAddress,
            localPort = localPort
        )

        verifyCorrectTLSProtocols()
        assertEquals(expectedSocket, result)
    }

    @Test
    fun `createSocket with SSLSocket, calls internalSSLSocketFactory createSocket`() {
        val host = mockk<InetAddress>()
        val port = 1
        every { internalSocketFactory.createSocket(host, port) } returns expectedSocket
        initializeSubject()

        val result = subject.createSocket(
            host = host,
            port = port,
        )

        verifyCorrectTLSProtocols()
        assertEquals(expectedSocket, result)
    }

    @Test
    fun `on init, trustManager is set to first X509TrustManager from trustManagerFactory`() {
        val x509TrustManager = mockk<X509TrustManager>(relaxed = true)
        val otherTrustManager = mockk<javax.net.ssl.TrustManager>(relaxed = true)

        initializeSubject(trustManagers = arrayOf(otherTrustManager, x509TrustManager))

        assertEquals(x509TrustManager, subject.trustManager)
    }

    @Test
    fun `on init, trustManager ignores non-X509TrustManager instances`() {
        val x509TrustManager = mockk<X509TrustManager>(relaxed = true)
        val otherTrustManager1 = mockk<TrustManager>(relaxed = true)
        val otherTrustManager2 = mockk<TrustManager>(relaxed = true)

        initializeSubject(
            trustManagers = arrayOf(
                otherTrustManager1,
                otherTrustManager2,
                x509TrustManager
            )
        )

        assertEquals(x509TrustManager, subject.trustManager)
    }

    @Test(expected = SSLException::class)
    fun `on init, when no X509TrustManager is found, throws SSLException`() {
        val otherTrustManager1 = mockk<TrustManager>(relaxed = true)
        val otherTrustManager2 = mockk<TrustManager>(relaxed = true)

        initializeSubject(trustManagers = arrayOf(otherTrustManager1, otherTrustManager2))
    }

    @Test(expected = SSLException::class)
    fun `on init, when trustManagers array is empty, throws SSLException`() {
        initializeSubject(trustManagers = emptyArray())
    }

    @Test(expected = SSLException::class)
    fun `on init, when trustManagers is null, throws SSLException`() {
        initializeSubject(trustManagers = null)
    }

    @Test
    fun `on init, trustManager uses first X509TrustManager when multiple exist`() {
        val firstX509TrustManager = mockk<X509TrustManager>(relaxed = true)
        val secondX509TrustManager = mockk<X509TrustManager>(relaxed = true)

        initializeSubject(
            trustManagers = arrayOf(
                firstX509TrustManager,
                secondX509TrustManager
            )
        )

        assertEquals(firstX509TrustManager, subject.trustManager)
    }

    private fun verifyCorrectTLSProtocols() {
        verify {
            expectedSocket.enabledProtocols = withArg {
                assertTrue(it.size == 2)
                assertTrue(it.contains("TLSv1.2"))
                assertTrue(it.contains("TLSv1.3"))
            }
        }
    }
}
