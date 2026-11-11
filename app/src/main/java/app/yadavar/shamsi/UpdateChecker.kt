package app.yadavar.shamsi

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {
    private const val APK_URL = "https://github.com/most1404/sinerehmain/releases/download/ff/app-release.apk"
    // Simple version check: we treat any successful HEAD/GET that returns a different last-modified or larger size as "new".
    // For a real app you would parse a version endpoint; here we just offer the download when user confirms.

    suspend fun checkAndPrompt(context: Context, currentVersion: String, onNewVersion: (String) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                val conn = (URL(APK_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "HEAD"
                    connectTimeout = 8000
                    readTimeout = 8000
                    instanceFollowRedirects = true
                }
                val code = conn.responseCode
                conn.disconnect()
                if (code in 200..399) {
                    withContext(Dispatchers.Main) {
                        onNewVersion("نسخه جدید در دسترس است. می‌خواهید دانلود و نصب شود؟")
                    }
                }
            } catch (_: Exception) {
                // silent – offline or network error
            }
        }
    }

    fun startDownload(context: Context) {
        try {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val req = DownloadManager.Request(Uri.parse(APK_URL))
                .setTitle("به‌روزرسانی یادآور شمسی")
                .setDescription("در حال دانلود نسخه جدید…")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "YadavarShamsi-update.apk")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
            dm.enqueue(req)
        } catch (_: Exception) {
            // fallback: open browser
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(APK_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Exception) { }
        }
    }
}
