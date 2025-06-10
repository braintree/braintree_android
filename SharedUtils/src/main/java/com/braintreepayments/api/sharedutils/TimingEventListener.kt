package com.braintreepayments.api.sharedutils

import android.util.Log
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Handshake
import okhttp3.Protocol
import okhttp3.Response
import java.net.InetAddress
import java.net.Proxy
import java.net.InetSocketAddress

class TimingEventListener : EventListener() {
    private var callStartNanos: Long = 0
    private var dnsStartNanos: Long = 0
    private var dnsEndNanos: Long = 0
    private var connectStartNanos: Long = 0
    private var connectEndNanos: Long = 0
    private var secureConnectStartNanos: Long = 0
    private var secureConnectEndNanos: Long = 0
    private var requestHeadersStartNanos: Long = 0
    private var requestHeadersEndNanos: Long = 0
    private var requestBodyStartNanos: Long = 0
    private var requestBodyEndNanos: Long = 0
    private var responseHeadersStartNanos: Long = 0
    private var responseHeadersEndNanos: Long = 0
    private var responseBodyStartNanos: Long = 0
    private var responseBodyEndNanos: Long = 0
    private var callEndNanos: Long = 0
    private var callFailedNanos: Long = 0

    // Add value to track time between request headers end and response headers start
    private var requestToResponseHeadersDurationMs: Long = 0

    override fun callStart(call: Call) {
        callStartNanos = System.nanoTime()
        Log.d("asdf", "Call started")
    }

    override fun dnsStart(call: Call, domainName: String) {
        dnsStartNanos = System.nanoTime()
        Log.d("asdf", "DNS start: $domainName")
    }

    override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<InetAddress>) {
        dnsEndNanos = System.nanoTime()
        val durationMs = (dnsEndNanos - dnsStartNanos) / 1_000_000
        Log.d("asdf", "DNS end: $domainName, duration: ${durationMs}ms")
    }

    override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
        connectStartNanos = System.nanoTime()
        Log.d("asdf", "Connect start: $inetSocketAddress")
    }

    override fun secureConnectStart(call: Call) {
        secureConnectStartNanos = System.nanoTime()
        Log.d("asdf", "TLS handshake start")
    }

    override fun secureConnectEnd(call: Call, handshake: Handshake?) {
        secureConnectEndNanos = System.nanoTime()
        val durationMs = (secureConnectEndNanos - secureConnectStartNanos) / 1_000_000
        Log.d("asdf", "TLS handshake end, duration: ${durationMs}ms")
    }

    override fun connectEnd(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: Proxy,
        protocol: Protocol?
    ) {
        connectEndNanos = System.nanoTime()
        val durationMs = (connectEndNanos - connectStartNanos) / 1_000_000
        Log.d("asdf", "Connect end: $inetSocketAddress, duration: ${durationMs}ms")
    }

    override fun connectFailed(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: Proxy,
        protocol: Protocol?,
        ioe: java.io.IOException
    ) {
        val failedNanos = System.nanoTime()
        val durationMs = (failedNanos - connectStartNanos) / 1_000_000
        Log.d("asdf", "Connect failed: $inetSocketAddress, duration: ${durationMs}ms, error: ${ioe.message}")
    }

    override fun requestHeadersStart(call: Call) {
        requestHeadersStartNanos = System.nanoTime()
        Log.d("asdf", "Request headers start")
    }

    override fun requestHeadersEnd(call: Call, request: okhttp3.Request) {
        requestHeadersEndNanos = System.nanoTime()
        val durationMs = (requestHeadersEndNanos - requestHeadersStartNanos) / 1_000_000
        Log.d("asdf", "Request headers end, duration: ${durationMs}ms")
    }

    override fun requestBodyStart(call: Call) {
        requestBodyStartNanos = System.nanoTime()
        Log.d("asdf", "Request body start")
    }

    override fun requestBodyEnd(call: Call, byteCount: Long) {
        requestBodyEndNanos = System.nanoTime()
        val durationMs = (requestBodyEndNanos - requestBodyStartNanos) / 1_000_000
        Log.d("asdf", "Request body end, duration: ${durationMs}ms, bytes: $byteCount")
    }

    override fun responseHeadersStart(call: Call) {
        responseHeadersStartNanos = System.nanoTime()
        // Calculate and log time between request headers end and response headers start
        requestToResponseHeadersDurationMs = (responseHeadersStartNanos - requestHeadersEndNanos) / 1_000_000
        Log.d("asdf", "Response headers start")
        Log.d(
            "asdf",
            "Time between request headers end and response headers start: ${requestToResponseHeadersDurationMs}ms"
        )
    }

    override fun responseHeadersEnd(call: Call, response: Response) {
        responseHeadersEndNanos = System.nanoTime()
        val durationMs = (responseHeadersEndNanos - responseHeadersStartNanos) / 1_000_000
        // Calculate and log time between request headers end and response headers end
        Log.d("asdf", "Response headers end, duration: ${durationMs}ms")
    }

    override fun responseBodyStart(call: Call) {
        responseBodyStartNanos = System.nanoTime()
        Log.d("asdf", "Response body start")
    }

    override fun responseBodyEnd(call: Call, byteCount: Long) {
        responseBodyEndNanos = System.nanoTime()
        val durationMs = (responseBodyEndNanos - responseBodyStartNanos) / 1_000_000
        Log.d("asdf", "Response body end, duration: ${durationMs}ms, bytes: $byteCount")
    }

    override fun callEnd(call: Call) {
        callEndNanos = System.nanoTime()
        val totalDurationMs = (callEndNanos - callStartNanos) / 1_000_000
        Log.d("asdf", "Call end, total duration: ${totalDurationMs}ms")
    }

    override fun callFailed(call: Call, ioe: java.io.IOException) {
        callFailedNanos = System.nanoTime()
        val totalDurationMs = (callFailedNanos - callStartNanos) / 1_000_000
        Log.d("asdf", "Call failed, total duration: ${totalDurationMs}ms, error: ${ioe.message}")
    }
}
