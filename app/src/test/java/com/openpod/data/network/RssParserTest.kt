package com.openpod.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class RssParserTest {

    private val parser = RssParser()

    // region helpers

    private fun feed(
        title: String = "Test Podcast",
        description: String = "A description",
        artworkTag: String = "",
        items: String = ""
    ) = "<rss version=\"2.0\" xmlns:itunes=\"http://www.itunes.com/dtds/podcast-1.0.dtd\">" +
        "<channel><title>$title</title><description>$description</description>" +
        "$artworkTag$items</channel></rss>"

    private fun item(
        title: String = "Episode 1",
        guid: String = "ep-1",
        description: String = "Ep description",
        audioUrl: String = "https://example.com/ep1.mp3",
        duration: String = "1:23:45",
        pubDate: String = "Mon, 01 Jan 2024 00:00:00 +0000"
    ) = "<item><title>$title</title><guid>$guid</guid><description>$description</description>" +
        "<enclosure url=\"$audioUrl\" type=\"audio/mpeg\" length=\"0\"/>" +
        "<itunes:duration>$duration</itunes:duration><pubDate>$pubDate</pubDate></item>"

    // endregion

    @Test
    fun `parses channel title and description`() {
        val result = parser.parse(feed(title = "My Show", description = "About my show"))
        assertEquals("My Show", result.title)
        assertEquals("About my show", result.description)
    }

    @Test
    fun `parses itunes image artwork`() {
        val xml = feed(artworkTag = """<itunes:image href="https://example.com/art.jpg"/>""")
        assertEquals("https://example.com/art.jpg", parser.parse(xml).artworkUrl)
    }

    @Test
    fun `parses image url artwork as fallback`() {
        val xml = feed(artworkTag = """<image><url>https://example.com/art.jpg</url></image>""")
        assertEquals("https://example.com/art.jpg", parser.parse(xml).artworkUrl)
    }

    @Test
    fun `itunes image takes priority over image url`() {
        val xml = feed(artworkTag = """
            <itunes:image href="https://example.com/itunes.jpg"/>
            <image><url>https://example.com/rss.jpg</url></image>
        """)
        assertEquals("https://example.com/itunes.jpg", parser.parse(xml).artworkUrl)
    }

    @Test
    fun `artwork is null when absent`() {
        assertNull(parser.parse(feed()).artworkUrl)
    }

    @Test
    fun `parses episode fields`() {
        val xml = feed(items = item(
            title = "Deep Dive",
            guid = "deep-dive-1",
            description = "A deep dive episode",
            audioUrl = "https://example.com/deep.mp3",
            duration = "45:00",
            pubDate = "Mon, 01 Jan 2024 12:00:00 +0000"
        ))
        val ep = parser.parse(xml).episodes.single()
        assertEquals("deep-dive-1", ep.guid)
        assertEquals("Deep Dive", ep.title)
        assertEquals("A deep dive episode", ep.description)
        assertEquals("https://example.com/deep.mp3", ep.audioUrl)
        assertEquals("45:00", ep.duration)
        val expected = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
            .parse("Mon, 01 Jan 2024 12:00:00 +0000")!!.time
        assertEquals(expected, ep.pubDate)
    }

    @Test
    fun `guid falls back to audio url when absent`() {
        val xml = feed(items = "<item><title>No Guid</title>" +
            "<enclosure url=\"https://example.com/ep.mp3\" type=\"audio/mpeg\" length=\"0\"/></item>")
        assertEquals("https://example.com/ep.mp3", parser.parse(xml).episodes.single().guid)
    }

    @Test
    fun `skips items without audio enclosure`() {
        val xml = feed(items = "<item><title>Blog Post</title><guid>blog-1</guid>" +
            "<enclosure url=\"https://example.com/image.jpg\" type=\"image/jpeg\" length=\"0\"/></item>")
        assertEquals(0, parser.parse(xml).episodes.size)
    }

    @Test
    fun `parses multiple episodes in order`() {
        val xml = feed(items = item(title = "Ep 1", guid = "ep-1") + "\n" + item(title = "Ep 2", guid = "ep-2"))
        val episodes = parser.parse(xml).episodes
        assertEquals(2, episodes.size)
        assertEquals("Ep 1", episodes[0].title)
        assertEquals("Ep 2", episodes[1].title)
    }

    @Test
    fun `invalid pubDate returns zero`() {
        val xml = feed(items = "<item><title>Bad Date</title><guid>bad-date</guid>" +
            "<enclosure url=\"https://example.com/ep.mp3\" type=\"audio/mpeg\" length=\"0\"/>" +
            "<pubDate>not a real date</pubDate></item>")
        assertEquals(0L, parser.parse(xml).episodes.single().pubDate)
    }

    @Test
    fun `empty description is null`() {
        val xml = feed(items = "<item><title>No Desc</title><guid>no-desc</guid>" +
            "<enclosure url=\"https://example.com/ep.mp3\" type=\"audio/mpeg\" length=\"0\"/>" +
            "<description></description></item>")
        assertNull(parser.parse(xml).episodes.single().description)
    }
}
