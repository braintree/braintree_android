package com.braintreepayments.api.core.usecase

import android.content.pm.PackageManager
import android.net.Uri
import androidx.annotation.RestrictTo

/**
 * Use this to fetch the default application that will open the passed in URI.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GetDefaultBrowserUseCase(
    private val packageManager: PackageManager,
    private val getDefaultAppUseCase: GetDefaultAppUseCase = GetDefaultAppUseCase()
) {

    operator fun invoke(uri: Uri?): String? = getDefaultAppUseCase(
        packageManager,
        uri
    )
}
