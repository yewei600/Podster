/*
 * Copyright (c) 2020 Eric Wei. All rights reserved.
 */

package com.ericwei.podster.util

import android.os.Build
import android.text.Html
import android.text.Spanned

object HtmlUtils {
    fun htmlToSpannable(htmlDesc: String): Spanned {
        var newHtmlDesc = htmlDesc.replace("\n".toRegex(), "")
        newHtmlDesc = newHtmlDesc.replace(
            "(<(/)img>)|(<img.+?>)".toRegex(), ""
        )
        val descSpan: Spanned
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            descSpan = Html.fromHtml(
                newHtmlDesc,
                Html.FROM_HTML_MODE_LEGACY
            )
        } else {
            @Suppress("DEPRECATION")
            //breaks the text down into multiple sections to render with different styles.
            descSpan = Html.fromHtml(newHtmlDesc)
        }
        return descSpan
    }
}