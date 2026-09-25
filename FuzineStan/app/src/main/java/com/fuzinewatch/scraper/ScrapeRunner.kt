package com.fuzinewatch.scraper

import android.content.Context
import android.util.Log
import com.fuzinewatch.data.AppDatabase
import com.fuzinewatch.data.Listing
import com.fuzinewatch.data.ScrapeStatusStore
import com.fuzinewatch.data.listingId
import com.fuzinewatch.notif.Notifier

object ScrapeRunner {
    const val TAG = "FuzineWatch"

    suspend fun scrapeAll(context: Context, sendNotifications: Boolean = true) {
        val appContext = context.applicationContext
        val dao = AppDatabase.get(appContext).listingDao()
        val statusStore = ScrapeStatusStore.get(appContext)
        val fetcher = WebViewFetcher(appContext)

        Site.entries.forEach { site ->
            runCatching {
                when (val fetched = fetcher.fetch(site.url)) {
                    is FetchResult.Error -> {
                        log(site, "error=${fetched.message}", 0, 0, 0)
                        statusStore.update(site, "Error: ${fetched.message}")
                    }
                    is FetchResult.Success -> {
                        val wall = Parsers.detectBotWall(fetched.html)
                        if (wall != BotWall.NONE) {
                            val msg = "Bot wall: $wall"
                            log(site, msg, 0, 0, 0)
                            statusStore.update(site, msg)
                            return@runCatching
                        }

                        val parsed = Parsers.parse(site, fetched.html)
                        val seenIds = dao.idsBySource(site.label).toSet()
                        val firstRunForSite = !statusStore.hasSeeded(site)
                        val now = System.currentTimeMillis()
                        val listings = parsed.listings.map {
                            Listing(
                                id = listingId(it.source, it.link),
                                source = it.source,
                                title = it.title,
                                price = it.price,
                                size = it.size,
                                link = it.link,
                                firstSeen = now,
                            )
                        }
                        val newListings = listings.filterNot { it.id in seenIds }
                        dao.insertAll(newListings)
                        statusStore.markSeeded(site)
                        if (!firstRunForSite && sendNotifications) {
                            newListings.forEach { Notifier.notifyNewListing(appContext, it) }
                        }

                        val outcome = if (firstRunForSite) "seeded" else "success"
                        log(site, outcome, parsed.rawCount, parsed.listings.size, newListings.size)
                        statusStore.update(
                            site,
                            "$outcome: ${parsed.listings.size} parsed, ${newListings.size} new",
                        )
                    }
                }
            }.onFailure { e ->
                val message = e.message ?: e.javaClass.simpleName
                log(site, "exception=$message", 0, 0, 0)
                statusStore.update(site, "Error: $message")
            }
        }
        statusStore.markRun()
    }

    private fun log(site: Site, outcome: String, raw: Int, parsed: Int, new: Int) {
        Log.i(
            TAG,
            "site=${site.label} outcome=$outcome rawMatches=$raw parsed=$parsed new=$new",
        )
    }
}
