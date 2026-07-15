package com.braintreepayments.dokka

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement

/**
 * Dokka plugin that removes any declaration annotated with `androidx.annotation.RestrictTo`
 * from the generated documentation.
 *
 * Unlike the official `android-documentation-plugin` (which only hides declarations carrying a
 * `@hide` KDoc tag), this plugin keys off the `@RestrictTo` annotation directly, so internal APIs
 * are hidden automatically without needing a per-class `@suppress` KDoc tag.
 */
public class RestrictToPlugin : DokkaPlugin() {

    private val dokkaBase by lazy { plugin<DokkaBase>() }

    public val hideRestrictedApi by extending {
        dokkaBase.preMergeDocumentableTransformer providing ::HideRestrictedTransformer
    }

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement =
        PluginApiPreviewAcknowledgement
}