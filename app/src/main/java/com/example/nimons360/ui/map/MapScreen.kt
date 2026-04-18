package com.example.nimons360.ui.map

import android.animation.ValueAnimator
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
import android.location.Location
import android.os.Looper
import android.view.animation.LinearInterpolator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
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
	var mapboxMap by remember { mutableStateOf<MapboxMap?>(null) }
	var styleReady by remember { mutableStateOf(false) }
	var didMoveToCurrentUser by remember { mutableStateOf(false) }
	val markerMemberLookup = remember { mutableStateMapOf<Long, String>() }
	val markerFavoriteLookup = remember { mutableStateMapOf<Long, String>() }
	val memberMarkers = remember { linkedMapOf<String, com.mapbox.mapboxsdk.annotations.Marker>() }
	val favoriteMarkers = remember { linkedMapOf<String, com.mapbox.mapboxsdk.annotations.Marker>() }

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
				mapboxMap = map
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
			val map = mapboxMap ?: return@AndroidView
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
						com.mapbox.mapboxsdk.camera.CameraUpdateFactory.newLatLngZoom(
							LatLng(currentUser.latitude, currentUser.longitude),
							15.5
						)
					)
					didMoveToCurrentUser = true
				}
			}

			val filteredRemoteMembers = state.remoteMembers.filter { member ->
				state.searchQuery.isBlank() ||
					member.fullName.contains(state.searchQuery, ignoreCase = true) ||
					member.email.contains(state.searchQuery, ignoreCase = true)
			}
			val nearbyIds = state.nearbyMembers.map { it.id }.toSet()
			val remoteFilteredIds = filteredRemoteMembers.map { it.id }.toSet()

			filteredRemoteMembers.forEach { member ->
				val color = if (member.id in nearbyIds) {
					Color.parseColor("#EF6C00")
				} else if (member.batteryLevel < 20) {
					Color.parseColor("#C62828")
				} else {
					Color.parseColor("#2E7D32")
				}
				syncMemberMarker(
					context = context,
					map = map,
					member = member,
					memberMarkers = memberMarkers,
					markerMemberLookup = markerMemberLookup,
					pinColor = color,
					drawArrow = false
				)
			}

			val keepIds = remoteFilteredIds + setOfNotNull(state.currentUser?.id)
			val removedIds = memberMarkers.keys.filterNot { it in keepIds }
			removedIds.forEach { id ->
				memberMarkers[id]?.let { marker -> map.removeMarker(marker) }
				memberMarkers.remove(id)
			}

			val focusedId = state.focusedFavoriteLocationId
			if (focusedId != null) {
				val focused = state.favoriteLocations.firstOrNull { it.id == focusedId }
				if (focused != null) {
					map.animateCamera(
						com.mapbox.mapboxsdk.camera.CameraUpdateFactory.newLatLngZoom(
							LatLng(focused.latitude, focused.longitude),
							16.0
						)
					)
				}
				onFavoriteFocused()
			}
		}
	)
}

private fun syncFavoriteMarkers(
	context: Context,
	map: MapboxMap,
	favorites: List<FavoriteLocationDto>,
	favoriteMarkers: MutableMap<String, com.mapbox.mapboxsdk.annotations.Marker>,
	markerFavoriteLookup: MutableMap<Long, String>
) {
	val iconFactory = IconFactory.getInstance(context)
	favorites.forEach { favorite ->
		val marker = favoriteMarkers[favorite.id]
		if (marker == null) {
			val newMarker = map.addMarker(
				MarkerOptions()
					.position(LatLng(favorite.latitude, favorite.longitude))
					.title(favorite.label)
					.snippet(favorite.address)
					.icon(iconFactory.fromBitmap(buildFavoritePinBitmap()))
			)
			favoriteMarkers[favorite.id] = newMarker
			markerFavoriteLookup[newMarker.id] = favorite.id
		} else {
			markerFavoriteLookup[marker.id] = favorite.id
		}
	}

	val removedIds = favoriteMarkers.keys.filterNot { id -> favorites.any { it.id == id } }
	removedIds.forEach { id ->
		favoriteMarkers[id]?.let { marker -> map.removeMarker(marker) }
		favoriteMarkers.remove(id)
	}
}

private fun syncMemberMarker(
	context: Context,
	map: MapboxMap,
	member: MemberMapUi,
	memberMarkers: MutableMap<String, com.mapbox.mapboxsdk.annotations.Marker>,
	markerMemberLookup: MutableMap<Long, String>,
	pinColor: Int,
	drawArrow: Boolean
) {
	val existing = memberMarkers[member.id]
	if (existing == null) {
		val marker = map.addMarker(
			MarkerOptions()
				.position(LatLng(member.latitude, member.longitude))
				.title(member.fullName)
				.snippet(member.email)
				.icon(
					IconFactory.getInstance(context).fromBitmap(
						buildUserPinBitmap(
							context = context,
							initials = initials(member.fullName),
							backgroundColor = pinColor,
							rotation = member.rotation,
							drawArrow = drawArrow
						)
					)
				)
		)
		memberMarkers[member.id] = marker
		markerMemberLookup[marker.id] = member.id
		return
	}

	animateMarkerTo(existing, LatLng(member.latitude, member.longitude))
	markerMemberLookup[existing.id] = member.id
}

private fun animateMarkerTo(
	marker: com.mapbox.mapboxsdk.annotations.Marker,
	target: LatLng
) {
	val start = marker.position
	if (start.latitude == target.latitude && start.longitude == target.longitude) return

	ValueAnimator.ofFloat(0f, 1f).apply {
		duration = 850L
		interpolator = LinearInterpolator()
		addUpdateListener { animator ->
			val fraction = animator.animatedFraction
			val lat = start.latitude + (target.latitude - start.latitude) * fraction
			val lng = start.longitude + (target.longitude - start.longitude) * fraction
			marker.position = LatLng(lat, lng)
		}
		start()
	}
}

private fun drawFavoriteMarkers(
	context: Context,
	map: MapboxMap,
	favorites: List<FavoriteLocationDto>,
	markerFavoriteLookup: MutableMap<Long, String>
) {
	val iconFactory = IconFactory.getInstance(context)
	favorites.forEach { favorite ->
		val marker = map.addMarker(
			MarkerOptions()
				.position(LatLng(favorite.latitude, favorite.longitude))
				.title(favorite.label)
				.icon(iconFactory.fromBitmap(buildFavoritePinBitmap()))
		)
		markerFavoriteLookup[marker.id] = favorite.id
	}
}

private fun buildFavoritePinBitmap(): Bitmap {
	val size = 56
	val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
	val canvas = Canvas(bitmap)

	val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = Color.parseColor("#F9A825")
		style = Paint.Style.FILL
	}
	canvas.drawCircle(size / 2f, size / 2f, size / 2.3f, circlePaint)

	val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = Color.WHITE
		textAlign = Paint.Align.CENTER
		textSize = size * 0.46f
		isFakeBoldText = true
	}
	canvas.drawText("★", size / 2f, size * 0.67f, starPaint)

	return bitmap
}

private fun buildUserPinBitmap(
	context: Context,
	initials: String,
	backgroundColor: Int,
	rotation: Float,
	drawArrow: Boolean
): Bitmap {
	val density = context.resources.displayMetrics.density
	val size = (56 * density).roundToInt().coerceAtLeast(56)
	val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
	val canvas = Canvas(bitmap)

	val circleRadius = size * 0.28f
	val centerX = size / 2f
	val centerY = size / 2f

	if (drawArrow) {
		val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
			color = Color.parseColor("#0D47A1")
			style = Paint.Style.FILL
		}
		canvas.save()
		canvas.rotate(rotation, centerX, centerY)
		val arrowPath = android.graphics.Path().apply {
			moveTo(centerX, centerY - circleRadius - size * 0.2f)
			lineTo(centerX - size * 0.08f, centerY - circleRadius + size * 0.02f)
			lineTo(centerX + size * 0.08f, centerY - circleRadius + size * 0.02f)
			close()
		}
		canvas.drawPath(arrowPath, arrowPaint)
		canvas.restore()
	}

	val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = Color.argb(75, 0, 0, 0)
		style = Paint.Style.FILL
	}
	canvas.drawCircle(centerX, centerY + size * 0.04f, circleRadius + 4f, shadowPaint)

	val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = backgroundColor
		style = Paint.Style.FILL
	}
	canvas.drawCircle(centerX, centerY, circleRadius, circlePaint)

	val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = Color.WHITE
		textAlign = Paint.Align.CENTER
		textSize = size * 0.22f
		isFakeBoldText = true
	}
	canvas.drawText(initials, centerX, centerY + size * 0.08f, textPaint)
	return bitmap
}

private fun initials(name: String): String {
	if (name.isBlank()) return "?"
	return name.trim().split(" ")
		.filter { it.isNotBlank() }
		.take(2)
		.joinToString(separator = "") { token -> token.first().uppercase() }
}

@Composable
private fun FavoriteLocationsPanel(
	favorites: List<FavoriteLocationDto>,
	onRemoveFavorite: (String) -> Unit,
	onFocusFavorite: (String) -> Unit,
	modifier: Modifier = Modifier
) {
	Surface(
		modifier = modifier,
		shape = RoundedCornerShape(16.dp),
		color = ComposeColor.White.copy(alpha = 0.96f),
		shadowElevation = 4.dp
	) {
		Column(modifier = Modifier.padding(12.dp)) {
			Text(
				text = "Lokasi Favorit (Lokal)",
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.Bold
			)
			Text(
				text = "Disimpan di SharedPreferences: map_favorite_locations",
				style = MaterialTheme.typography.bodySmall,
				color = ComposeColor(0xFF546E7A)
			)
			Spacer(modifier = Modifier.height(8.dp))

			if (favorites.isEmpty()) {
				Text(
					text = "Belum ada lokasi favorit. Tekan lama pada peta untuk menambahkan.",
					style = MaterialTheme.typography.bodyMedium,
					color = ComposeColor(0xFF455A64)
				)
			} else {
				LazyColumn(modifier = Modifier.height(220.dp)) {
					items(favorites, key = { it.id }) { favorite ->
						Row(
							modifier = Modifier
								.fillMaxWidth()
								.padding(vertical = 6.dp)
								.clickable { onFocusFavorite(favorite.id) },
						) {
							Column(modifier = Modifier.weight(1f)) {
								Text(text = favorite.label, fontWeight = FontWeight.SemiBold)
								Text(
									text = favorite.address,
									style = MaterialTheme.typography.bodySmall,
									color = ComposeColor(0xFF546E7A)
								)
								Text(
									text = "${String.format(Locale.US, "%.5f", favorite.latitude)}, ${String.format(Locale.US, "%.5f", favorite.longitude)}",
									style = MaterialTheme.typography.bodySmall,
									color = ComposeColor(0xFF455A64)
								)
							}
							Text(
								text = "Hapus",
								color = ComposeColor(0xFFC62828),
								fontWeight = FontWeight.SemiBold,
								modifier = Modifier
									.padding(start = 8.dp)
									.clickable { onRemoveFavorite(favorite.id) }
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
	  "name": "Nimons OSM Raster",
	  "sources": {
	    "osm": {
	      "type": "raster",
	      "tiles": [
	        "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
	      ],
	      "tileSize": 256,
	      "attribution": "© OpenStreetMap contributors",
	      "minzoom": 0,
	      "maxzoom": 19
	    }
	  },
	  "layers": [
	    {
	      "id": "osm-layer",
	      "type": "raster",
	      "source": "osm",
	      "minzoom": 0,
	      "maxzoom": 22
	    }
	  ]
	}
	""".trimIndent()
}
