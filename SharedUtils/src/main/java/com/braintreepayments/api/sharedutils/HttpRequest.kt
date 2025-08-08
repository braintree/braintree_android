package com.braintreepayments.api.sharedutils

import androidx.annotation.RestrictTo
import java.io.File
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class HttpRequest(
    val readTimeout: Int = THIRTY_SECONDS_MS,
    val connectTimeout: Int = THIRTY_SECONDS_MS
) {

    companion object {
        private const val THIRTY_SECONDS_MS = 30000

        @JvmStatic
        fun newInstance(): HttpRequest {
            return HttpRequest()
        }

        private fun join(path1: String, path2: String): String {
            val f1 = File(path1)
            val f2 = File(f1, path2)
            return f2.path
        }
    }

    var path: String? = null
    var data: ByteArray? = null
    var method: String? = null

    private var baseUrl: String? = ""
    private var _headers: MutableMap<String, String>? = null
    private val additionalHeaders: MutableMap<String, String> = HashMap()

    fun path(path: String?): HttpRequest {
        this.path = path
        return this
    }

    fun baseUrl(baseUrl: String?): HttpRequest {
        this.baseUrl = baseUrl
        return this
    }

    fun data(dataAsString: String): HttpRequest {
        this.data = dataAsString.toByteArray(StandardCharsets.UTF_8)
        return this
    }

    fun method(method: String): HttpRequest {
        this.method = method
        return this
    }

    fun addHeader(name: String, value: String): HttpRequest {
        additionalHeaders[name] = value
        return this
    }

    fun dispose() {
        data?.fill(0)
    }

    val headers: Map<String, String>
        get() {
            return _headers ?: HashMap<String, String>().apply {
                put("Accept-Encoding", "gzip")
                put("Accept-Language", Locale.getDefault().language)
                putAll(additionalHeaders)
            }
        }

    val url: URL
        @Throws(MalformedURLException::class, URISyntaxException::class)
        get() {
            val currentPath = path ?: ""
            return if (currentPath.startsWith("http")) {
                URL(currentPath)
            } else {
                val baseURI = URL(baseUrl).toURI()
                val newPath = join(baseURI.path ?: "", currentPath)
                val newURI = baseURI.resolve(newPath).normalize()
                newURI.toURL()
            }
        }
}
