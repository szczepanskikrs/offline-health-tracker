package io.github.szczepanskikrs.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import io.github.szczepanskikrs.data.ExerciseLog
import io.github.szczepanskikrs.data.HealthTrackerViewModel
import io.github.szczepanskikrs.service.WalkTrackerState
import io.github.szczepanskikrs.service.LocationTrackingService
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalksScreen(
    viewModel: HealthTrackerViewModel,
    modifier: Modifier = Modifier,
    showTopAppBar: Boolean = true
) {
    val context = LocalContext.current
    val exerciseTypes by viewModel.exerciseTypes.collectAsState()
    val exerciseLogs by viewModel.exerciseLogs.collectAsState()

    // Filter logs for Walks (Spacer)
    val walkLogs = remember(exerciseLogs) {
        exerciseLogs.filter { it.exerciseName.lowercase() == "spacer" }
    }

    var activeTab by remember { mutableStateOf(0) } // 0 = Rejestruj, 1 = Historia tras
    var hasLocationPermission by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = fineGranted || coarseGranted
    }

    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    var showRouteDialog by remember { mutableStateOf<ExerciseLog?>(null) }

    Scaffold(
        topBar = {
            if (showTopAppBar) {
                TopAppBar(
                    title = { Text("Śledzenie Spacerów", fontWeight = FontWeight.Black) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Selector
            SecondaryTabRow(selectedTabIndex = activeTab) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Nowy spacer", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Moje trasy", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.History, contentDescription = null) }
                )
            }

            if (activeTab == 0) {
                if (!hasLocationPermission) {
                    LocationPermissionRequestScreen(onGrantRequest = {
                        val permissionsToRequest = mutableListOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                        if (android.os.Build.VERSION.SDK_INT >= 33) { // Android 13 (Tiramisu)
                            permissionsToRequest.add("android.permission.POST_NOTIFICATIONS")
                        }
                        permissionLauncher.launch(permissionsToRequest.toTypedArray())
                    })
                } else {
                    WalkTrackerMap(
                        viewModel = viewModel,
                        exerciseTypes = exerciseTypes,
                        modifier = Modifier.fillMaxSize().weight(1f).clipToBounds()
                    )
                }
            } else {
                WalkHistoryList(
                    walkLogs = walkLogs,
                    dateFormat = dateFormat,
                    onShowOnMap = { showRouteDialog = it },
                    onDelete = { viewModel.deleteExerciseLog(it.id) }
                )
            }
        }
    }

    // Dialog showing recorded route path
    if (showRouteDialog != null) {
        val routeLog = showRouteDialog!!
        Dialog(onDismissRequest = { showRouteDialog = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Trasa spaceru",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.CenterStart)
                        )
                        IconButton(
                            onClick = { showRouteDialog = null },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Zamknij")
                        }
                    }

                    val points = remember(routeLog.routePath) {
                        parseRoutePath(routeLog.routePath)
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        if (points.isNotEmpty()) {
                            AndroidView(
                                factory = { context ->
                                    MapView(context).apply {
                                        onResume()
                                        setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
                                        setMultiTouchControls(true)
                                        zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
                                        controller.setZoom(16.0)

                                        val polyline = Polyline(this).apply {
                                            outlinePaint.color = android.graphics.Color.rgb(16, 185, 129)
                                            outlinePaint.strokeWidth = 8f
                                            setPoints(points)
                                        }
                                        overlays.add(polyline)

                                        val startM = Marker(this).apply {
                                            position = points.first()
                                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                            title = "Start"
                                        }
                                        overlays.add(startM)

                                        val endM = Marker(this).apply {
                                            position = points.last()
                                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                            title = "Meta"
                                        }
                                        overlays.add(endM)

                                        controller.setCenter(points.first())
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Brak zapisanej trasy (brak sygnału GPS)", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Dystans: ${routeLog.weight ?: 0.0} km", fontWeight = FontWeight.Bold)
                            Text(text = "Czas: ${routeLog.reps} min", fontWeight = FontWeight.Bold)
                            Text(text = "Kalorie: ${routeLog.calories.toInt()} kcal", fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                        if (routeLog.notes.isNotEmpty()) {
                            Text(
                                text = "Opis: ${routeLog.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LocationPermissionRequestScreen(onGrantRequest: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "Wymagany dostęp do GPS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Aby aplikacja mogła śledzić trasę Twojego spaceru, rysować ją na mapie i liczyć pokonany dystans, potrzebuje dostępu do lokalizacji urządzenia.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(
                onClick = onGrantRequest,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Udziel uprawnień")
            }
        }
    }
}

@Composable
fun WalkTrackerMap(
    viewModel: HealthTrackerViewModel,
    exerciseTypes: List<io.github.szczepanskikrs.data.ExerciseType>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }

    val isTracking by WalkTrackerState.isTracking.collectAsState()
    val secondsElapsed by WalkTrackerState.secondsElapsed.collectAsState()
    val distanceKm by WalkTrackerState.distanceKm.collectAsState()
    val pathPoints by WalkTrackerState.pathPoints.collectAsState()
    val lastLocation by WalkTrackerState.lastLocation.collectAsState()
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    
    // Live save dialog state
    var showSaveDialog by remember { mutableStateOf(false) }
    var routeNotes by remember { mutableStateOf("") }

    // Centering map on initial location load
    LaunchedEffect(mapViewInstance, lastLocation) {
        val mv = mapViewInstance
        val loc = lastLocation
        if (mv != null && loc != null && pathPoints.isEmpty()) {
            mv.controller.setCenter(GeoPoint(loc.latitude, loc.longitude))
        }
    }

    // GPS Location listener for centering/updating current location marker in UI when NOT tracking.
    // When tracking, the Service updates the WalkTrackerState.lastLocation itself.
    val locationListener = remember {
        object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (!isTracking) {
                    WalkTrackerState.lastLocation.value = location
                }
            }
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }
    }

    // Always listen to location updates while Walks tab is active (for map display/initial centering)
    DisposableEffect(Unit) {
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2000L, // 2s
                2f, // 2m
                locationListener
            )
            // Get last known location for initial map centering
            val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (lastKnown != null && lastLocation == null) {
                WalkTrackerState.lastLocation.value = lastKnown
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        onDispose {
            locationManager.removeUpdates(locationListener)
        }
    }

    val caloriesBurned = distanceKm * 60.0

    Box(modifier = modifier) {
        // Map View
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    onResume()
                    setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    // Disable built-in zoom controls to prevent overlap with start button
                    zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                    controller.setZoom(16.0)
                    
                    // Initial Warsaw center
                    val defaultPt = GeoPoint(52.2297, 21.0122)
                    controller.setCenter(defaultPt)
                    
                    mapViewInstance = this
                }
            },
            update = { mapView ->
                mapView.overlays.clear()
                
                // Draw route path
                if (pathPoints.isNotEmpty()) {
                    val polyline = Polyline(mapView).apply {
                        outlinePaint.color = android.graphics.Color.rgb(16, 185, 129) // Emerald
                        outlinePaint.strokeWidth = 8f
                        setPoints(pathPoints)
                    }
                    mapView.overlays.add(polyline)

                    // Start marker
                    val startMarker = Marker(mapView).apply {
                        position = pathPoints.first()
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Początek"
                    }
                    mapView.overlays.add(startMarker)
                }

                // Current location marker (always draw if we have lastLocation)
                val lastLoc = lastLocation
                if (lastLoc != null) {
                    val currentPt = GeoPoint(lastLoc.latitude, lastLoc.longitude)
                    val currentMarker = Marker(mapView).apply {
                        position = currentPt
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "Twoja lokalizacja"
                    }
                    mapView.overlays.add(currentMarker)
                    
                    // Keep centering map on user location while tracking
                    if (isTracking && pathPoints.isNotEmpty()) {
                        mapView.controller.animateTo(pathPoints.last())
                    }
                }
            },
            modifier = Modifier.fillMaxSize().clipToBounds()
        )

        // Real-time Stats overlay
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Czas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatTime(secondsElapsed), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Dystans", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(String.format(Locale.US, "%.2f km", distanceKm), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Kalorie", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(String.format(Locale.US, "%.0f kcal", caloriesBurned), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                }
            }
        }

        // Control buttons overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
                .align(Alignment.BottomCenter)
        ) {
            if (!isTracking && secondsElapsed == 0L) {
                Button(
                    onClick = {
                        LocationTrackingService.startService(context)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rozpocznij spacer", fontWeight = FontWeight.Bold)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            if (isTracking) {
                                LocationTrackingService.pauseTracking(context)
                            } else {
                                LocationTrackingService.resumeTracking(context)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTracking) MaterialTheme.colorScheme.error else Color(0xFF10B981)
                        )
                    ) {
                        Icon(if (isTracking) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isTracking) "Wstrzymaj" else "Wznów", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            LocationTrackingService.pauseTracking(context)
                            showSaveDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Zapisz", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Vertical Floating Control Panel (Zoom In, Zoom Out, My Location)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 100.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Zoom In
            FloatingActionButton(
                onClick = { mapViewInstance?.let { it.controller.zoomIn() } },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Przybliż")
            }

            // Zoom Out
            FloatingActionButton(
                onClick = { mapViewInstance?.let { it.controller.zoomOut() } },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Oddal")
            }

            // My Location
            FloatingActionButton(
                onClick = {
                    val lastLoc = lastLocation
                    if (lastLoc != null) {
                        mapViewInstance?.controller?.animateTo(GeoPoint(lastLoc.latitude, lastLoc.longitude))
                    } else if (pathPoints.isNotEmpty()) {
                        mapViewInstance?.controller?.animateTo(pathPoints.last())
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Moja lokalizacja")
            }
        }
    }

    // Save Walk dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Zapisz spacer", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Sprawdź parametry spaceru przed zapisaniem:")
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Dystans:", fontWeight = FontWeight.Medium)
                        Text(String.format(Locale.US, "%.2f km", distanceKm), fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Czas:", fontWeight = FontWeight.Medium)
                        Text("${secondsElapsed / 60} min ${secondsElapsed % 60} sek", fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Kalorie:", fontWeight = FontWeight.Medium)
                        Text(String.format(Locale.US, "%.0f kcal", caloriesBurned), fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }

                    OutlinedTextField(
                        value = routeNotes,
                        onValueChange = { routeNotes = it },
                        label = { Text("Nazwa trasy / Notatki") },
                        placeholder = { Text("np. spacer po parku, do lasu") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val spacerType = exerciseTypes.firstOrNull { it.name.lowercase() == "spacer" }
                        val spacerId = spacerType?.id ?: 1L

                        // Convert coordinates to JSON
                        val jsonArray = JSONArray()
                        pathPoints.forEach { pt ->
                            val obj = JSONObject().apply {
                                put("lat", pt.latitude)
                                put("lng", pt.longitude)
                            }
                            jsonArray.put(obj)
                        }
                        val routeJson = if (pathPoints.isNotEmpty()) jsonArray.toString() else null

                        viewModel.addWalkLog(
                            exerciseId = spacerId,
                            durationMinutes = (secondsElapsed / 60).toInt().coerceAtLeast(1),
                            distanceKm = Math.round(distanceKm * 100.0) / 100.0,
                            calories = caloriesBurned,
                            routePathJson = routeJson,
                            notes = routeNotes
                        )

                        // Stop service
                        LocationTrackingService.stopService(context)
                        routeNotes = ""
                        showSaveDialog = false
                    }
                ) {
                    Text("Zapisz w historii")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSaveDialog = false
                    }
                ) {
                    Text("Anuluj")
                }
            }
        )
    }
}

@Composable
fun WalkHistoryList(
    walkLogs: List<ExerciseLog>,
    dateFormat: SimpleDateFormat,
    onShowOnMap: (ExerciseLog) -> Unit,
    onDelete: (ExerciseLog) -> Unit
) {
    if (walkLogs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "Brak zarejestrowanych tras",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Rozpocznij spacer w zakładce 'Nowy spacer', aby zarejestrować swoją pierwszą trasę.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(walkLogs) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                contentDescription = null,
                                tint = Color(0xFF10B981)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (log.notes.isNotEmpty()) log.notes else "Spacer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "${log.weight ?: 0.0} km • ${log.reps} min • ${log.calories.toInt()} kcal",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF10B981)
                            )

                            Text(
                                text = dateFormat.format(Date(log.timestamp)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!log.routePath.isNullOrEmpty()) {
                                IconButton(onClick = { onShowOnMap(log) }) {
                                    Icon(
                                        imageVector = Icons.Default.Map,
                                        contentDescription = "Pokaż trasę na mapie",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            IconButton(onClick = { onDelete(log) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Usuń trasę",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatTime(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

fun parseRoutePath(routePathJson: String?): List<GeoPoint> {
    val list = mutableListOf<GeoPoint>()
    if (routePathJson.isNullOrEmpty()) return list
    try {
        val jsonArray = JSONArray(routePathJson)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            list.add(GeoPoint(obj.getDouble("lat"), obj.getDouble("lng")))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}
