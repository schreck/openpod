package com.openpod.data.network

import com.openpod.data.db.Podcast

object OpmlWriter {
    fun write(podcasts: List<Podcast>): String = buildString {
        appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        appendLine("<opml version=\"2.0\">")
        appendLine("  <head><title>OpenPod Subscriptions</title></head>")
        appendLine("  <body>")
        for (podcast in podcasts) {
            val title = podcast.title.replace("\"", "&quot;")
            appendLine("    <outline type=\"rss\" text=\"$title\" xmlUrl=\"${podcast.feedUrl}\"/>")
        }
        appendLine("  </body>")
        append("</opml>")
    }
}
