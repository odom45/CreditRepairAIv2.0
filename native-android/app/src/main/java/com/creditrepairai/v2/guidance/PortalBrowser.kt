package com.creditrepairai.v2.guidance

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

/** No WebView, JS, accessibility service, browser cookie access, navigation callbacks or login hooks. */
object PortalBrowser {
    fun open(context: Context, url: String): Boolean = runCatching {
        require(Uri.parse(url).scheme == "https")
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setInitialActivityHeightPx((context.resources.displayMetrics.heightPixels * .60).toInt())
            .setBackgroundInteractionEnabled(true)
            .build().launchUrl(context, Uri.parse(url))
    }.isSuccess
}
