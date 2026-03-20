package com.openpod.data.network

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

data class ParsedFeed(
    val title: String,
    val description: String,
    val artworkUrl: String?,
    val episodes: List<ParsedEpisode>
)

data class ParsedEpisode(
    val guid: String,
    val title: String,
    val description: String?,
    val audioUrl: String,
    val duration: String?,
    val pubDate: Long
)

class RssParser @Inject constructor() {

    fun parse(xml: String): ParsedFeed {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))

        var channelTitle = ""
        var channelDescription = ""
        var artworkUrl: String? = null
        val episodes = mutableListOf<ParsedEpisode>()

        var inItem = false
        var inChannelImage = false
        var currentTag = ""
        val text = StringBuilder()

        var itemTitle = ""
        var itemGuid = ""
        var itemDescription: String? = null
        var itemAudioUrl = ""
        var itemDuration: String? = null
        var itemPubDate = 0L

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    text.clear()
                    when (currentTag) {
                        "item" -> inItem = true
                        "image" -> if (!inItem) inChannelImage = true
                        "enclosure" -> if (inItem) {
                            val type = parser.getAttributeValue(null, "type") ?: ""
                            if (type.startsWith("audio")) {
                                itemAudioUrl = parser.getAttributeValue(null, "url") ?: ""
                            }
                        }
                        "itunes:image" -> if (!inItem && artworkUrl == null) {
                            artworkUrl = parser.getAttributeValue(null, "href")
                        }
                    }
                }
                XmlPullParser.TEXT -> text.append(parser.text)
                XmlPullParser.END_TAG -> {
                    val t = text.toString().trim()
                    when (parser.name) {
                        "title" -> when {
                            inItem -> itemTitle = t
                            channelTitle.isEmpty() -> channelTitle = t
                        }
                        "description" -> when {
                            inItem -> itemDescription = t.ifEmpty { null }
                            channelDescription.isEmpty() -> channelDescription = t
                        }
                        "guid" -> if (inItem) itemGuid = t
                        "itunes:duration" -> if (inItem) itemDuration = t.ifEmpty { null }
                        "pubDate" -> if (inItem) itemPubDate = parsePubDate(t)
                        "url" -> if (inChannelImage && artworkUrl == null) artworkUrl = t
                        "image" -> inChannelImage = false
                        "item" -> {
                            if (itemAudioUrl.isNotEmpty()) {
                                episodes.add(ParsedEpisode(
                                    guid = itemGuid.ifEmpty { itemAudioUrl },
                                    title = itemTitle,
                                    description = itemDescription,
                                    audioUrl = itemAudioUrl,
                                    duration = itemDuration,
                                    pubDate = itemPubDate
                                ))
                            }
                            inItem = false
                            itemTitle = ""; itemGuid = ""; itemDescription = null
                            itemAudioUrl = ""; itemDuration = null; itemPubDate = 0L
                        }
                    }
                    currentTag = ""
                    text.clear()
                }
            }
            event = parser.next()
        }

        return ParsedFeed(channelTitle, channelDescription, artworkUrl, episodes)
    }

    private fun parsePubDate(text: String): Long = try {
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH).parse(text)?.time ?: 0L
    } catch (e: Exception) {
        0L
    }
}
