package com.fuzinewatch.scraper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParsersTest {

    @Test
    fun nepremicnineHappyPath() {
        val html = """
            <html><body>
              <div class="property-details">
                <a class="url-title-d" href="/oglasi/foo/"><h2>Fužine, 2-sobno</h2></a>
                <h6>850 EUR/mesec</h6>
                <ul><li>55,0 m2</li><li>2 sobi</li></ul>
              </div>
            </body></html>
        """.trimIndent()

        val result = Parsers.parseNepremicnine(html)

        assertEquals(1, result.rawCount)
        assertEquals(1, result.listings.size)
        assertEquals("Fužine, 2-sobno", result.listings.single().title)
        assertEquals("850 EUR/mesec", result.listings.single().price)
        assertEquals("55,0 m2", result.listings.single().size)
        assertEquals("https://www.nepremicnine.net/oglasi/foo/", result.listings.single().link)
    }

    @Test
    fun bolhaHappyPathFiltersFuzineAndExtractsSize() {
        val html = """
            <html><body>
              <li class="EntityList-item">
                <a class="link" href="/oglas/stanovanje">Stanovanje Ljubljana</a>
                <strong class="price">700 EUR</strong>
                <div class="entity-description">Lokacija: Fužine<br>površina: 42 m2</div>
              </li>
              <li class="EntityList-item">
                <a class="link" href="/oglas/drugo">Moste</a>
                <strong class="price">800 EUR</strong>
                <div class="entity-description">Lokacija: Moste<br>površina: 50 m2</div>
              </li>
            </body></html>
        """.trimIndent()

        val result = Parsers.parseBolha(html)

        assertEquals(2, result.rawCount)
        assertEquals(1, result.listings.size)
        assertEquals("42 m2", result.listings.single().size)
        assertEquals("https://www.bolha.com/oglas/stanovanje", result.listings.single().link)
    }

    @Test
    fun zeroMatchesReturnsEmptyResult() {
        val result = Parsers.parseNepremicnine("<html><body>No listings</body></html>")

        assertEquals(0, result.rawCount)
        assertTrue(result.listings.isEmpty())
    }

    @Test
    fun missingFieldsDoNotCrash() {
        val html = """
            <li class="EntityList-item">
              <a class="link" href="/oglas/brez">Fužine garsonjera</a>
              <div class="entity-description">Lokacija: Fužine</div>
            </li>
        """.trimIndent()

        val listing = Parsers.parseBolha(html).listings.single()

        assertEquals("Fužine garsonjera", listing.title)
        assertEquals("", listing.price)
        assertEquals("", listing.size)
    }
}
