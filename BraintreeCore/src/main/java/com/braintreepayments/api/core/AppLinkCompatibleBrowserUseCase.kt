package com.braintreepayments.api.core

class AppLinkCompatibleBrowserUseCase(
    private val getDefaultBrowserUseCase: GetDefaultBrowserUseCase,
) {

    operator fun invoke(): Boolean {
        getDefaultBrowserUseCase()?.let {
            return appLinkCompatibleBrowsers.contains(it)
        }
        return false
    }

    companion object {
        private val appLinkIncompatibleBrowsers = listOf(
            "com.mi.globalbrowser",
            "com.UCMobile.intl",
            "com.duckduckgo.mobile.android",
            "com.opera.browser",
            "com.opera.gx",
            "com.opera.mini.native",
            "com.yandex.browser"
        )

        private val appLinkCompatibleBrowsers = listOf(
            "com.android.chrome",
            "com.brave.browser",
            "com.sec.android.app.sbrowser",
            "org.mozilla.firefox",
            "com.microsoft.emmx"
        )
    }
}
