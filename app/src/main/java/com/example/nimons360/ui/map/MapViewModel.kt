package com.example.nimons360.ui.map

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Geocoder
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nimons360.data.remote.dto.common.FavoriteLocationDto
import com.example.nimons360.data.remote.websocket.WebSocketManager
import com.example.nimons360.data.remote.websocket.model.MemberPresencePayload
import com.example.nimons360.data.remote.websocket.model.UpdatePresencePayload
import com.example.nimons360.data.repository.UserRepository
import com.example.nimons360.utils.Result
import com.google.gson.Gson
import com.google.gson.JsonParser
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import java.util.Locale

private const val MEMBER_TIMEOUT_MS = 5_000L
private const val PRESENCE_PUBLISH_INTERVAL_MS = 1_000L
private const val PING_INTERVAL_MS = 15_000L
private const val FAVORITE_PREF_NAME = "map_favorite_locations"
private const val FAVORITE_PREF_KEY = "favorite_locations_json"

data class MemberMapUi(
	val id: String,
	val userId: Int?,
	val fullName: String,
	val email: String,
	val latitude: Double,
	val longitude: Double,
	val rotation: Float,
	val batteryLevel: Int,
	val isCharging: Boolean,
	val internetStatus: String,
	val isCurrentUser: Boolean,
	val lastUpdatedAt: Long
)

data class MapUiState(
	val hasLocationPermission: Boolean = false,
	val isWsConnected: Boolean = false,
	val searchQuery: String = "",
	val selectedMember: MemberMapUi? = null,
	val currentUser: MemberMapUi? = null,
	val remoteMembers: List<MemberMapUi> = emptyList(),
	val nearbyMembers: List<MemberMapUi> = emptyList(),
	val nearbyRadiusMeters: Double = 2_000.0,
	val favoriteLocations: List<FavoriteLocationDto> = emptyList(),
	val isFavoritesPanelVisible: Boolean = false,
	val focusedFavoriteLocationId: String? = null,
	val errorMessage: String? = null
)

@HiltViewModel
class MapViewModel @Inject constructor(
	private val userRepository: UserRepository,
	private val webSocketManager: WebSocketManager,
	@ApplicationContext private val appContext: Context
) : ViewModel() {

	private val gson = Gson()
	private val favoritesPref = appContext.getSharedPreferences(FAVORITE_PREF_NAME, Context.MODE_PRIVATE)

	private val remoteMemberStore = linkedMapOf<String, MemberMapUi>()

	private var currentLatitude: Double? = null
	private var currentLongitude: Double? = null
	private var currentRotation: Float = 0f
	private var lastAcceptedLocationMs: Long = 0L

	private var currentUserName: String = "You"
	private var currentUserEmail: String = "you@nimons.local"
	private var currentUserId: Int? = null

	private var timeoutCleanupJob: Job? = null
	private var periodicPublishJob: Job? = null
	private var pingJob: Job? = null

	private val _uiState = MutableStateFlow(MapUiState())
	val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

	init {
		loadCurrentUserProfile()
		loadFavoriteLocations()
		observeWebSocketEvents()
	}

	fun startRealtime() {
		webSocketManager.connect()
		startMemberTimeoutCleanup()
		startPresencePublishing()
		startPingLoop()
	}

	fun stopRealtime() {
		timeoutCleanupJob?.cancel()
		timeoutCleanupJob = null
		periodicPublishJob?.cancel()
		periodicPublishJob = null
		pingJob?.cancel()
		pingJob = null
		webSocketManager.disconnect()
	}

	fun onLocationPermissionChanged(granted: Boolean) {
		_uiState.update { it.copy(hasLocationPermission = granted) }
	}

	fun onSearchQueryChanged(query: String) {
		_uiState.update { it.copy(searchQuery = query) }
	}

	fun onCurrentLocationChanged(
		latitude: Double,
		longitude: Double,
		accuracyMeters: Float = 50f,
		timestampMs: Long = System.currentTimeMillis()
	) {
		if (currentLatitude != null && accuracyMeters > 60f) return

		val prevLat = currentLatitude
		val prevLng = currentLongitude
		if (prevLat != null && prevLng != null) {
			val distance = distanceMeters(prevLat, prevLng, latitude, longitude)
			val deltaMs = (timestampMs - lastAcceptedLocationMs).coerceAtLeast(1L)

			if (deltaMs < 1500L && distance > 45.0) return

			val alpha = when {
				distance < 3.0 -> 0.15
				distance < 10.0 -> 0.28
				distance < 25.0 -> 0.45
				else -> 0.72
			}
			currentLatitude = prevLat + (latitude - prevLat) * alpha
			currentLongitude = prevLng + (longitude - prevLng) * alpha
		} else {
			currentLatitude = latitude
			currentLongitude = longitude
		}
		lastAcceptedLocationMs = timestampMs
		updateCurrentUserMarker()
	}

	fun onCurrentRotationChanged(rotation: Float) {
		currentRotation = normalizeRotation(rotation)
		updateCurrentUserMarker()
	}

	fun onMemberMarkerClicked(memberId: String) {
		val selected = (_uiState.value.remoteMembers + listOfNotNull(_uiState.value.currentUser))
			.firstOrNull { it.id == memberId }
		_uiState.update { it.copy(selectedMember = selected) }
	}

	fun dismissMemberSheet() {
		_uiState.update { it.copy(selectedMember = null) }
	}

	fun addFavoriteLocation(latitude: Double, longitude: Double) {
		viewModelScope.launch {
			val geocoder = Geocoder(appContext, Locale.getDefault())
			var placeName: String = "Favorite ${(uiState.value.favoriteLocations.size + 1)}"
			var addressText: String = "Address unavailable"

			runCatching {
				val addresses = geocoder.getFromLocation(latitude, longitude, 1)
				val first = addresses?.firstOrNull()
				placeName = first?.featureName?.takeIf { it.isNotBlank() }
					?: first?.subLocality?.takeIf { it.isNotBlank() }
					?: "Favorite ${(uiState.value.favoriteLocations.size + 1)}"
				addressText = first?.getAddressLine(0)?.takeIf { it.isNotBlank() }
					?: "Address unavailable"
			}.onFailure {
				placeName = "Favorite ${(uiState.value.favoriteLocations.size + 1)}"
				addressText = "Address unavailable"
			}

			val favorite = FavoriteLocationDto(
				id = "fav-${System.currentTimeMillis()}",
				label = placeName,
				address = addressText,
				latitude = latitude,
				longitude = longitude,
				createdAt = System.currentTimeMillis()
			)
			val updated = (_uiState.value.favoriteLocations + favorite)
				.distinctBy { it.id }
				.sortedByDescending { it.createdAt }
			_uiState.update { it.copy(favoriteLocations = updated, isFavoritesPanelVisible = true) }
			saveFavoriteLocations(updated)
		}
	}

	fun toggleFavoritesPanel() {
		_uiState.update { it.copy(isFavoritesPanelVisible = !it.isFavoritesPanelVisible) }
	}

	fun focusFavoriteLocation(id: String) {
		_uiState.update { it.copy(focusedFavoriteLocationId = id) }
	}

	fun consumeFocusedFavorite() {
		if (_uiState.value.focusedFavoriteLocationId == null) return
		_uiState.update { it.copy(focusedFavoriteLocationId = null) }
	}

	fun removeFavoriteLocation(id: String) {
		val updated = _uiState.value.favoriteLocations.filterNot { it.id == id }
		_uiState.update {
			it.copy(
				favoriteLocations = updated,
				focusedFavoriteLocationId = if (it.focusedFavoriteLocationId == id) null else it.focusedFavoriteLocationId
			)
		}
		saveFavoriteLocations(updated)
	}

	private fun loadCurrentUserProfile() {
		viewModelScope.launch {
			when (val result = userRepository.getProfile()) {
				is Result.Success -> {
					val profile = result.data
					currentUserName = profile?.fullName.orEmpty().ifBlank { "You" }
					currentUserEmail = profile?.email.orEmpty().ifBlank { "you@nimons.local" }
					currentUserId = profile?.id
				}
				is Result.Error -> {
					_uiState.update { it.copy(errorMessage = result.message) }
				}
				Result.Loading -> Unit
			}
		}
	}

	private fun observeWebSocketEvents() {
		viewModelScope.launch {
			webSocketManager.events.collect { event ->
				when (event) {
					is WebSocketManager.Event.Connected -> {
						_uiState.update { it.copy(isWsConnected = true, errorMessage = null) }
					}
					is WebSocketManager.Event.Disconnected -> {
						_uiState.update {
							it.copy(
								isWsConnected = false,
								errorMessage = if (event.reason.isBlank()) null else event.reason
							)
						}
					}
					is WebSocketManager.Event.Error -> {
						_uiState.update { it.copy(errorMessage = event.message) }
					}
					is WebSocketManager.Event.PresenceReceived -> {
						handleIncomingPresence(event.payload)
					}
				}
			}
		}
	}

	private fun handleIncomingPresence(payload: MemberPresencePayload) {
		if (payload.email.equals(currentUserEmail, ignoreCase = true)) return

		val now = System.currentTimeMillis()
		val memberId = memberIdFrom(payload.userId, payload.email)
		remoteMemberStore[memberId] = payload.toMapUi(now)
		publishRemoteMembersState()
	}

	private fun startMemberTimeoutCleanup() {
		if (timeoutCleanupJob != null) return
		timeoutCleanupJob = viewModelScope.launch {
			while (true) {
				delay(1_000L)
				val now = System.currentTimeMillis()
				val removedIds = remoteMemberStore
					.filterValues { now - it.lastUpdatedAt > MEMBER_TIMEOUT_MS }
					.keys

				if (removedIds.isNotEmpty()) {
					removedIds.forEach { remoteMemberStore.remove(it) }
					if (_uiState.value.selectedMember?.id in removedIds) {
						_uiState.update { it.copy(selectedMember = null) }
					}
					publishRemoteMembersState()
				}
			}
		}
	}

	private fun startPresencePublishing() {
		if (periodicPublishJob != null) return
		periodicPublishJob = viewModelScope.launch {
			while (true) {
				delay(PRESENCE_PUBLISH_INTERVAL_MS)
				publishCurrentPresence()
			}
		}
	}

	private fun startPingLoop() {
		if (pingJob != null) return
		pingJob = viewModelScope.launch {
			while (true) {
				delay(PING_INTERVAL_MS)
				webSocketManager.sendPing()
			}
		}
	}

	private fun publishCurrentPresence() {
		val lat = currentLatitude ?: return
		val lng = currentLongitude ?: return

		val (batteryLevel, isCharging) = readBatteryInfo()
		val networkStatus = readNetworkStatus()

		webSocketManager.sendPresence(
			UpdatePresencePayload(
				name = currentUserName,
				latitude = lat,
				longitude = lng,
				rotation = currentRotation,
				batteryLevel = batteryLevel,
				isCharging = isCharging,
				internetStatus = networkStatus,
				metadata = mapOf(
					"email" to currentUserEmail,
					"userId" to (currentUserId?.toString() ?: ""),
					"source" to "android"
				)
			)
		)
	}

	private fun updateCurrentUserMarker() {
		val lat = currentLatitude ?: return
		val lng = currentLongitude ?: return
		val (batteryLevel, isCharging) = readBatteryInfo()
		val networkStatus = readNetworkStatus()

		val current = MemberMapUi(
			id = memberIdFrom(currentUserId, currentUserEmail),
			userId = currentUserId,
			fullName = currentUserName,
			email = currentUserEmail,
			latitude = lat,
			longitude = lng,
			rotation = currentRotation,
			batteryLevel = batteryLevel,
			isCharging = isCharging,
			internetStatus = networkStatus,
			isCurrentUser = true,
			lastUpdatedAt = System.currentTimeMillis()
		)
		_uiState.update { it.copy(currentUser = current) }
		recomputeNearbyMembers()
	}

	private fun publishRemoteMembersState() {
		_uiState.update {
			it.copy(remoteMembers = remoteMemberStore.values.sortedBy { member -> member.fullName.lowercase() })
		}
		recomputeNearbyMembers()
	}

	private fun recomputeNearbyMembers() {
		val state = _uiState.value
		val current = state.currentUser ?: return
		val nearby = state.remoteMembers
			.map { member -> member to distanceMeters(current.latitude, current.longitude, member.latitude, member.longitude) }
			.filter { (_, distance) -> distance <= state.nearbyRadiusMeters }
			.sortedBy { (_, distance) -> distance }
			.map { (member, _) -> member }

		_uiState.update { it.copy(nearbyMembers = nearby) }
	}

	private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
		val results = FloatArray(1)
		Location.distanceBetween(lat1, lon1, lat2, lon2, results)
		return results[0].toDouble()
	}

	private fun loadFavoriteLocations() {
		val raw = favoritesPref.getString(FAVORITE_PREF_KEY, null)
		if (raw.isNullOrBlank()) return

		runCatching {
			val root = JsonParser.parseString(raw)
			if (!root.isJsonArray) return@runCatching emptyList<FavoriteLocationDto>()
			root.asJsonArray.mapNotNull { element ->
				runCatching {
					val obj = element.asJsonObject
					FavoriteLocationDto(
						id = obj.get("id")?.asString ?: return@runCatching null,
						label = obj.get("label")?.asString ?: "Favorit",
						address = obj.get("address")?.asString ?: "Alamat tidak tersedia",
						latitude = obj.get("latitude")?.asDouble ?: return@runCatching null,
						longitude = obj.get("longitude")?.asDouble ?: return@runCatching null,
						createdAt = obj.get("createdAt")?.asLong ?: System.currentTimeMillis()
					)
				}.getOrNull()
			}
		}.onSuccess { list ->
			_uiState.update { it.copy(favoriteLocations = list.orEmpty()) }
		}
	}

	private fun saveFavoriteLocations(locations: List<FavoriteLocationDto>) {
		val raw = gson.toJson(locations)
		favoritesPref.edit().putString(FAVORITE_PREF_KEY, raw).apply()
	}

	private fun readBatteryInfo(): Pair<Int, Boolean> {
		val batteryIntent = appContext.registerReceiver(
			null,
			IntentFilter(Intent.ACTION_BATTERY_CHANGED)
		) ?: return 0 to false

		val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
		val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
		val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
		val percentage = ((level / scale.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
		val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
			status == BatteryManager.BATTERY_STATUS_FULL
		return percentage to charging
	}

	private fun readNetworkStatus(): String {
		val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
		val network = cm.activeNetwork ?: return "mobile"
		val caps = cm.getNetworkCapabilities(network) ?: return "mobile"

		return when {
			caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
			else -> "mobile"
		}
	}

	private fun memberIdFrom(userId: Int?, email: String): String {
		val safeEmail = email.ifBlank { "unknown" }
		return "${userId ?: -1}:$safeEmail"
	}

	private fun MemberPresencePayload.toMapUi(now: Long): MemberMapUi {
		val safeEmail = email.ifBlank {
			(metadata["email"] as? String).orEmpty()
		}
		val safeName = fullName.ifBlank { (metadata["fullName"] as? String).orEmpty().ifBlank { "Unknown" } }
		return MemberMapUi(
			id = memberIdFrom(userId, safeEmail),
			userId = userId,
			fullName = safeName,
			email = safeEmail.ifBlank { "unknown@nimons.local" },
			latitude = latitude,
			longitude = longitude,
			rotation = normalizeRotation(rotation),
			batteryLevel = batteryLevel.coerceIn(0, 100),
			isCharging = isCharging,
			internetStatus = internetStatus.ifBlank { "mobile" },
			isCurrentUser = false,
			lastUpdatedAt = now
		)
	}

	private fun normalizeRotation(value: Float): Float {
		var result = value % 360f
		if (result < 0f) result += 360f
		return result
	}

	override fun onCleared() {
		stopRealtime()
		super.onCleared()
	}
}
