package com.braintreepayments.api.sharedutils

import androidx.annotation.RestrictTo
import java.io.IOException
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
import javax.net.ssl.TrustManagerFactory

/**
 * `TLSSocketFactory` is a custom implementation of [SSLSocketFactory] that enforces the use of specific TLS protocols
 * (TLSv1.2 and TLSv1.3) and allows for the management of trusted certificates from a provided [InputStream].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class TLSSocketFactory(
    certificateStream: InputStream,
    keyStore: KeyStore = KeyStore.getInstance(KeyStore.getDefaultType()),
    certificateFactory: CertificateFactory = CertificateFactory.getInstance("X.509"),
    trustManagerFactory: TrustManagerFactory = TrustManagerFactory.getInstance(
        TrustManagerFactory.getDefaultAlgorithm()
    ),
    sslContext: SSLContext = SSLContext.getInstance("TLS")
) : SSLSocketFactory() {

    private val internalSSLSocketFactory: SSLSocketFactory

    init {
        try {
            keyStore.load(null, null)

            val certificates = certificateFactory.generateCertificates(certificateStream)
            for (cert in certificates) {
                if (cert is X509Certificate) {
                    keyStore.setCertificateEntry(cert.subjectDN.name, cert)
                }
            }

            trustManagerFactory.init(keyStore)
            sslContext.init(null, trustManagerFactory.trustManagers, null)
            internalSSLSocketFactory = sslContext.socketFactory
        } catch (e: Exception) {
            throw SSLException(e.message)
        } finally {
            try {
                certificateStream.close()
            } catch (ignored: IOException) {
            } catch (ignored: NullPointerException) {
            }
        }
    }

    override fun getDefaultCipherSuites(): Array<String> {
        return internalSSLSocketFactory.defaultCipherSuites
    }

    override fun getSupportedCipherSuites(): Array<String> {
        return internalSSLSocketFactory.supportedCipherSuites
    }

    @Throws(IOException::class)
    override fun createSocket(
        s: Socket,
        host: String,
        port: Int,
        autoClose: Boolean
    ): Socket {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(s, host, port, autoClose))
    }

    @Throws(IOException::class)
    override fun createSocket(host: String, port: Int): Socket {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port))
    }

    @Throws(IOException::class)
    override fun createSocket(
        host: String,
        port: Int,
        localHost: InetAddress,
        localPort: Int
    ): Socket {
        return enableTLSOnSocket(
            internalSSLSocketFactory.createSocket(host, port, localHost, localPort)
        )
    }

    @Throws(IOException::class)
    override fun createSocket(host: InetAddress, port: Int): Socket {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port))
    }

    @Throws(IOException::class)
    override fun createSocket(
        address: InetAddress,
        port: Int,
        localAddress: InetAddress,
        localPort: Int
    ): Socket {
        return enableTLSOnSocket(
            internalSSLSocketFactory.createSocket(address, port, localAddress, localPort)
        )
    }

    private fun enableTLSOnSocket(socket: Socket): Socket {
        if (socket is SSLSocket) {
            val supportedProtocols = socket.supportedProtocols.toMutableList()
            supportedProtocols.retainAll(listOf("TLSv1.2", "TLSv1.3"))

            socket.enabledProtocols = supportedProtocols.toTypedArray()
        }

        return socket
    }
}
