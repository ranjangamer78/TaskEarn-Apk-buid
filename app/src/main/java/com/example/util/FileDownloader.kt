package com.example.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.widget.Toast
import java.io.File

object FileDownloader {

    /**
     * Downloads a file using Android's DownloadManager with robust multi-device compatibility:
     * - Fixes destination directory errors on Android 7.0 through Android 15/16
     * - Supports custom/sanitized file names without illegal characters
     * - Configures proper headers, cookies, and MIME types (including APK packaging)
     * - Gracefully falls back to browser if DownloadManager service is disabled or blocked
     */
    fun downloadFile(
        context: Context,
        url: String,
        userAgent: String? = null,
        contentDisposition: String? = null,
        mimeType: String? = null,
        title: String? = null
    ) {
        if (url.isBlank()) return

        // If market scheme, route to Play Store
        if (url.startsWith("market://") || url.contains("play.google.com/store/apps")) {
            openPlayStoreOrBrowser(context, url)
            return
        }

        val cleanUrl = if (!url.contains("://") && !url.startsWith("market:")) "https://$url" else url

        try {
            val uri = Uri.parse(cleanUrl)
            val scheme = uri.scheme?.lowercase() ?: ""

            if (scheme != "http" && scheme != "https") {
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            }

            var resolvedFileName = URLUtil.guessFileName(cleanUrl, contentDisposition, mimeType)
            // Sanitize illegal filesystem characters for older and OEM devices (MIUI, ColorOS, etc.)
            resolvedFileName = resolvedFileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            if (resolvedFileName.isBlank() || resolvedFileName == "_") {
                resolvedFileName = "download_${System.currentTimeMillis()}"
            }

            val isApk = cleanUrl.contains(".apk", ignoreCase = true) ||
                    contentDisposition?.contains(".apk", ignoreCase = true) == true ||
                    mimeType == "application/vnd.android.package-archive"

            if (isApk && !resolvedFileName.endsWith(".apk", ignoreCase = true)) {
                resolvedFileName = if (resolvedFileName.contains(".")) {
                    resolvedFileName.substringBeforeLast(".") + ".apk"
                } else {
                    "$resolvedFileName.apk"
                }
            }

            val request = DownloadManager.Request(uri).apply {
                if (isApk) {
                    setMimeType("application/vnd.android.package-archive")
                } else if (!mimeType.isNullOrBlank() && mimeType != "application/octet-stream") {
                    setMimeType(mimeType)
                }

                try {
                    val cookies = CookieManager.getInstance().getCookie(cleanUrl)
                    if (!cookies.isNullOrBlank()) {
                        addRequestHeader("cookie", cookies)
                    }
                } catch (_: Throwable) {}

                if (!userAgent.isNullOrBlank()) {
                    addRequestHeader("User-Agent", userAgent)
                }

                val displayTitle = title ?: resolvedFileName
                setTitle(displayTitle)
                setDescription("Downloading $resolvedFileName...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

                // Safe public Downloads directory targeting across all Android versions
                try {
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, resolvedFileName)
                } catch (e: Throwable) {
                    // Fallback to internal app files path if external storage directory cannot be acquired
                    try {
                        val externalFiles = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        if (externalFiles != null) {
                            setDestinationUri(Uri.fromFile(File(externalFiles, resolvedFileName)))
                        }
                    } catch (_: Throwable) {}
                }

                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (dm != null) {
                dm.enqueue(request)
                Toast.makeText(context, "Starting download: $resolvedFileName", Toast.LENGTH_SHORT).show()
            } else {
                openInBrowser(context, cleanUrl)
            }
        } catch (e: Throwable) {
            android.util.Log.w("FileDownloader", "DownloadManager failed: ${e.message}. Opening via browser.", e)
            openInBrowser(context, cleanUrl)
        }
    }

    /**
     * Handles URLs inside WebViews.
     * Intercepts:
     * - market:// links (redirects to Google Play)
     * - intent:// links (resolves native apps or market fallbacks)
     * - custom app schemes (e.g., whatsapp, telegram, mailto, tel)
     * - direct download links (.apk, .zip, .rar, .7z, .pdf, .tar, .gz)
     * Returns true if URL was handled externally, false to let WebView render it normally.
     */
    fun handleWebViewUrl(context: Context, url: String?): Boolean {
        if (url.isNullOrBlank()) return false

        // Market / Play Store links
        if (url.startsWith("market://")) {
            openPlayStoreOrBrowser(context, url)
            return true
        }

        // Intent scheme links
        if (url.startsWith("intent://")) {
            try {
                val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return true
                }
                val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                if (!fallbackUrl.isNullOrBlank()) {
                    openInBrowser(context, fallbackUrl)
                    return true
                }
                val pkg = intent.`package`
                if (!pkg.isNullOrBlank()) {
                    openPlayStoreOrBrowser(context, "market://details?id=$pkg")
                    return true
                }
            } catch (e: Throwable) {
                android.util.Log.w("FileDownloader", "Intent parse error: ${e.message}")
            }
            return true
        }

        // Custom app schemes (e.g. wa.me, tel, mailto, tg)
        val uri = Uri.parse(url)
        val scheme = uri.scheme?.lowercase() ?: ""
        if (scheme != "http" && scheme != "https" && scheme != "file" && scheme != "javascript" && scheme != "about" && scheme.isNotEmpty()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Throwable) {
                Toast.makeText(context, "Application not found to open this link", Toast.LENGTH_SHORT).show()
            }
            return true
        }

        // Play Store web URL
        if (url.contains("play.google.com/store/apps")) {
            openPlayStoreOrBrowser(context, url)
            return true
        }

        // Direct file download URLs: .apk, .zip, .rar, .7z, .pdf, .mp3, .mp4, .gz, .tar
        val path = uri.path?.lowercase() ?: ""
        if (path.endsWith(".apk") || path.endsWith(".zip") || path.endsWith(".rar") ||
            path.endsWith(".7z") || path.endsWith(".pdf") || path.endsWith(".tar") ||
            path.endsWith(".gz") || path.endsWith(".dmg")
        ) {
            downloadFile(context, url)
            return true
        }

        return false
    }

    /**
     * Opens or downloads any link (used in update dialog, offer links, etc.)
     * If the link is an APK or direct download, initiates safe download and provides browser fallback.
     */
    fun downloadOrOpen(context: Context, url: String, title: String = "Download") {
        if (url.isBlank()) return
        val cleanUrl = if (!url.contains("://") && !url.startsWith("market:")) "https://$url" else url

        val uri = try { Uri.parse(cleanUrl) } catch (_: Throwable) { null }
        val path = uri?.path?.lowercase() ?: ""

        if (cleanUrl.startsWith("market://") || cleanUrl.contains("play.google.com/store/apps")) {
            openPlayStoreOrBrowser(context, cleanUrl)
        } else if (path.endsWith(".apk") || path.endsWith(".zip") || cleanUrl.contains("download=1") || cleanUrl.contains(".apk", ignoreCase = true)) {
            downloadFile(context, cleanUrl, title = title)
        } else {
            openInBrowser(context, cleanUrl)
        }
    }

    fun openInBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Throwable) {
            Toast.makeText(context, "Could not open link in browser: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openPlayStoreOrBrowser(context: Context, url: String) {
        try {
            val marketUri = if (url.startsWith("market://")) {
                Uri.parse(url)
            } else {
                val pkg = Uri.parse(url).getQueryParameter("id")
                if (pkg != null) Uri.parse("market://details?id=$pkg") else Uri.parse(url)
            }
            val intent = Intent(Intent.ACTION_VIEW, marketUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Throwable) {
            openInBrowser(context, url)
        }
    }
}
