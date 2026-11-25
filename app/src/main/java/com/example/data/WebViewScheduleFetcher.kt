package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class WebViewFetchResult(
    val payloadJson: String,
    val userAgent: String,
    val cookies: String
)

object WebViewScheduleFetcher {

    private val mutex = Mutex()
    private val stringAdapter = Moshi.Builder().build().adapter(String::class.java)

    @Volatile
    private var webViewDisabledForSession = false

    private fun isHeadlessCloudEmulator(): Boolean {
        val hw = Build.HARDWARE.lowercase()
        val board = Build.BOARD.lowercase()
        val product = Build.PRODUCT.lowercase()
        val model = Build.MODEL.lowercase()
        return hw.contains("cutf") ||
            board.contains("vsoc") ||
            product.contains("cuttlefish") ||
            model.contains("cuttlefish")
    }

    suspend fun fetchPayload(context: Context, url: String): WebViewFetchResult? {
        if (webViewDisabledForSession || isHeadlessCloudEmulator()) {
            return null
        }
        return mutex.withLock {
            if (webViewDisabledForSession) return@withLock null
            withTimeoutOrNull(12_000L) {
                withContext(Dispatchers.Main) {
                    fetchOnMainThread(context.applicationContext, url)
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun fetchOnMainThread(context: Context, url: String): WebViewFetchResult? =
        suspendCancellableCoroutine { cont ->
            val handler = Handler(Looper.getMainLooper())
            var finished = false
            var webView: WebView? = null
            var pollRunnable: Runnable? = null

            fun cleanup() {
                pollRunnable?.let { handler.removeCallbacks(it) }
                try {
                    webView?.stopLoading()
                    webView?.destroy()
                } catch (_: Exception) {
                }
                webView = null
            }

            fun complete(result: WebViewFetchResult?) {
                if (finished) return
                finished = true
                cleanup()
                if (cont.isActive) {
                    cont.resume(result)
                }
            }

            cont.invokeOnCancellation {
                handler.post {
                    finished = true
                    cleanup()
                }
            }

            try {
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)

                val wv = WebView(context)
                webView = wv
                wv.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                cookieManager.setAcceptThirdPartyCookies(wv, true)

                val settings = wv.settings
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadsImagesAutomatically = false
                settings.blockNetworkImage = true
                settings.cacheMode = WebSettings.LOAD_NO_CACHE
                val ua = settings.userAgentString

                fun checkPayload() {
                    if (finished) return
                    wv.evaluateJavascript(
                        "(function(){var el=document.getElementById('schedulePayload');return el?el.textContent:null;})()"
                    ) { rawValue ->
                        if (finished) return@evaluateJavascript
                        if (!rawValue.isNullOrBlank() && rawValue != "null") {
                            val decoded = try {
                                stringAdapter.fromJson(rawValue)
                            } catch (_: Exception) {
                                null
                            }
                            if (!decoded.isNullOrBlank() && decoded.trimStart().startsWith("{")) {
                                try {
                                    cookieManager.flush()
                                } catch (_: Exception) {
                                }
                                val cookies = cookieManager.getCookie(url).orEmpty()
                                complete(
                                    WebViewFetchResult(
                                        payloadJson = decoded,
                                        userAgent = ua,
                                        cookies = cookies
                                    )
                                )
                            }
                        }
                    }
                }

                wv.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean = false

                    override fun onPageFinished(view: WebView?, loadedUrl: String?) {
                        super.onPageFinished(view, loadedUrl)
                        checkPayload()
                    }

                    override fun onRenderProcessGone(
                        view: WebView?,
                        detail: RenderProcessGoneDetail?
                    ): Boolean {
                        webViewDisabledForSession = true
                        complete(null)
                        return true
                    }
                }

                pollRunnable = object : Runnable {
                    override fun run() {
                        if (finished) return
                        checkPayload()
                        handler.postDelayed(this, 500L)
                    }
                }
                handler.postDelayed(pollRunnable!!, 800L)

                wv.loadUrl(url)
            } catch (_: Throwable) {
                webViewDisabledForSession = true
                complete(null)
            }
        }
}
