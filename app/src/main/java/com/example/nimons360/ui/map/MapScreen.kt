package com.example.nimons360.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.nimons360.data.local.db.dao.MarkedLocationWithPhotos
import java.io.File
import com.example.nimons360.data.remote.dto.common.FavoriteLocationDto
import com.example.nimons360.ui.map.components.MapLongPressBottomSheet
import com.example.nimons360.ui.map.components.MarkedLocationBottomSheet
import com.example.nimons360.ui.map.components.MarkedLocationDetailBottomSheet
import com.example.nimons360.ui.map.components.UserInfoBottomSheet
import com.example.nimons360.ui.map.components.UserMarkerOverlay
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import kotlin.math.roundToInt
import java.util.Locale

@Composable
fun MapScreen(viewModel: MapViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var recenterRequestId by remember { mutableStateOf(0) }
    var resetNorthRequestId by remember { mutableStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onLocationPermissionChanged(granted)
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onLocationPermissionChanged(granted)
        if (!granted) permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    viewModel.reloadCustomPins()
                    viewModel.startRealtime()
                }
                Lifecycle.Event.ON_STOP -> viewModel.stopRealtime()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    ObserveLocationAndOrientation(
        hasLocationPermission = uiState.hasLocationPermission,
        onLocationChanged = { latitude, longitude, accuracyMeters, timestampMs ->
            viewModel.onCurrentLocationChanged(latitude, longitude, accuracyMeters, timestampMs)
        },
        onRotationChanged = { rotation ->
            viewModel.onCurrentRotationChanged(rotation)
        }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        MapLibreContent(
            state = uiState,
            resolveMarkerColor = viewModel::resolveMarkerColor,
            onMemberClick = { viewModel.onMemberMarkerClicked(it) },
            onMarkedLocationClick = { viewModel.onMarkedLocationClicked(it) },
            onMapLongPress = { lat, lng -> viewModel.onMapLongPressed(lat, lng) },
            onDoubleTap = { viewModel.toggleFavoritesPanel() },
            onFavoriteFocused = viewModel::consumeFocusedFavorite,
            recenterRequestId = recenterRequestId,
            resetNorthRequestId = resetNorthRequestId,
            modifier = Modifier.fillMaxSize()
        )

        UserMarkerOverlay(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            searchQuery = uiState.searchQuery,
            isConnected = uiState.isWsConnected,
            nearbyCount = uiState.nearbyMembers.size,
            nearbyRadiusMeters = uiState.nearbyRadiusMeters,
            favoritesCount = uiState.favoriteLocations.size,
            isFavoritesPanelVisible = uiState.isFavoritesPanelVisible,
            currentUser = uiState.currentUser,
            currentUserMarkerColor = uiState.currentUser?.let(viewModel::resolveMarkerColor),
            onSearchQueryChanged = viewModel::onSearchQueryChanged,
            onFavoritesChipClick = viewModel::toggleFavoritesPanel
        )

        if (uiState.isFavoritesPanelVisible) {
            FavoriteLocationsPanel(
                favorites = uiState.favoriteLocations,
                onRemoveFavorite = viewModel::removeFavoriteLocation,
                onFocusFavorite = viewModel::focusFavoriteLocation,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(top = 148.dp)
            )
        }

        MapFloatingControls(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = if (uiState.currentUser != null) 226.dp else 56.dp),
            onResetNorth = { resetNorthRequestId += 1 },
            onRecenter = { recenterRequestId += 1 }
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.0f))
        )
    }

    // Member info bottom sheet
    uiState.selectedMember?.let { member ->
        UserInfoBottomSheet(
            member = member,
            memberMarkerColor = viewModel.resolveMarkerColor(member),
            onDismiss = viewModel::dismissMemberSheet
        )
    }

    // Map long-press options bottom sheet
    uiState.mapLongPressState?.let { longPressState ->
        MapLongPressBottomSheet(
            state = longPressState,
            onAddMarkedLocation = viewModel::selectAddMarkedLocation,
            onSaveAsFavorite = viewModel::selectSaveAsFavorite,
            onDismiss = viewModel::dismissMapLongPress
        )
    }

    // Add/Edit marked location bottom sheet
    uiState.addEditMarkedLocation?.let { addEditState ->
        MarkedLocationBottomSheet(
            state = addEditState,
            viewModel = viewModel,
            onDismiss = viewModel::dismissAddEditSheet
        )
    }

    // Detail marked location bottom sheet
    uiState.selectedMarkedLocation?.let { item ->
        MarkedLocationDetailBottomSheet(
            item = item,
            viewModel = viewModel,
            onDismiss = viewModel::dismissMarkedLocationDetail
        )
    }
}

@Composable
private fun ObserveLocationAndOrientation(
    hasLocationPermission: Boolean,
    onLocationChanged: (Double, Double, Float, Long) -> Unit,
    onRotationChanged: (Float) -> Unit
) {
    val context = LocalContext.current

    DisposableEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            onDispose { }
        } else {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000L)
                .setMinUpdateIntervalMillis(1_000L)
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: return
                    onLocationChanged(location.latitude, location.longitude, location.accuracy, location.time)
                    if (location.hasBearing()) {
                        onRotationChanged(location.bearing)
                    }
                }
            }

            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())

            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val azimuthRad = orientation[0]
                    val azimuthDeg = ((Math.toDegrees(azimuthRad.toDouble()) + 360.0) % 360.0).toFloat()
                    onRotationChanged(azimuthDeg)
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }

            if (rotationSensor != null) {
                sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
            }

            onDispose {
                fusedClient.removeLocationUpdates(callback)
                sensorManager.unregisterListener(listener)
            }
        }
    }
}

@Composable
private fun MapLibreContent(
    state: MapUiState,
    resolveMarkerColor: (MemberMapUi) -> Int,
    onMemberClick: (String) -> Unit,
    onMarkedLocationClick: (String) -> Unit,
    onMapLongPress: (Double, Double) -> Unit,
    onDoubleTap: () -> Unit,
    onFavoriteFocused: () -> Unit,
    recenterRequestId: Int,
    resetNorthRequestId: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        runCatching {
            MapView(context).apply {
                onCreate(null)
            }
        }.getOrNull()
    }

    if (mapView == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(ComposeColor(0xFFECEFF1))
                .padding(24.dp)
        ) {
            Text(
                text = "Map gagal diinisialisasi. Coba buka ulang layar ini atau restart app.",
                style = MaterialTheme.typography.bodyLarge,
                color = ComposeColor(0xFF263238)
            )
        }
        return
    }

    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { mutableStateOf(false) }
    var didMoveToCurrentUser by remember { mutableStateOf(false) }
    val markerMemberLookup = remember { mutableStateMapOf<Long, String>() }
    val markerFavoriteLookup = remember { mutableStateMapOf<Long, String>() }
    val markerMarkedLookup = remember { mutableStateMapOf<Long, String>() }
    val memberMarkers = remember { linkedMapOf<String, org.maplibre.android.annotations.Marker>() }
    val favoriteMarkers = remember { linkedMapOf<String, org.maplibre.android.annotations.Marker>() }
    val markedMarkers = remember { linkedMapOf<String, org.maplibre.android.annotations.Marker>() }
    val tempLongPressMarkerRef = remember { mutableStateOf<org.maplibre.android.annotations.Marker?>(null) }
    var handledRecenterRequestId by remember { mutableStateOf(0) }
    var handledResetNorthRequestId by remember { mutableStateOf(0) }

    // Double tap detection
    var lastTapMs by remember { mutableStateOf(0L) }

    DisposableEffect(mapView, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            mapView.getMapAsync { map ->
                mapLibreMap = map
                map.setStyle(Style.Builder().fromJson(buildOsmRasterStyleJson())) {
                    styleReady = true
                }
                map.uiSettings.isCompassEnabled = false
                map.uiSettings.isRotateGesturesEnabled = true
                map.uiSettings.isTiltGesturesEnabled = true

                // Long press → open Add Marked Location sheet
                map.addOnMapLongClickListener { latLng ->
                    onMapLongPress(latLng.latitude, latLng.longitude)
                    true
                }

                // Single tap for double-tap detection (toggle favorites panel)
                map.addOnMapClickListener { _ ->
                    val now = System.currentTimeMillis()
                    if (now - lastTapMs < 350L) {
                        onDoubleTap()
                        lastTapMs = 0L
                    } else {
                        lastTapMs = now
                    }
                    false
                }

                map.setOnMarkerClickListener { marker ->
                    val memberId = markerMemberLookup[marker.id]
                    if (memberId != null) {
                        onMemberClick(memberId)
                        return@setOnMarkerClickListener true
                    }
                    val markedId = markerMarkedLookup[marker.id]
                    if (markedId != null) {
                        onMarkedLocationClick(markedId)
                        return@setOnMarkerClickListener true
                    }
                    val favoriteId = markerFavoriteLookup[marker.id]
                    favoriteId != null
                }
            }
            mapView
        },
        update = {
            val map = mapLibreMap ?: return@AndroidView
            if (!styleReady) return@AndroidView
            markerMemberLookup.clear()
            markerFavoriteLookup.clear()
            markerMarkedLookup.clear()

            // Sync favorite markers
            syncFavoriteMarkers(
                context = context,
                map = map,
                favorites = state.favoriteLocations,
                favoriteMarkers = favoriteMarkers,
                markerFavoriteLookup = markerFavoriteLookup,
                customPinFavoritePath = state.customPinFavoritePath
            )

            // Sync marked location markers
            syncMarkedLocationMarkers(
                context = context,
                map = map,
                markedLocations = state.markedLocations,
                markedMarkers = markedMarkers,
                markerMarkedLookup = markerMarkedLookup
            )

            // Sync current user marker
            state.currentUser?.let { currentUser ->
                syncMemberMarker(
                    context = context,
                    map = map,
                    member = currentUser,
                    memberMarkers = memberMarkers,
                    markerMemberLookup = markerMemberLookup,
                    pinColor = resolveMarkerColor(currentUser),
                    drawArrow = true,
                    customPinBiasaPath = state.customPinBiasaPath
                )

                if (!didMoveToCurrentUser) {
                    map.animateCamera(
                        org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(
                            LatLng(currentUser.latitude, currentUser.longitude),
                            15.5
                        )
                    )
                    didMoveToCurrentUser = true
                }
            }

            val filteredRemoteMembers = state.remoteMembers.filter { member ->
                (state.searchQuery.isEmpty() || member.fullName.contains(state.searchQuery, ignoreCase = true)) &&
                memberMatchesSelectedFamily(member, state.selectedFamilyId)
            }

            filteredRemoteMembers.forEach { member ->
                syncMemberMarker(
                    context = context,
                    map = map,
                    member = member,
                    memberMarkers = memberMarkers,
                    markerMemberLookup = markerMemberLookup,
                    pinColor = resolveMarkerColor(member),
                    drawArrow = false,
                    customPinBiasaPath = state.customPinBiasaPath
                )
            }

            val allMemberIds = filteredRemoteMembers.map { it.id }.toSet() + (state.currentUser?.id ?: "")
            val removedMemberIds = memberMarkers.keys.filter { it !in allMemberIds }
            removedMemberIds.forEach { id ->
                memberMarkers[id]?.let { map.removeMarker(it) }
                memberMarkers.remove(id)
            }

            // Sync temporary long-press marker
            val longPress = state.mapLongPressState
            if (longPress != null) {
                val longPressLatLng = LatLng(longPress.latitude, longPress.longitude)
                val existing = tempLongPressMarkerRef.value
                if (existing == null) {
                    val marker = map.addMarker(
                        MarkerOptions()
                            .position(longPressLatLng)
                            .title("Lokasi Dipilih")
                            .snippet(longPress.address)
                    )
                    tempLongPressMarkerRef.value = marker
                } else {
                    existing.position = longPressLatLng
                    existing.snippet = longPress.address
                }
            } else {
                tempLongPressMarkerRef.value?.let { marker ->
                    map.removeMarker(marker)
                    tempLongPressMarkerRef.value = null
                }
            }

            state.focusedFavoriteLocationId?.let { favId ->
                state.favoriteLocations.find { it.id == favId }?.let { fav ->
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(fav.latitude, fav.longitude),
                            16.5
                        )
                    )
                    onFavoriteFocused()
                }
            }

            if (recenterRequestId != handledRecenterRequestId) {
                handledRecenterRequestId = recenterRequestId
                state.currentUser?.let { currentUser ->
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(currentUser.latitude, currentUser.longitude),
                            17.5
                        )
                    )
                }
            }

            if (resetNorthRequestId != handledResetNorthRequestId) {
                handledResetNorthRequestId = resetNorthRequestId
                val northUp = CameraPosition.Builder(map.cameraPosition)
                    .bearing(0.0)
                    .build()
                map.animateCamera(CameraUpdateFactory.newCameraPosition(northUp))
            }
        }
    )
}

@Composable
private fun MapFloatingControls(
    modifier: Modifier = Modifier,
    onResetNorth: () -> Unit,
    onRecenter: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(52.dp)
                .clickable { onResetNorth() },
            shape = CircleShape,
            color = ComposeColor.White.copy(alpha = 0.95f),
            shadowElevation = 6.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = "Reset north",
                    tint = ComposeColor(0xFF1976D2)
                )
            }
        }

        Surface(
            modifier = Modifier
                .size(52.dp)
                .clickable { onRecenter() },
            shape = CircleShape,
            color = ComposeColor(0xFF0B3D91),
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Recenter",
                    tint = ComposeColor.White
                )
            }
        }
    }
}

private fun memberMatchesSelectedFamily(member: MemberMapUi, selectedFamilyId: Int?): Boolean {
    if (selectedFamilyId == null) return true
    return selectedFamilyId in member.familyIds
}

private fun syncMemberMarker(
    context: Context,
    map: MapLibreMap,
    member: MemberMapUi,
    memberMarkers: MutableMap<String, org.maplibre.android.annotations.Marker>,
    markerMemberLookup: MutableMap<Long, String>,
    pinColor: Int,
    drawArrow: Boolean,
    customPinBiasaPath: String?
) {
    val latLng = LatLng(member.latitude, member.longitude)
    val existingMarker = memberMarkers[member.id]

    if (existingMarker == null) {
        val icon = IconFactory.getInstance(context).fromBitmap(
            createMemberBitmap(context, member.fullName, pinColor, member.rotation, drawArrow, customPinBiasaPath)
        )
        val marker = map.addMarker(
            MarkerOptions()
                .position(latLng)
                .icon(icon)
                .title(member.fullName)
        )
        memberMarkers[member.id] = marker
        markerMemberLookup[marker.id] = member.id
    } else {
        existingMarker.position = latLng
        val icon = IconFactory.getInstance(context).fromBitmap(
            createMemberBitmap(context, member.fullName, pinColor, member.rotation, drawArrow, customPinBiasaPath)
        )
        existingMarker.icon = icon
        markerMemberLookup[existingMarker.id] = member.id
    }
}

private fun syncFavoriteMarkers(
    context: Context,
    map: MapLibreMap,
    favorites: List<FavoriteLocationDto>,
    favoriteMarkers: MutableMap<String, org.maplibre.android.annotations.Marker>,
    markerFavoriteLookup: MutableMap<Long, String>,
    customPinFavoritePath: String?
) {
    favorites.forEach { fav ->
        val latLng = LatLng(fav.latitude, fav.longitude)
        val existing = favoriteMarkers[fav.id]
        if (existing == null) {
            val icon = IconFactory.getInstance(context).fromBitmap(
                createFavoriteBitmap(context, customPinFavoritePath)
            )
            val marker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .icon(icon)
                    .title(fav.label)
                    .snippet(fav.address)
            )
            favoriteMarkers[fav.id] = marker
            markerFavoriteLookup[marker.id] = fav.id
        } else {
            existing.position = latLng
            markerFavoriteLookup[existing.id] = fav.id
        }
    }

    val currentIds = favorites.map { it.id }.toSet()
    val toRemove = favoriteMarkers.keys.filter { it !in currentIds }
    toRemove.forEach { id ->
        favoriteMarkers[id]?.let { map.removeMarker(it) }
        favoriteMarkers.remove(id)
    }
}

private fun syncMarkedLocationMarkers(
    context: Context,
    map: MapLibreMap,
    markedLocations: List<MarkedLocationWithPhotos>,
    markedMarkers: MutableMap<String, org.maplibre.android.annotations.Marker>,
    markerMarkedLookup: MutableMap<Long, String>
) {
    markedLocations.forEach { item ->
        val loc = item.location
        val latLng = LatLng(loc.latitude, loc.longitude)
        val existing = markedMarkers[loc.id]
        if (existing == null) {
            val icon = IconFactory.getInstance(context).fromBitmap(
                createMarkedLocationBitmap(context)
            )
            val marker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .icon(icon)
                    .title(loc.name)
                    .snippet(loc.description.take(60))
            )
            markedMarkers[loc.id] = marker
            markerMarkedLookup[marker.id] = loc.id
        } else {
            existing.position = latLng
            existing.title = loc.name
            markerMarkedLookup[existing.id] = loc.id
        }
    }

    val currentIds = markedLocations.map { it.location.id }.toSet()
    val toRemove = markedMarkers.keys.filter { it !in currentIds }
    toRemove.forEach { id ->
        markedMarkers[id]?.let { map.removeMarker(it) }
        markedMarkers.remove(id)
    }
}

private fun createMemberBitmap(
    context: Context,
    name: String,
    color: Int,
    rotation: Float,
    drawArrow: Boolean,
    customPinBiasaPath: String?
): Bitmap {
    if (customPinBiasaPath != null) {
        val file = File(customPinBiasaPath)
        if (file.exists()) {
            val raw = android.graphics.BitmapFactory.decodeFile(customPinBiasaPath)
            if (raw != null) {
                val baseDp = if (drawArrow) 46 else 38
                val size = (baseDp * context.resources.displayMetrics.density).roundToInt().coerceAtLeast(baseDp)
                return Bitmap.createScaledBitmap(raw, size, size, true)
            }
        }
    }
    val baseDp = if (drawArrow) 52 else 42
    val size = (baseDp * context.resources.displayMetrics.density).roundToInt().coerceAtLeast(baseDp)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.color = Color.WHITE
    canvas.drawCircle(size / 2f, size / 2f, size * 0.31f, paint)

    paint.color = color
    canvas.drawCircle(size / 2f, size / 2f, size * 0.27f, paint)

    if (drawArrow) {
        canvas.save()
        canvas.rotate(rotation, size / 2f, size / 2f)
        paint.color = color
        val path = android.graphics.Path().apply {
            moveTo(size / 2f, size * 0.02f)
            lineTo(size / 2f - size * 0.15f, size * 0.30f)
            lineTo(size / 2f + size * 0.15f, size * 0.30f)
            close()
        }
        canvas.drawPath(path, paint)
        canvas.restore()
    }

    paint.color = Color.WHITE
    paint.textSize = size * 0.20f
    paint.textAlign = Paint.Align.CENTER
    val displayName = name.trim().split(" ").filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }.ifBlank { "?" }
    canvas.drawText(displayName, size / 2f, size * 0.57f, paint)

    return bitmap
}

private fun createFavoriteBitmap(context: Context, customPinFavoritePath: String?): Bitmap {
    if (customPinFavoritePath != null) {
        val file = File(customPinFavoritePath)
        if (file.exists()) {
            val raw = android.graphics.BitmapFactory.decodeFile(customPinFavoritePath)
            if (raw != null) {
                val baseDp = 34
                val size = (baseDp * context.resources.displayMetrics.density).roundToInt().coerceAtLeast(baseDp)
                return Bitmap.createScaledBitmap(raw, size, size, true)
            }
        }
    }
    val size = (34 * context.resources.displayMetrics.density).roundToInt().coerceAtLeast(34)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.color = Color.parseColor("#B00020")
    paint.textAlign = Paint.Align.CENTER
    paint.textSize = size * 0.88f
    paint.isFakeBoldText = true
    canvas.drawText("♥", size / 2f, size * 0.82f, paint)

    return bitmap
}

/** Teal pin icon for user-marked locations */
private fun createMarkedLocationBitmap(context: Context): Bitmap {
    val dp = 40
    val size = (dp * context.resources.displayMetrics.density).roundToInt().coerceAtLeast(dp)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val pinColor = Color.parseColor("#00796B") // teal

    // Circle body
    paint.color = Color.WHITE
    canvas.drawCircle(size / 2f, size * 0.38f, size * 0.34f, paint)
    paint.color = pinColor
    canvas.drawCircle(size / 2f, size * 0.38f, size * 0.28f, paint)

    // Pin triangle tail
    paint.color = pinColor
    val path = android.graphics.Path().apply {
        moveTo(size * 0.38f, size * 0.62f)
        lineTo(size * 0.62f, size * 0.62f)
        lineTo(size / 2f, size * 0.96f)
        close()
    }
    canvas.drawPath(path, paint)

    // Bookmark icon (simple flag marker)
    paint.color = Color.WHITE
    paint.textAlign = Paint.Align.CENTER
    paint.textSize = size * 0.22f
    canvas.drawText("★", size / 2f, size * 0.45f, paint)

    return bitmap
}

@Composable
private fun FavoriteLocationsPanel(
    favorites: List<FavoriteLocationDto>,
    onRemoveFavorite: (String) -> Unit,
    onFocusFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.heightIn(max = 220.dp),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 8.dp,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Lokasi Favorit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (favorites.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Double tap peta untuk membuka panel ini. Belum ada favorit.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                LazyColumn {
                    items(favorites) { fav ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onFocusFavorite(fav.id) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = fav.label, style = MaterialTheme.typography.bodyLarge)
                                if (fav.address.isNotBlank() && !fav.address.equals("Address unavailable", ignoreCase = true)) {
                                    Text(
                                        text = fav.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "Koordinat: " + String.format(Locale.US, "%.5f", fav.latitude),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format(Locale.US, "%.5f", fav.longitude),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Hapus",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .clickable { onRemoveFavorite(fav.id) }
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun buildOsmRasterStyleJson(): String {
    return """
    {
      "version": 8,
      "sources": {
        "osm-tiles": {
          "type": "raster",
          "tiles": [
            "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
          ],
          "tileSize": 256,
          "attribution": "© OpenStreetMap contributors"
        }
      },
      "layers": [
        {
          "id": "osm-tiles-layer",
          "type": "raster",
          "source": "osm-tiles",
          "minzoom": 0,
          "maxzoom": 19
        }
      ]
    }
    """.trimIndent()
}
