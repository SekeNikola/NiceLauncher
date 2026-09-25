package com.fuzinewatch.scraper

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

enum class Site(val label: String, val url: String, val baseUrl: String) {
    NEPREMICNINE(
        label = "nepremicnine.net",
        url = "https://www.nepremicnine.net/oglasi-prodaja/ljubljana-mesto/fuzine/stanovanje/",
        baseUrl = "https://www.nepremicnine.net",
    ),
    BOLHA(
        label = "bolha.com",
        url = "https://www.bolha.com/prodaja-stanovanja/ljubljana",
        baseUrl = "https://www.bolha.com",
    ),
}

data class ParsedListing(
    val source: String,
    val title: String,
    val price: String,
    val size: String,
    val link: String,
)

/** [rawCount] = how many elements matched the listing selector, before any filtering/validation. */
data class ParseResult(val rawCount: Int, val listings: List<ParsedListing>)

enum class BotWall { NONE, CLOUDFLARE, RADWARE }

object Parsers {

    fun detectBotWall(html: String): BotWall = titleToBotWall(Jsoup.parse(html).title())

    fun titleToBotWall(title: String?): BotWall = when {
        title == null -> BotWall.NONE
        title.contains("Just a moment", ignoreCase = true) -> BotWall.CLOUDFLARE
        title.contains("Radware", ignoreCase = true) -> BotWall.RADWARE
        else -> BotWall.NONE
    }

    fun parse(site: Site, html: String): ParseResult = when (site) {
        Site.NEPREMICNINE -> parseNepremicnine(html)
        Site.BOLHA -> parseBolha(html)
    }

    fun parseNepremicnine(html: String): ParseResult {
        val cards = Jsoup.parse(html).select("div.property-details")
        val listings = cards.mapNotNull { card ->
            val href = card.selectFirst("a.url-title-d")?.attr("href")?.trim().orEmpty()
            if (href.isEmpty()) return@mapNotNull null
            ParsedListing(
                source = Site.NEPREMICNINE.label,
                title = card.selectFirst("h2")?.text()?.trim().orEmpty().ifEmpty { "(brez naslova)" },
                price = card.selectFirst("h6")?.text()?.trim().orEmpty(),
                size = card.selectFirst("ul li")?.text()?.trim().orEmpty(),
                link = absolutize(href, Site.NEPREMICNINE.baseUrl),
            )
        }
        return ParseResult(cards.size, listings)
    }

    /** Bolha has no district filter in the URL, so keep only listings mentioning Fužine. */
    fun parseBolha(html: String): ParseResult {
        val items = Jsoup.parse(html).select("li.EntityList-item")
        val listings = items.mapNotNull { item ->
            val anchor = item.selectFirst("a.link") ?: return@mapNotNull null
            val href = anchor.attr("href").trim()
            if (href.isEmpty()) return@mapNotNull null
            val descText = item.selectFirst("div.entity-description")?.let { textWithLineBreaks(it) }.orEmpty()
            if (!normalize(anchor.text() + " " + descText).contains("fuzine")) return@mapNotNull null
            ParsedListing(
                source = Site.BOLHA.label,
                title = anchor.text().trim().ifEmpty { "(brez naslova)" },
                price = item.selectFirst("strong.price")?.text()?.trim().orEmpty(),
                size = extractSize(descText),
                link = absolutize(href, Site.BOLHA.baseUrl),
            )
        }
        return ParseResult(items.size, listings)
    }

    /** Lowercase and strip Slovenian diacritics so "Fužine"/"FUŽINE"/"fuzine" all match. */
    fun normalize(s: String): String = s.lowercase()
        .replace('ž', 'z')
        .replace('š', 's')
        .replace('č', 'c')

    private val sizeRegex = Regex("""(?i)povr[šs]ina:\s*([^\n]+)""")

    /** Size is the text after "površina:" up to end of line in the description block. */
    fun extractSize(desc: String): String =
        sizeRegex.find(desc)?.groupValues?.get(1)?.trim().orEmpty()

    fun absolutize(href: String, base: String): String =
        if (href.startsWith("http")) href
        else base + (if (href.startsWith("/")) href else "/$href")

    /** jsoup's text() collapses newlines; keep <br>-separated lines so "up to end of line" works. */
    private fun textWithLineBreaks(el: Element): String {
        val htmlWithNewlines = el.html().replace(Regex("(?i)<br[^>]*>"), "\n")
        return Jsoup.parse(htmlWithNewlines).wholeText().trim()
    }
}
