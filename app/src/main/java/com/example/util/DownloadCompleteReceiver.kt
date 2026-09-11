package com.example.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/**
 * BroadcastReceiver for handling completed downloads.
 * When an APK or file download completes, it notifies the user and launches the package installer
 * or viewer safely using FileProvider on Android 7.0+ (API 24 to 36).
 */
class DownloadCompleteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return

        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (downloadId == -1L) return

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return

        try {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = dm.query(query) ?: return

            if (cursor.moveToFirst()) {
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                val mimeTypeIndex = cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE)
                val titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)

                val status = if (statusIndex != -1) cursor.getInt(statusIndex) else -1
                val localUriStr = if (uriIndex != -1) cursor.getString(uriIndex) else null
                val mimeType = if (mimeTypeIndex != -1) cursor.getString(mimeTypeIndex) else null
                val title = if (titleIndex != -1) cursor.getString(titleIndex) else "File"

                cursor.close()

                if (status == DownloadManager.STATUS_SUCCESSFUL && !localUriStr.isNullOrBlank()) {
                    val downloadedUri = Uri.parse(localUriStr)
                    val filePath = downloadedUri.path
                    val file = if (filePath != null) File(filePath) else null
                    val isApk = downloadedUri.toString().endsWith(".apk", ignoreCase = true) ||
                            mimeType == "application/vnd.android.package-archive" ||
                            title?.endsWith(".apk", ignoreCase = true) == true

                    if (isApk) {
                        Toast.makeText(context, "Download complete: $title. Opening installer...", Toast.LENGTH_LONG).show()
                        openApkInstaller(context, dm, downloadId, file)
                    } else {
                        Toast.makeText(context, "Downloaded: $title", Toast.LENGTH_SHORT).show()
                    }
                } else if (status == DownloadManager.STATUS_FAILED) {
                    Toast.makeText(context, "Download failed. Please try again or open via browser.", Toast.LENGTH_LONG).show()
                }
            } else {
                cursor.close()
            }
        } catch (e: Throwable) {
            android.util.Log.e("DownloadReceiver", "Error processing download completion: ${e.message}", e)
        }
    }

    private fun openApkInstaller(context: Context, dm: DownloadManager, downloadId: Long, localFile: File?) {
        try {
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            var contentUri: Uri? = null

            // 1. Try FileProvider if local file exists and can be resolved
            if (localFile != null && localFile.exists()) {
                try {
                    contentUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        localFile
                    )
                } catch (e: Throwable) {
                    android.util.Log.w("DownloadReceiver", "FileProvider resolution fallback: ${e.message}")
                }
            }

            // 2. Fallback to DownloadManager content URI (Supported on Android 7.0 - 15+)
            if (contentUri == null) {
                contentUri = dm.getUriForDownloadedFile(downloadId)
            }

            if (contentUri != null) {
                installIntent.setDataAndType(contentUri, "application/vnd.android.package-archive")
                context.startActivity(installIntent)
            } else {
                // If direct URI couldn't be obtained, open downloads app/folder
                val downloadsIntent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(downloadsIntent)
            }
        } catch (e: Throwable) {
            android.util.Log.e("DownloadReceiver", "Failed to launch package installer: ${e.message}", e)
            try {
                val downloadsIntent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(downloadsIntent)
            } catch (_: Throwable) {
                Toast.makeText(context, "Open Downloads folder to install the downloaded file.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
