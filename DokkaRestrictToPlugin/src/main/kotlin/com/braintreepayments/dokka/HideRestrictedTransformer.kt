package com.braintreepayments.dokka

import org.jetbrains.dokka.base.transformers.documentables.SuppressedByConditionDocumentableFilterTransformer
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.plugability.DokkaContext

/**
 * Suppresses any [Documentable] that carries an `androidx.annotation.RestrictTo` annotation.
 *
 * Runs as a pre-merge transformer so suppression happens per source set before the multi-module
 * merge, matching how the built-in `@suppress` KDoc filter and the android-documentation-plugin
 * operate.
 */
public class HideRestrictedTransformer(context: DokkaContext) :
    SuppressedByConditionDocumentableFilterTransformer(context) {

    override fun shouldBeSuppressed(d: Documentable): Boolean {
        val annotations = (d as? WithExtraProperties<*>)
            ?.extra
            ?.allOfType<Annotations>()
            ?.flatMap { it.directAnnotations.values.flatten() }
            .orEmpty()

        return annotations.any { it.dri.classNames == RESTRICT_TO_CLASS_NAME }
    }

    private companion object {
        const val RESTRICT_TO_CLASS_NAME = "RestrictTo"
    }
}