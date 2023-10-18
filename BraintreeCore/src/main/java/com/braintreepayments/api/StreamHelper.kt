package com.braintreepayments.api

import androidx.annotation.RestrictTo
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.nio.charset.Charset

// NEXT_MAJOR_VERSION: remove class once its added to drop in
/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object StreamHelper {
    @JvmStatic
    @Throws(IOException::class)
    fun getString(inputStream: InputStream?): String {
        val buffReader = BufferedReader(InputStreamReader(inputStream, Charset.forName("UTF-8")))
        return buffReader.use { reader ->
            val data = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                data.append(line)
            }
            data.toString()
        }
    }
}
