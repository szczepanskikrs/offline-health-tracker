package io.github.szczepanskikrs.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import io.github.szczepanskikrs.MainActivity
import java.util.Locale

object WalkTrackerState {
    val isTracking = MutableStateFlow(false)
    val secondsElapsed = MutableStateFlow(0L)
    val distanceKm = MutableStateFlow(0.0)
    val pathPoints = MutableStateFlow<List<GeoPoint>>(emptyList())
    val lastLocation = MutableStateFlow<Location?>(null)

    fun reset() {
        isTracking.value = false
        secondsElapsed.value = 0L
        distanceKm.value = 0.0
        pathPoints.value = emptyList()
        lastLocation.value = null
    }
}

class LocationTrackingService : Service() {

    private lateinit var locationManager: LocationManager
    private var serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var timerJob: Job? = null

    companion object {
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "health_tracker_walk_tracking"
        private const val CHANNEL_NAME = "Aktywny Spacer"
        private const val CHANNEL_DESC = "Wyświetla bieżący dystans i czas spaceru podczas rejestrowania trasy"

        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_STOP = "ACTION_STOP"

        fun startService(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun pauseTracking(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resumeTracking(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val prevLocation = WalkTrackerState.lastLocation.value
            WalkTrackerState.lastLocation.value = location
            
            if (WalkTrackerState.isTracking.value) {
                val newPoint = GeoPoint(location.latitude, location.longitude)
                val points = WalkTrackerState.pathPoints.value.toMutableList()
                
                if (points.isNotEmpty()) {
                    if (prevLocation != null && prevLocation != location) {
                        val distMeters = prevLocation.distanceTo(location)
                        // GPS Jitter filter: ignore tiny updates or inaccurate data
                        if (distMeters > 3.0 && location.accuracy < 25f) {
                            WalkTrackerState.distanceKm.value += distMeters / 1000.0
                            points.add(newPoint)
                            WalkTrackerState.pathPoints.value = points
                            updateNotification()
                        }
                    }
                } else {
                    points.add(newPoint)
                    WalkTrackerState.pathPoints.value = points
                    updateNotification()
                }
            }
        }

        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                WalkTrackerState.reset()
                WalkTrackerState.isTracking.value = true
                startForeground(NOTIFICATION_ID, buildNotification())
                startLocationUpdates()
                startTimer()
            }
            ACTION_PAUSE -> {
                WalkTrackerState.isTracking.value = false
                stopTimer()
                updateNotification()
            }
            ACTION_RESUME -> {
                WalkTrackerState.isTracking.value = true
                startTimer()
                updateNotification()
            }
            ACTION_STOP -> {
                stopLocationUpdates()
                stopTimer()
                WalkTrackerState.isTracking.value = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                // Request updates every 2 seconds or 2 meters
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000L,
                    2f,
                    locationListener
                )
                val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (lastKnown != null) {
                    WalkTrackerState.lastLocation.value = lastKnown
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    private fun stopLocationUpdates() {
        locationManager.removeUpdates(locationListener)
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (WalkTrackerState.isTracking.value) {
                delay(1000)
                WalkTrackerState.secondsElapsed.value += 1
                // Update notification text every 5 seconds to reduce UI thread calls
                if (WalkTrackerState.secondsElapsed.value % 5 == 0L) {
                    updateNotification()
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): android.app.Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val seconds = WalkTrackerState.secondsElapsed.value
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        val timeString = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs)
        val distanceString = String.format(Locale.getDefault(), "%.2f km", WalkTrackerState.distanceKm.value)

        val statusText = if (WalkTrackerState.isTracking.value) "Rejestrowanie..." else "Wstrzymano"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Spacer w toku")
            .setContentText("$statusText | Czas: $timeString | Dystans: $distanceString")
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = CHANNEL_DESC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
