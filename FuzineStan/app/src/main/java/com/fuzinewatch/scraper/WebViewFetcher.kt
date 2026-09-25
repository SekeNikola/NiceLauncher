package com.fuzinewatch.scraper

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONTokener
import kotlin.coroutines.resume

sealed class FetchResult {
    data class Success(val html: String) : FetchResult()
    data class Error(val message: String) : FetchResult()
}

/**
 * Fetches a page through a hidden WebView so bot-protection JS (Cloudflare managed
 * challenge, Radware) runs in a real browser engine. Cookies persist app-wide via
 * CookieManager, so a challenge cleared once (here or on the visible Unblock screen)
 * is skipped on later fetches.
 */
class WebViewFetcher(private val context: Context) {

    companion object {
        private const val TAG = "FuzineWatch"
        private const val CHALLENGE_POLL_MS = 2_000L
        private const val CHALLENGE_MAX_WAIT_MS = 30_000L
        private const val SETTLE_DELAY_MS = 1_500L

        /** Cloudflare treats the "; wv" WebView marker as a bot signal — present as plain Chrome. */
        fun cleanUserAgent(ua: String): String = ua
            .replace("; wv", "")
            .replace(Regex("""Version/\d+\.\d+\s"""), "")

        /** Shared by the hidden fetcher and the visible Unblock screen so cookies match the UA. */
        fun applySettings(webView: WebView) {
            webView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                userAgentString = cleanUserAgent(userAgentString)
            }
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        }
    }

    suspend fun fetch(url: String, timeoutMs: Long = 90_000L): FetchResult = withContext(Dispatchers.Main) {
        var webView: WebView? = null
        try {
            withTimeout(timeoutMs) {
                val pageLoaded = CompletableDeferred<String?>()
                val wv = WebView(context.applicationContext)
                webView = wv
                applySettings(wv)
                wv.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                        pageLoaded.complete(null)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?,
                    ) {
                        if (request?.isForMainFrame == true) {
                            pageLoaded.complete("code=${error?.errorCode} ${error?.description}")
                        }
                    }
                }
                Log.i(TAG, "WebView loading $url")
                wv.loadUrl(url)

                val loadError = pageLoaded.await()
                if (loadError != null) return@withTimeout FetchResult.Error("Load failed: $loadError")
                delay(SETTLE_DELAY_MS)

                var waited = 0L
                while (waited < CHALLENGE_MAX_WAIT_MS) {
                    val title = evalJs(wv, "document.title")
                    if (Parsers.titleToBotWall(title) == BotWall.NONE) break
                    Log.i(TAG, "Challenge page ('$title'), waiting ${CHALLENGE_POLL_MS}ms...")
                    delay(CHALLENGE_POLL_MS)
                    waited += CHALLENGE_POLL_MS
                }
                delay(SETTLE_DELAY_MS)

                val html = evalJs(wv, "document.documentElement.outerHTML")
                CookieManager.getInstance().flush()
                if (html.isNullOrBlank()) FetchResult.Error("Empty HTML after load")
                else FetchResult.Success(html)
            }
        } catch (e: TimeoutCancellationException) {
            FetchResult.Error("Timed out after ${timeoutMs / 1000}s")
        } catch (e: Exception) {
            FetchResult.Error(e.message ?: e.javaClass.simpleName)
        } finally {
            webView?.apply {
                stopLoading()
                destroy()
            }
        }
    }

    private suspend fun evalJs(webView: WebView, script: String): String? =
        suspendCancellableCoroutine { cont ->
            webView.evaluateJavascript(script) { value ->
                if (cont.isActive) cont.resume(decodeJsResult(value))
            }
        }

    /** evaluateJavascript returns a JSON-encoded value; decode to the raw string. */
    private fun decodeJsResult(value: String?): String? {
        if (value == null || value == "null") return null
        return try {
            JSONTokener(value).nextValue() as? String
        } catch (e: Exception) {
            null
        }
    }
}
