package com.example.nimons360.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.nimons360.MainActivity
import com.example.nimons360.R
import com.example.nimons360.data.local.preference.NotificationPreference
import com.example.nimons360.data.local.preference.TokenPreference
import com.example.nimons360.data.repository.NotificationRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var notificationPreference: NotificationPreference
    @Inject lateinit var notificationRepository: NotificationRepository
    @Inject lateinit var tokenPreference: TokenPreference

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (!notificationPreference.isNotificationEnabled()) return
        serviceScope.launch {
            notificationRepository.subscribeToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        if (!notificationPreference.isNotificationEnabled()) return

        // Filter greeting by target user ID if present
        val targetUserIdStr = message.data["targetUserId"] ?: message.data["target_user_id"]
        if (!targetUserIdStr.isNullOrEmpty()) {
            val targetUserId = targetUserIdStr.toIntOrNull()
            val currentUserId = tokenPreference.getUserId()
            if (targetUserId != null && currentUserId != -1 && targetUserId != currentUserId) {
                // Not the intended recipient of this greeting/notification, ignore it
                return
            }
        }

        val title = message.notification?.title 
            ?: message.data["title"] 
            ?: "Nimons360"
            
        val body = message.notification?.body 
            ?: message.data["body"]
            ?: message.data["message"]
            ?: return

        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "nimons360_notif"
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Nimons360 Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Family notifications and greetings"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}