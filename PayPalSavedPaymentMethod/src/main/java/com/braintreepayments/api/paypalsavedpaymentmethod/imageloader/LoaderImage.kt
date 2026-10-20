package com.braintreepayments.api.paypalsavedpaymentmethod.imageloader

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
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
import com.braintreepayments.api.paypalsavedpaymentmethod.asHttpsUrlOrNull
import java.io.IOException

/**
 * Renders the funding-instrument icon at [url] via [ImageLoader]. While the image loads it shows [loading], which is
 * blank by default. It shows [placeholder] only when there is no image to show: if the load fails, or if [url] is
 * `null` or not an https URL with a host.
 *
 * The icon is decorative (the FI label is rendered next to it), so it has no content description.
 *
 * @param loading drawn while the image loads. It must apply the [Modifier] it receives so it takes the icon's size.
 */
@Composable
internal fun LoaderImage(
    url: String?,
    placeholder: Painter,
    modifier: Modifier = Modifier,
    loading: @Composable (Modifier) -> Unit = { Spacer(it) },
    imageLoader: ImageLoader = remember { ImageLoader.getInstance() },
) {
    // Seeded from the memory cache so a cached icon renders on the first frame instead of flashing the loading state.
    var state by remember(url) { mutableStateOf(initialState(url, imageLoader)) }

    LaunchedEffect(url) {
        if (state != LoadState.Loading || url == null) return@LaunchedEffect
        state = try {
            LoadState.Loaded(imageLoader.load(url))
        } catch (ignored: IOException) {
            LoadState.Failed
        } catch (ignored: IllegalArgumentException) {
            LoadState.Failed
        }
    }

    when (val current = state) {
        LoadState.Loading -> loading(modifier.testTag(LOADING_TEST_TAG))
        is LoadState.Loaded -> Image(
            bitmap = current.bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier.testTag(LOADED_IMAGE_TEST_TAG)
        )
        LoadState.Failed -> Image(
            painter = placeholder,
            contentDescription = null,
            modifier = modifier.testTag(PLACEHOLDER_IMAGE_TEST_TAG)
        )
    }
}

private sealed interface LoadState {
    data object Loading : LoadState
    data class Loaded(val bitmap: Bitmap) : LoadState
    data object Failed : LoadState
}

// A missing or non-https URL has nothing to load, so it goes straight to the placeholder instead of showing loading.
private fun initialState(url: String?, imageLoader: ImageLoader): LoadState {
    val validUrl = url.asHttpsUrlOrNull() ?: return LoadState.Failed
    return imageLoader.peek(validUrl)?.let(LoadState::Loaded) ?: LoadState.Loading
}

// No semantic distinction exists otherwise, since the icon is decorative (contentDescription = null above).
internal const val LOADING_TEST_TAG = "loader_image_loading"
internal const val LOADED_IMAGE_TEST_TAG = "loader_image_loaded"
internal const val PLACEHOLDER_IMAGE_TEST_TAG = "loader_image_placeholder"
