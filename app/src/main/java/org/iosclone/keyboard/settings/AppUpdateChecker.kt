package org.iosclone.keyboard.settings

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
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
                    connectTimeout = 10000
                    readTimeout = 10000
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
                if (targetFile.exists()) targetFile.delete()

                val url = URL(downloadUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = 15000
                    readTimeout = 30000
                    setRequestProperty("User-Agent", "iOS-Keyboard-Android")
                }

                // Handle HTTP redirects manually if required
                var redirectUrl = downloadUrl
                var currentConn = conn
                var code = currentConn.responseCode
                var redirects = 0
                while ((code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP || code == 307 || code == 308) && redirects < 5) {
                    redirectUrl = currentConn.getHeaderField("Location")
                    currentConn = (URL(redirectUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15000
                        readTimeout = 30000
                        setRequestProperty("User-Agent", "iOS-Keyboard-Android")
                    }
                    code = currentConn.responseCode
                    redirects++
                }

                val contentLength = currentConn.contentLength
                currentConn.inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        var totalRead = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (contentLength > 0) {
                                val progress = ((totalRead * 100) / contentLength).toInt()
                                withContext(Dispatchers.Main) {
                                    onProgress(progress)
                                }
                            }
                        }
                        output.flush()
                    }
                }

                // Launch Package Installer Intent
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

    private fun installApk(context: Context, apkFile: File) {
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
    }

    private fun compareVersions(v1: String, v2: String): Int {
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
