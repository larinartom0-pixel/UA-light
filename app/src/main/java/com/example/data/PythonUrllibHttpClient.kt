package com.example.data

import android.os.Build
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLParameters
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * Lightweight invisible HTTP/1.1 over raw TLS client that replicates Python's
 * `urllib.request` wire behavior (no HTTP/2 ALPN, Accept-Encoding: identity,
 * Connection: close, identical header set) without embedding a 30MB CPython binary.
 */
object PythonUrllibHttpClient {

    data class HttpResult(
        val statusCode: Int,
        val body: String
    )

    fun get(
        urlString: String,
        userAgent: String,
        cookieHeader: String = "",
        maxRedirects: Int = 5
    ): HttpResult? {
        var currentUrl = urlString
        for (hop in 0..maxRedirects) {
            val result = executeSingleRequest(currentUrl, userAgent, cookieHeader) ?: return null
            if (result.first in 300..399 && !result.second.isNullOrBlank()) {
                val location = result.second!!
                currentUrl = if (location.startsWith("http", ignoreCase = true)) {
                    location
                } else {
                    URL(URL(currentUrl), location).toString()
                }
                continue
            }
            return HttpResult(statusCode = result.first, body = result.third)
        }
        return null
    }

    private fun executeSingleRequest(
        urlString: String,
        userAgent: String,
        cookieHeader: String
    ): Triple<Int, String?, String>? {
        val url = URL(urlString)
        val host = url.host
        val isHttps = url.protocol.equals("https", ignoreCase = true)
        val port = if (url.port != -1) url.port else if (isHttps) 443 else 80
        val path = buildString {
            append(if (url.path.isNullOrEmpty()) "/" else url.path)
            if (!url.query.isNullOrEmpty()) {
                append("?").append(url.query)
            }
        }

        var rawSocket: Socket? = null
        return try {
            val baseSocket = Socket()
            rawSocket = baseSocket
            baseSocket.soTimeout = 12_000
            baseSocket.connect(InetSocketAddress(host, port), 12_000)

            val socket: Socket = if (isHttps) {
                val sslSocket = (SSLSocketFactory.getDefault() as SSLSocketFactory)
                    .createSocket(baseSocket, host, port, true) as SSLSocket
                val params: SSLParameters = sslSocket.sslParameters
                params.serverNames = listOf(SNIHostName(host))
                // Match Python ssl default: HTTP/1.1 only (no h2 ALPN)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    params.applicationProtocols = arrayOf("http/1.1")
                }
                sslSocket.sslParameters = params
                sslSocket.startHandshake()
                sslSocket
            } else {
                baseSocket
            }

            rawSocket = socket

            val requestBytes = buildString {
                append("GET ").append(path).append(" HTTP/1.1\r\n")
                append("Accept-Encoding: identity\r\n")
                append("Host: ").append(host).append("\r\n")
                append("User-Agent: ").append(userAgent).append("\r\n")
                append("Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8\r\n")
                append("Accept-Language: uk-UA,uk;q=0.9,en;q=0.8\r\n")
                if (cookieHeader.isNotBlank()) {
                    append("Cookie: ").append(cookieHeader).append("\r\n")
                }
                append("Connection: close\r\n\r\n")
            }.toByteArray(Charsets.ISO_8859_1)

            val out = socket.getOutputStream()
            out.write(requestBytes)
            out.flush()

            val input = socket.getInputStream()
            parseHttpResponse(input)
        } catch (_: Exception) {
            null
        } finally {
            try {
                rawSocket?.close()
            } catch (_: Exception) {
            }
        }
    }

    internal fun parseHttpResponse(input: InputStream): Triple<Int, String?, String>? {
        val statusLine = readAsciiLine(input) ?: return null
        val statusParts = statusLine.split(" ")
        val statusCode = statusParts.getOrNull(1)?.toIntOrNull() ?: return null

        val headers = mutableMapOf<String, String>()
        while (true) {
            val line = readAsciiLine(input) ?: break
            if (line.isEmpty()) break
            val colonIdx = line.indexOf(':')
            if (colonIdx > 0) {
                val name = line.substring(0, colonIdx).trim().lowercase()
                val value = line.substring(colonIdx + 1).trim()
                headers[name] = value
            }
        }

        val location = headers["location"]
        val transferEncoding = headers["transfer-encoding"]?.lowercase().orEmpty()
        val contentLength = headers["content-length"]?.toIntOrNull()

        val bodyBytes = when {
            transferEncoding.contains("chunked") -> readChunkedBody(input)
            contentLength != null && contentLength >= 0 -> readFixedLength(input, contentLength)
            else -> input.readBytes()
        }

        return Triple(statusCode, location, bodyBytes.toString(Charsets.UTF_8))
    }

    private fun readAsciiLine(input: InputStream): String? {
        val baos = ByteArrayOutputStream()
        var prev = -1
        while (true) {
            val b = input.read()
            if (b == -1) {
                return if (baos.size() == 0) null else baos.toString(Charsets.ISO_8859_1.name())
            }
            if (b == '\n'.code) {
                val bytes = baos.toByteArray()
                val len = if (prev == '\r'.code && bytes.isNotEmpty()) bytes.size - 1 else bytes.size
                return String(bytes, 0, len, Charsets.ISO_8859_1)
            }
            baos.write(b)
            prev = b
        }
    }

    private fun readFixedLength(input: InputStream, length: Int): ByteArray {
        val baos = ByteArrayOutputStream(length)
        val buffer = ByteArray(4096)
        var remaining = length
        while (remaining > 0) {
            val toRead = minOf(buffer.size, remaining)
            val read = input.read(buffer, 0, toRead)
            if (read == -1) break
            baos.write(buffer, 0, read)
            remaining -= read
        }
        return baos.toByteArray()
    }

    private fun readChunkedBody(input: InputStream): ByteArray {
        val baos = ByteArrayOutputStream()
        while (true) {
            val sizeLine = readAsciiLine(input) ?: break
            val hexSize = sizeLine.substringBefore(';').trim()
            val chunkSize = hexSize.toIntOrNull(16) ?: break
            if (chunkSize == 0) break

            val chunkData = readFixedLength(input, chunkSize)
            baos.write(chunkData)
            // Consume trailing CRLF after chunk
            readAsciiLine(input)
        }
        return baos.toByteArray()
    }
}
