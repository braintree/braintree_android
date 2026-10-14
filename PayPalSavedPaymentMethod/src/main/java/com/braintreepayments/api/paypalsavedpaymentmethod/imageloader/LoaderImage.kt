package com.braintreepayments.api.paypalsavedpaymentmethod.imageloader

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import java.io.IOException

/**
 * Renders the funding-instrument icon at [url] via [ImageLoader], falling back to [placeholder]
 * until the load completes, if it fails, or if [url] is `null` or blank.
 *
 * The icon is decorative (the FI label is rendered next to it), so it has no content description.
 */
@Composable
internal fun LoaderImage(
    url: String?,
    placeholder: Painter,
    modifier: Modifier = Modifier,
    imageLoader: ImageLoader = remember { ImageLoader.getInstance() },
) {
    // Seed from the memory cache so a cached icon renders on the first frame instead of flashing the placeholder.
    var bitmap by remember(url) { mutableStateOf<Bitmap?>(url?.let(imageLoader::peek)) }

    LaunchedEffect(url) {
        if (bitmap != null || url.isNullOrBlank()) return@LaunchedEffect
        bitmap = try {
            imageLoader.load(url)
        } catch (ignored: IOException) {
            null
        } catch (ignored: IllegalArgumentException) {
            null
        }
    }

    val loadedBitmap = bitmap
    if (loadedBitmap != null) {
        Image(
            bitmap = loadedBitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier.testTag(LOADED_IMAGE_TEST_TAG)
        )
    } else {
        Image(
            painter = placeholder,
            contentDescription = null,
            modifier = modifier.testTag(PLACEHOLDER_IMAGE_TEST_TAG)
        )
    }
}

// No semantic distinction exists otherwise, since the icon is decorative (contentDescription = null above).
internal const val LOADED_IMAGE_TEST_TAG = "loader_image_loaded"
internal const val PLACEHOLDER_IMAGE_TEST_TAG = "loader_image_placeholder"
