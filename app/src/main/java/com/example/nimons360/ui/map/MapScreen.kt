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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.nimons360.data.remote.dto.common.FavoriteLocationDto
import com.example.nimons360.ui.map.components.UserInfoBottomSheet
import com.example.nimons360.ui.map.components.UserMarkerOverlay
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
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
				Lifecycle.Event.ON_START -> viewModel.startRealtime()
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
			onMemberClick = { viewModel.onMemberMarkerClicked(it) },
			onFavoriteLongPress = { lat, lng -> viewModel.addFavoriteLocation(lat, lng) },
			onFavoriteFocused = viewModel::consumeFocusedFavorite,
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

		SnackbarHost(
			hostState = snackbarHostState,
			modifier = Modifier
				.padding(16.dp)
				.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.0f))
		)
	}

	uiState.selectedMember?.let { member ->
		UserInfoBottomSheet(
			member = member,
			onDismiss = viewModel::dismissMemberSheet
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
	onMemberClick: (String) -> Unit,
	onFavoriteLongPress: (Double, Double) -> Unit,
	onFavoriteFocused: () -> Unit,
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
				text = "Map failed to initialize on this device. Please reopen this screen or restart the app.",
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
	val memberMarkers = remember { linkedMapOf<String, org.maplibre.android.annotations.Marker>() }
	val favoriteMarkers = remember { linkedMapOf<String, org.maplibre.android.annotations.Marker>() }

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
				map.uiSettings.isCompassEnabled = true
				map.uiSettings.isRotateGesturesEnabled = true
				map.uiSettings.isTiltGesturesEnabled = true
				map.addOnMapLongClickListener { latLng ->
					onFavoriteLongPress(latLng.latitude, latLng.longitude)
					true
				}
				map.setOnMarkerClickListener { marker ->
					val memberId = markerMemberLookup[marker.id]
					if (memberId != null) {
						onMemberClick(memberId)
						true
					} else {
						val favoriteId = markerFavoriteLookup[marker.id]
						favoriteId != null
					}
				}
			}
			mapView
		},
		update = {
			val map = mapLibreMap ?: return@AndroidView
			if (!styleReady) return@AndroidView
			markerMemberLookup.clear()
			markerFavoriteLookup.clear()

			syncFavoriteMarkers(
				context = context,
				map = map,
				favorites = state.favoriteLocations,
				favoriteMarkers = favoriteMarkers,
				markerFavoriteLookup = markerFavoriteLookup
			)

			state.currentUser?.let { currentUser ->
				syncMemberMarker(
					context = context,
					map = map,
					member = currentUser,
					memberMarkers = memberMarkers,
					markerMemberLookup = markerMemberLookup,
					pinColor = Color.parseColor("#1565C0"),
					drawArrow = true
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
				state.searchQuery.isEmpty() || member.fullName.contains(state.searchQuery, ignoreCase = true)
			}

			filteredRemoteMembers.forEach { member ->
				syncMemberMarker(
					context = context,
					map = map,
					member = member,
					memberMarkers = memberMarkers,
					markerMemberLookup = markerMemberLookup,
					pinColor = Color.parseColor("#E53935"),
					drawArrow = false
				)
			}

			val allMemberIds = filteredRemoteMembers.map { it.id }.toSet() + (state.currentUser?.id ?: "")
			val removedMemberIds = memberMarkers.keys.filter { it !in allMemberIds }
			removedMemberIds.forEach { id ->
				memberMarkers[id]?.let { map.removeMarker(it) }
				memberMarkers.remove(id)
			}

			state.focusedFavoriteLocationId?.let { favId ->
				state.favoriteLocations.find { it.id == favId }?.let { fav ->
					map.animateCamera(
						org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(
							LatLng(fav.latitude, fav.longitude),
							16.5
						)
					)
					onFavoriteFocused()
				}
			}
		}
	)
}

private fun syncMemberMarker(
	context: Context,
	map: MapLibreMap,
	member: MemberMapUi,
	memberMarkers: MutableMap<String, org.maplibre.android.annotations.Marker>,
	markerMemberLookup: MutableMap<Long, String>,
	pinColor: Int,
	drawArrow: Boolean
) {
	val latLng = LatLng(member.latitude, member.longitude)
	val existingMarker = memberMarkers[member.id]

	if (existingMarker == null) {
		val icon = IconFactory.getInstance(context).fromBitmap(
			createMemberBitmap(context, member.fullName, pinColor, member.rotation, drawArrow)
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
			createMemberBitmap(context, member.fullName, pinColor, member.rotation, drawArrow)
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
	markerFavoriteLookup: MutableMap<Long, String>
) {
	favorites.forEach { fav ->
		val latLng = LatLng(fav.latitude, fav.longitude)
		val existing = favoriteMarkers[fav.id]
		if (existing == null) {
			val icon = IconFactory.getInstance(context).fromBitmap(
				createFavoriteBitmap(context)
			)
			val marker = map.addMarker(
				MarkerOptions()
					.position(latLng)
					.icon(icon)
					.title(fav.label)
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

private fun createMemberBitmap(context: Context, name: String, color: Int, rotation: Float, drawArrow: Boolean): Bitmap {
	val size = (48 * context.resources.displayMetrics.density).roundToInt()
	val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
	val canvas = Canvas(bitmap)
	val paint = Paint(Paint.ANTI_ALIAS_FLAG)

	paint.color = color
	canvas.drawCircle(size / 2f, size / 2f, size / 3f, paint)

	paint.color = Color.WHITE
	canvas.drawCircle(size / 2f, size / 2f, size / 4f, paint)

	if (drawArrow) {
		canvas.save()
		canvas.rotate(rotation, size / 2f, size / 2f)
		paint.color = color
		val path = android.graphics.Path().apply {
			moveTo(size / 2f, size / 8f)
			lineTo(size / 2f - size / 10f, size / 3.5f)
			lineTo(size / 2f + size / 10f, size / 3.5f)
			close()
		}
		canvas.drawPath(path, paint)
		canvas.restore()
	}

	paint.color = Color.BLACK
	paint.textSize = size / 5f
	paint.textAlign = Paint.Align.CENTER
	val displayName = if (name.length > 8) name.substring(0, 6) + ".." else name
	canvas.drawText(displayName, size / 2f, size * 0.9f, paint)

	return bitmap
}

private fun createFavoriteBitmap(context: Context): Bitmap {
	val size = (32 * context.resources.displayMetrics.density).roundToInt()
	val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
	val canvas = Canvas(bitmap)
	val paint = Paint(Paint.ANTI_ALIAS_FLAG)

	paint.color = Color.parseColor("#FBC02D")
	val path = android.graphics.Path().apply {
		moveTo(size / 2f, size * 0.1f)
		lineTo(size * 0.65f, size * 0.4f)
		lineTo(size * 0.95f, size * 0.4f)
		lineTo(size * 0.7f, size * 0.6f)
		lineTo(size * 0.8f, size * 0.9f)
		lineTo(size / 2f, size * 0.75f)
		lineTo(size * 0.2f, size * 0.9f)
		lineTo(size * 0.3f, size * 0.6f)
		lineTo(size * 0.05f, size * 0.4f)
		lineTo(size * 0.35f, size * 0.4f)
		close()
	}
	canvas.drawPath(path, paint)

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
		modifier = modifier.height(200.dp),
		shape = RoundedCornerShape(16.dp),
		tonalElevation = 8.dp,
		shadowElevation = 4.dp
	) {
		Column(modifier = Modifier.padding(16.dp)) {
			Text(
				text = "Favorite Locations",
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.Bold,
				modifier = Modifier.padding(bottom = 8.dp)
			)

			if (favorites.isEmpty()) {
				Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
					Text("Long press on map to add favorites", style = MaterialTheme.typography.bodySmall)
				}
			} else {
				LazyColumn {
					items(favorites) { fav ->
						Row(
							modifier = Modifier
								.fillMaxWidth()
								.clickable { onFocusFavorite(fav.id) }
								.padding(vertical = 8.dp),
							verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
						) {
							Column(modifier = Modifier.weight(1f)) {
								Text(text = fav.label, style = MaterialTheme.typography.bodyLarge)
								Text(
									text = String.format(Locale.US, "%.5f, %.5f", fav.latitude, fav.longitude),
									style = MaterialTheme.typography.bodySmall,
									color = MaterialTheme.colorScheme.onSurfaceVariant
								)
							}
							Text(
								text = "Remove",
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
