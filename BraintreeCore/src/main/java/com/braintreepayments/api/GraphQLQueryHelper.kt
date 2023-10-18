package com.braintreepayments.api

import android.content.Context
import android.content.res.Resources
import androidx.annotation.RestrictTo
import java.io.IOException
import java.io.InputStream

// NEXT_MAJOR_VERSION: remove class once its added to drop in
/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object GraphQLQueryHelper {
    @JvmStatic
    @Throws(Resources.NotFoundException::class, IOException::class)
    fun getQuery(context: Context, queryResource: Int): String {
        var inputStream: InputStream? = null
        return try {
            inputStream = context.resources.openRawResource(queryResource)
            StreamHelper.getString(inputStream)
        } finally {
            inputStream?.close()
        }
    }
}
