package org.iosclone.keyboard.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object AppUpdateChecker {

    private const val GITHUB_LATEST_RELEASE_URL =
        "https://api.github.com/repos/qxmcu/ios-keyboard-android/releases/latest"

    data class UpdateInfo(
        val hasUpdate: Boolean,
        val latestVersion: String,
        val releaseNotes: String,
        val apkDownloadUrl: String?
    )

    fun check(
        currentVersion: String,
        onResult: (UpdateInfo) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(GITHUB_LATEST_RELEASE_URL)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 15000
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    setRequestProperty("User-Agent", "iOS-Keyboard-Android")
                }

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val tagName = json.optString("tag_name", "").trim()
                    val cleanTag = tagName.removePrefix("v").trim()
                    val body = json.optString("body", "Bug fixes and performance enhancements.")

                    var downloadUrl: String? = null
                    val assets = json.optJSONArray("assets")
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.optString("name", "")
                            if (name.endsWith(".apk", ignoreCase = true) && !name.contains("debug", ignoreCase = true)) {
                                downloadUrl = asset.optString("browser_download_url", null)
                                break
                            } else if (name.endsWith(".apk", ignoreCase = true) && downloadUrl == null) {
                                downloadUrl = asset.optString("browser_download_url", null)
                            }
                        }
                    }

                    val isNewer = compareVersions(cleanTag, currentVersion) > 0
                    withContext(Dispatchers.Main) {
                        onResult(
                            UpdateInfo(
                                hasUpdate = isNewer,
                                latestVersion = if (tagName.isNotEmpty()) tagName else "v$cleanTag",
                                releaseNotes = body,
                                apkDownloadUrl = downloadUrl
                            )
                        )
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onError("Server returned response code ${conn.responseCode}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Failed to connect to update server")
                }
            }
        }
    }

    private fun openConnectionWithRedirects(initialUrl: String, rangeBytes: Long = 0L): Pair<HttpURLConnection, Long> {
        var currentUrl = initialUrl
        var redirects = 0
        while (redirects < 6) {
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 30000
                readTimeout = 60000
                setRequestProperty("User-Agent", "iOS-Keyboard-Android")
                setRequestProperty("Accept-Encoding", "identity")
                if (rangeBytes > 0L) {
                    setRequestProperty("Range", "bytes=$rangeBytes-")
                }
            }
            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_MOVED_PERM ||
                code == HttpURLConnection.HTTP_MOVED_TEMP ||
                code == 307 || code == 308) {
                val loc = conn.getHeaderField("Location") ?: throw IOException("HTTP redirect without Location header")
                conn.disconnect()
                currentUrl = loc
                redirects++
                continue
            }
            val totalLength = if (code == HttpURLConnection.HTTP_PARTIAL) {
                val contentRange = conn.getHeaderField("Content-Range")
                val totalFromRange = contentRange?.substringAfterLast('/')?.toLongOrNull()
                totalFromRange ?: (rangeBytes + conn.contentLengthLong)
            } else {
                conn.contentLengthLong
            }
            return Pair(conn, totalLength)
        }
        throw IOException("Too many redirects: $redirects")
    }

    fun downloadAndInstall(
        context: Context,
        downloadUrl: String,
        onProgress: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val updateDir = File(context.getExternalFilesDir(null), "updates").apply { mkdirs() }
                val targetFile = File(updateDir, "iOSKeyboard-update.apk")

                var totalExpectedLength = -1L
                var lastReportedProgress = -1
                var attempts = 0
                val maxAttempts = 5
                var downloadSuccess = false
                var lastException: Exception? = null

                while (attempts < maxAttempts && !downloadSuccess) {
                    attempts++
                    var conn: HttpURLConnection? = null
                    try {
                        val existingBytes = if (targetFile.exists()) targetFile.length() else 0L
                        if (totalExpectedLength > 0L && existingBytes >= totalExpectedLength) {
                            downloadSuccess = true
                            break
                        }

                        val (activeConn, totalLength) = openConnectionWithRedirects(downloadUrl, existingBytes)
                        conn = activeConn
                        if (totalLength > 0L) {
                            totalExpectedLength = totalLength
                        }

                        val isAppend = conn.responseCode == HttpURLConnection.HTTP_PARTIAL && existingBytes > 0L
                        if (!isAppend && targetFile.exists()) {
                            targetFile.delete()
                        }

                        val initialBytes = if (isAppend) existingBytes else 0L
                        var totalRead = initialBytes

                        conn.inputStream.use { input ->
                            FileOutputStream(targetFile, isAppend).use { output ->
                                val buffer = ByteArray(64 * 1024)
                                var bytesRead: Int

                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    output.write(buffer, 0, bytesRead)
                                    totalRead += bytesRead
                                    if (totalExpectedLength > 0L) {
                                        val progress = ((totalRead * 100) / totalExpectedLength).toInt().coerceIn(0, 99)
                                        if (progress != lastReportedProgress) {
                                            lastReportedProgress = progress
                                            withContext(Dispatchers.Main) {
                                                onProgress(progress)
                                            }
                                        }
                                    }
                                }
                                output.flush()
                            }
                        }

                        if (totalExpectedLength <= 0L || totalRead >= totalExpectedLength) {
                            downloadSuccess = true
                        }
                    } catch (e: Exception) {
                        lastException = e
                        Log.w("AppUpdateChecker", "Download attempt $attempts failed: ${e.message}, retrying...")
                        delay(1000L * attempts)
                    } finally {
                        conn?.disconnect()
                    }
                }

                if (!downloadSuccess) {
                    throw lastException ?: IOException("Failed to download APK after $maxAttempts attempts")
                }

                withContext(Dispatchers.Main) {
                    onProgress(100)
                    installApk(context, targetFile)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Failed to download update")
                }
            }
        }
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val manageIntent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(manageIntent)
                    return
                }
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("AppUpdateChecker", "Failed to launch package installer", e)
            try {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                Log.e("AppUpdateChecker", "Fallback installer also failed", e2)
            }
        }
    }

    fun openInBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("AppUpdateChecker", "Failed to open browser", e)
        }
    }

    fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").mapNotNull { it.toIntOrNull() }
        val parts2 = v2.split(".").mapNotNull { it.toIntOrNull() }
        val length = maxOf(parts1.size, parts2.size)

        for (i in 0 until length) {
            val num1 = parts1.getOrElse(i) { 0 }
            val num2 = parts2.getOrElse(i) { 0 }
            if (num1 != num2) {
                return num1.compareTo(num2)
            }
        }
        return 0
    }
}
