package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.BitmapFactory
import java.net.URL
import com.example.R
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import android.graphics.drawable.BitmapDrawable
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class NotificationWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = FirebaseFirestore.getInstance()
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val lastCheck = prefs.getLong("last_notif_check", System.currentTimeMillis())

            val snapshot = db.collection("notifications")
                .whereGreaterThan("timestamp", lastCheck)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                var maxTimestamp = lastCheck
                for (doc in snapshot.documents) {
                    val notifUserId = doc.getString("userId")
                    val currentUid = FirebaseAuth.getInstance().currentUser?.uid
                    
                    val isForMe = notifUserId.isNullOrEmpty() || notifUserId == currentUid
                    val timestamp = doc.getLong("timestamp") ?: 0L

                    if (isForMe) {
                        val title = doc.getString("title") ?: "New Notification"
                        val body = doc.getString("message") ?: "You have a new message."
                        val imageUrl = doc.getString("imageUrl")
                        
                        showNotification(title, body, imageUrl, doc.id.hashCode())
                    }
                    if (timestamp > maxTimestamp) {
                        maxTimestamp = timestamp
                    }
                }
                prefs.edit().putLong("last_notif_check", maxTimestamp).apply()
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun showNotification(title: String, message: String, imageUrl: String?, notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "high_importance_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "General Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingIntentFlags)

        var bitmap: android.graphics.Bitmap? = null
        if (!imageUrl.isNullOrEmpty()) {
            try {
                val url = URL(imageUrl)
                bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
            val appIcon = if (drawable is BitmapDrawable) {
                drawable.bitmap
            } else if (drawable != null) {
                val b = Bitmap.createBitmap(
                    drawable.intrinsicWidth.takeIf { it > 0 } ?: 100,
                    drawable.intrinsicHeight.takeIf { it > 0 } ?: 100,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(b)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                b
            } else null

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.example.R.drawable.ic_notification)
            .setLargeIcon(appIcon)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        if (bitmap != null) {
            builder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap))
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(message))
        }

        notificationManager.notify(notificationId, builder.build())
    }
}
