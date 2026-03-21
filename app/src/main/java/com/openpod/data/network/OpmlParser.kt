package com.openpod.data.network

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

object OpmlParser {
    fun parse(input: InputStream): List<String> {
        val feedUrls = mutableListOf<String>()
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(input, null)
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "outline") {
                val xmlUrl = parser.getAttributeValue(null, "xmlUrl")
                if (!xmlUrl.isNullOrBlank()) feedUrls.add(xmlUrl)
            }
            event = parser.next()
        }
        return feedUrls
    }
}
