package com.example.nimons360.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.nimons360.R
import com.example.nimons360.data.local.preference.PinPreference
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class PinDownloadService : Service() {

    @Inject
    lateinit var okHttpClient: OkHttpClient

    @Inject
    lateinit var pinPreference: PinPreference

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val CHANNEL_ID = "pin_download_channel"
        const val NOTIFICATION_ID = 2002

        // Broadcast actions
        const val ACTION_PIN_DOWNLOAD_PROGRESS = "com.example.nimons360.ACTION_PIN_DOWNLOAD_PROGRESS"
        
        // Extras
        const val EXTRA_PIN_ID = "extra_pin_id"
        const val EXTRA_PIN_NAME = "extra_pin_name"
        const val EXTRA_PIN_URL = "extra_pin_url"
        const val EXTRA_PROGRESS = "extra_progress"
        const val EXTRA_STATUS = "extra_status" // "downloading", "success", "error"
        const val EXTRA_LOCAL_PATH = "extra_local_path"
        const val EXTRA_ERROR = "extra_error"

        fun start(context: Context, pinId: String, pinName: String, pinUrl: String) {
            val intent = Intent(context, PinDownloadService::class.java).apply {
                putExtra(EXTRA_PIN_ID, pinId)
                putExtra(EXTRA_PIN_NAME, pinName)
                putExtra(EXTRA_PIN_URL, pinUrl)
            }
            context.startForegroundService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pinId = intent?.getStringExtra(EXTRA_PIN_ID)
        val pinName = intent?.getStringExtra(EXTRA_PIN_NAME)
        val pinUrl = intent?.getStringExtra(EXTRA_PIN_URL)

        if (pinId == null || pinName == null || pinUrl == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildProgressNotification(pinName, 0)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Start download in coroutine
        serviceScope.launch {
            downloadPin(pinId, pinName, pinUrl)
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun downloadPin(pinId: String, pinName: String, pinUrl: String) {
        try {
            sendBroadcastUpdate(pinId, 0, "downloading")

            val request = Request.Builder().url(pinUrl).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                throw Exception("Failed with code ${response.code}")
            }

            val body = response.body ?: throw Exception("Response body was empty")
            val contentLength = body.contentLength()
            val inputStream = body.byteStream()

            val dir = File(filesDir, "custom_pins").apply { mkdirs() }
            val destFile = File(dir, "${pinId}.png")
            val outputStream = FileOutputStream(destFile)

            val buffer = ByteArray(4096)
            var bytesRead: Int
            var totalBytesRead = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                if (contentLength > 0) {
                    val progress = ((totalBytesRead * 100) / contentLength).toInt().coerceIn(0, 100)
                    updateNotification(pinName, progress)
                    sendBroadcastUpdate(pinId, progress, "downloading")
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            // Success
            val finalPath = destFile.absolutePath
            pinPreference.saveDownloadedPin(pinId, finalPath)
            sendBroadcastUpdate(pinId, 100, "success", finalPath)
        } catch (e: Exception) {
            sendBroadcastUpdate(pinId, 0, "error", errorMsg = e.localizedMessage)
        } finally {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun updateNotification(pinName: String, progress: Int) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildProgressNotification(pinName, progress))
    }

    private fun buildProgressNotification(pinName: String, progress: Int): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mengunduh Pin")
            .setContentText("Mengunduh $pinName... ($progress%)")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Pengunduhan Pin",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Menampilkan progres pengunduhan pin kustom"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun sendBroadcastUpdate(
        pinId: String,
        progress: Int,
        status: String,
        localPath: String? = null,
        errorMsg: String? = null
    ) {
        val intent = Intent(ACTION_PIN_DOWNLOAD_PROGRESS).apply {
            putExtra(EXTRA_PIN_ID, pinId)
            putExtra(EXTRA_PROGRESS, progress)
            putExtra(EXTRA_STATUS, status)
            localPath?.let { putExtra(EXTRA_LOCAL_PATH, it) }
            errorMsg?.let { putExtra(EXTRA_ERROR, it) }
        }
        sendBroadcast(intent)
    }
}
