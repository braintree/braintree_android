package com.braintreepayments.api.core.usecase

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import androidx.annotation.RestrictTo

/**
 * Use to get the package name of the default application that can handle the passed in URI.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GetDefaultAppUseCase {

    operator fun invoke(packageManager: PackageManager, uri: Uri?): String? {
        val browserIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        val resolveInfo: ResolveInfo? = packageManager.resolveActivity(
            browserIntent, PackageManager
                .MATCH_DEFAULT_ONLY
        )

        if (resolveInfo != null && resolveInfo.activityInfo != null) {
            return resolveInfo.activityInfo.packageName
        }
        return null // No default browser found or resolved
    }
}
