package com.example.data

import android.content.Context
import android.webkit.CookieManager
import com.example.model.ScheduleData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class PowerScheduleRepository(context: Context) {

    private val appContext = context.applicationContext
    private val preferencesManager = PreferencesManager(appContext)
    private val memoryCache = ConcurrentHashMap<String, ScheduleData>()

    @Volatile
    private var activeUserAgent: String =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"

    private val client: OkHttpClient = OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_1_1))
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun getSchedule(
        queueCode: String,
        allowWebViewFallback: Boolean = true
    ): ScheduleData = withContext(Dispatchers.IO) {
        val slug = queueCode.replace(".", "-")
        val url = "https://energy-ua.info/cherga/$slug"

        val cookieHeader = try {
            CookieManager.getInstance().getCookie(url).orEmpty()
        } catch (_: Exception) {
            ""
        }

        var lastHttpCode = 0

        // 1. Primary: Python urllib-equivalent raw HTTP/1.1 TLS request (identical wire headers/ALPN)
        val pythonLikeResult = PythonUrllibHttpClient.get(
            urlString = url,
            userAgent = activeUserAgent,
            cookieHeader = cookieHeader
        )
        if (pythonLikeResult != null) {
            lastHttpCode = pythonLikeResult.statusCode
            if (!isCloudflareBlocked(pythonLikeResult.statusCode, pythonLikeResult.body)) {
                val parsed = PowerScheduleParser.parseHtml(queueCode, pythonLikeResult.body)
                if (parsed != null) {
                    memoryCache[queueCode] = parsed
                    preferencesManager.saveCachedSchedule(queueCode, parsed)
                    return@withContext parsed
                }
            }
        }

        val requestBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", activeUserAgent)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "uk-UA,uk;q=0.9,en;q=0.8")

        if (cookieHeader.isNotBlank()) {
            requestBuilder.header("Cookie", cookieHeader)
        }

        try {
            // 2. Secondary: OkHttp (HTTP/1.1)
            val response = client.newCall(requestBuilder.build()).execute()
            lastHttpCode = response.code
            val body = response.body?.string().orEmpty()

            if (!isCloudflareBlocked(response.code, body)) {
                val parsed = PowerScheduleParser.parseHtml(queueCode, body)
                if (parsed != null) {
                    memoryCache[queueCode] = parsed
                    preferencesManager.saveCachedSchedule(queueCode, parsed)
                    return@withContext parsed
                }
            }

            // 3. Tertiary: Native Android HttpURLConnection
            val nativeResult = fetchViaHttpUrlConnection(url, cookieHeader)
            if (nativeResult != null) {
                lastHttpCode = nativeResult.first
                val nativeBody = nativeResult.second
                if (!isCloudflareBlocked(nativeResult.first, nativeBody)) {
                    val parsed = PowerScheduleParser.parseHtml(queueCode, nativeBody)
                    if (parsed != null) {
                        memoryCache[queueCode] = parsed
                        preferencesManager.saveCachedSchedule(queueCode, parsed)
                        return@withContext parsed
                    }
                }
            }

            // 4. Quaternary: Real Chromium WebView fallback (skipped on headless cloud emulators)
            if (allowWebViewFallback) {
                val webResult = WebViewScheduleFetcher.fetchPayload(appContext, url)
                if (webResult != null) {
                    if (webResult.userAgent.isNotBlank()) {
                        activeUserAgent = webResult.userAgent
                    }
                    val parsedFromWeb = PowerScheduleParser.parsePayloadJson(queueCode, webResult.payloadJson)
                    if (parsedFromWeb != null) {
                        memoryCache[queueCode] = parsedFromWeb
                        preferencesManager.saveCachedSchedule(queueCode, parsedFromWeb)
                        return@withContext parsedFromWeb
                    }
                }
            }

            if (lastHttpCode == 403 || lastHttpCode == 503 || lastHttpCode == 429) {
                return@withContext fallbackFromCache(
                    queueCode = queueCode,
                    reason = "Захист сайту Cloudflare (код $lastHttpCode)"
                )
            }

            val emptyLive = ScheduleData(
                queueCode = queueCode,
                primaryIntervals = emptyList(),
                secondaryIntervals = emptyList(),
                lastUpdated = System.currentTimeMillis(),
                isLiveFromNetwork = true,
                note = "Дані графіка на сторінці відсутні"
            )
            memoryCache[queueCode] = emptyLive
            emptyLive
        } catch (_: Exception) {
            if (allowWebViewFallback) {
                val webResult = WebViewScheduleFetcher.fetchPayload(appContext, url)
                if (webResult != null) {
                    if (webResult.userAgent.isNotBlank()) {
                        activeUserAgent = webResult.userAgent
                    }
                    val parsedFromWeb = PowerScheduleParser.parsePayloadJson(queueCode, webResult.payloadJson)
                    if (parsedFromWeb != null) {
                        memoryCache[queueCode] = parsedFromWeb
                        preferencesManager.saveCachedSchedule(queueCode, parsedFromWeb)
                        return@withContext parsedFromWeb
                    }
                }
            }
            fallbackFromCache(
                queueCode = queueCode,
                reason = "Немає зв'язку із сервером"
            )
        }
    }

    private fun isCloudflareBlocked(code: Int, body: String): Boolean {
        return code == 403 ||
            code == 503 ||
            code == 429 ||
            body.contains("Just a moment", ignoreCase = true) ||
            body.contains("cf_chl_opt", ignoreCase = true)
    }

    private fun fetchViaHttpUrlConnection(urlStr: String, cookieHeader: String): Pair<Int, String>? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12_000
                readTimeout = 12_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", activeUserAgent)
                setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                setRequestProperty("Accept-Language", "uk-UA,uk;q=0.9,en;q=0.8")
                if (cookieHeader.isNotBlank()) {
                    setRequestProperty("Cookie", cookieHeader)
                }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            Pair(code, body)
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun fallbackFromCache(queueCode: String, reason: String): ScheduleData {
        val cached = getCached(queueCode)
        return if (cached != null) {
            cached.copy(
                isLiveFromNetwork = false,
                note = "$reason • Показано збережений графік"
            )
        } else {
            ScheduleData(
                queueCode = queueCode,
                primaryIntervals = emptyList(),
                secondaryIntervals = emptyList(),
                lastUpdated = System.currentTimeMillis(),
                isLiveFromNetwork = false,
                note = reason
            )
        }
    }

    fun getCached(queueCode: String): ScheduleData? {
        memoryCache[queueCode]?.let { return it }
        val disk = preferencesManager.getCachedSchedule(queueCode)
        if (disk != null) {
            memoryCache[queueCode] = disk
        }
        return disk
    }

    fun isFreshInMemory(queueCode: String, maxAgeMs: Long = 3 * 60 * 1000L): Boolean {
        val cached = memoryCache[queueCode] ?: return false
        return cached.isLiveFromNetwork && (System.currentTimeMillis() - cached.lastUpdated) < maxAgeMs
    }

    fun getSelectedQueue(): String = preferencesManager.getSelectedQueue()

    fun setSelectedQueue(queueCode: String) = preferencesManager.setSelectedQueue(queueCode)

    fun getSelectedScheduleType(): String = preferencesManager.getSelectedScheduleType()

    fun setSelectedScheduleType(type: String) = preferencesManager.setSelectedScheduleType(type)
}
