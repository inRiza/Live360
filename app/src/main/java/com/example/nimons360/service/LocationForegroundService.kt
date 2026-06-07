package com.example.nimons360.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.nimons360.MainActivity
import com.example.nimons360.R
import com.example.nimons360.data.local.preference.TokenPreference
import com.example.nimons360.data.remote.websocket.WebSocketManager
import com.example.nimons360.data.remote.websocket.model.UpdatePresencePayload
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class LocationForegroundService : Service() {

    @Inject
    lateinit var webSocketManager: WebSocketManager

    @Inject
    lateinit var tokenPreference: TokenPreference

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var presenceJob: Job? = null
    private var pingJob: Job? = null

    private var currentLat: Double? = null
    private var currentLng: Double? = null
    private var currentRotation: Float = 0f

    private var batteryLevel: Int = 0
    private var isCharging: Boolean = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != Intent.ACTION_BATTERY_CHANGED) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            batteryLevel = ((level / scale.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            currentLat = loc.latitude
            currentLng = loc.longitude
            if (loc.hasBearing()) currentRotation = loc.bearing
        }
    }

    private val fusedClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    companion object {
        const val CHANNEL_ID = "location_service_channel"
        const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, LocationForegroundService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocationForegroundService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
        registerBatteryReceiver()
        startLocationUpdates()
        webSocketManager.connect()
        startPresenceLoop()
        startPingLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        presenceJob?.cancel()
        pingJob?.cancel()
        fusedClient.removeLocationUpdates(locationCallback)
        runCatching { unregisterReceiver(batteryReceiver) }
        webSocketManager.disconnect()
        super.onDestroy()
    }

    private fun registerBatteryReceiver() {
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        // Seed initial battery state
        registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.let { intent ->
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            batteryLevel = ((level / scale.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
        }
    }

    private fun startLocationUpdates() {
        try {
            val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 5_000L)
                .setMinUpdateIntervalMillis(3_000L)
                .build()
            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            // Location permission not granted – service will send 0,0
        }
    }

    private fun startPresenceLoop() {
        presenceJob = serviceScope.launch {
            while (true) {
                delay(3_000L)
                publishPresence()
            }
        }
    }

    private fun startPingLoop() {
        pingJob = serviceScope.launch {
            while (true) {
                delay(15_000L)
                webSocketManager.sendPing()
            }
        }
    }

    private fun publishPresence() {
        val lat = currentLat ?: return
        val lng = currentLng ?: return

        val networkStatus = readNetworkStatus()

        webSocketManager.sendPresence(
            UpdatePresencePayload(
                name = tokenPreference.getUserName() ?: "User",
                latitude = lat,
                longitude = lng,
                rotation = currentRotation,
                batteryLevel = batteryLevel,
                isCharging = isCharging,
                internetStatus = networkStatus,
                metadata = mapOf(
                    "source" to "android_background"
                )
            )
        )
    }

    private fun readNetworkStatus(): String {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return "mobile"
        val caps = cm.getNetworkCapabilities(network) ?: return "mobile"
        return if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) "wifi" else "mobile"
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Nimons360 Aktif")
            .setContentText("Lokasi sedang dibagikan ke keluarga Anda")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Layanan Lokasi",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Digunakan untuk membagikan lokasi ke keluarga di background"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
