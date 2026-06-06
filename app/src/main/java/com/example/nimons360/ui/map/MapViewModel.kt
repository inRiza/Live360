package com.example.nimons360.ui.map

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nimons360.data.local.db.dao.MarkedLocationWithPhotos
import com.example.nimons360.data.local.db.entity.MarkedLocationEntity
import com.example.nimons360.data.local.preference.TokenPreference
import com.example.nimons360.data.local.preference.PinPreference
import com.example.nimons360.data.remote.dto.common.FavoriteLocationDto
import com.example.nimons360.data.remote.websocket.WebSocketManager
import com.example.nimons360.data.remote.websocket.model.MemberPresencePayload
import com.example.nimons360.data.remote.websocket.model.UpdatePresencePayload
import com.example.nimons360.data.repository.FamilyRepository
import com.example.nimons360.data.repository.MarkedLocationRepository
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
private const val PROFILE_AVATAR_BLUE = 0xFF2196F3.toInt()

private val userColorPalette = listOf(
    0xFF4CAF50.toInt(),
    0xFF2196F3.toInt(),
    0xFFE91E63.toInt(),
    0xFFFF9800.toInt(),
    0xFF9C27B0.toInt(),
    0xFF009688.toInt(),
    0xFF795548.toInt()
)

private val familyColorPalette = listOf(
    0xFF0B3D91.toInt(),
    0xFF2E7D32.toInt(),
    0xFFC62828.toInt(),
    0xFF6A1B9A.toInt(),
    0xFFEF6C00.toInt(),
    0xFF00695C.toInt(),
    0xFF455A64.toInt()
)

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
    val familyIds: Set<Int> = emptySet(),
    val lastUpdatedAt: Long
)

data class FamilyFilterOption(
    val id: Int?,
    val name: String,
    val color: Int
)

/** State used to drive the Add/Edit marked location bottom sheet */
data class AddEditMarkedLocationState(
    val id: String? = null,          // null = new location
    val name: String = "",
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val existingPhotoPaths: List<String> = emptyList(),  // already stored photos to keep
    val newPhotoPaths: List<String> = emptyList()        // newly picked/captured photos
)

data class MapLongPressState(
    val latitude: Double,
    val longitude: Double,
    val address: String = "Memuat alamat..."
)

data class MapUiState(
    val hasLocationPermission: Boolean = false,
    val isWsConnected: Boolean = false,
    val searchQuery: String = "",
    val selectedFamilyId: Int? = null,
    val familyOptions: List<FamilyFilterOption> = emptyList(),
    val selectedMember: MemberMapUi? = null,
    val currentUser: MemberMapUi? = null,
    val remoteMembers: List<MemberMapUi> = emptyList(),
    val nearbyMembers: List<MemberMapUi> = emptyList(),
    val nearbyRadiusMeters: Double = 2_000.0,
    val favoriteLocations: List<FavoriteLocationDto> = emptyList(),
    val isFavoritesPanelVisible: Boolean = false,
    val focusedFavoriteLocationId: String? = null,
    // Marked locations (SQLite)
    val markedLocations: List<MarkedLocationWithPhotos> = emptyList(),
    val addEditMarkedLocation: AddEditMarkedLocationState? = null,  // non-null = show sheet
    val selectedMarkedLocation: MarkedLocationWithPhotos? = null,   // non-null = show detail
    val mapLongPressState: MapLongPressState? = null,                // non-null = show map options sheet
    val customPinBiasaPath: String? = null,
    val customPinFavoritePath: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val familyRepository: FamilyRepository,
    private val webSocketManager: WebSocketManager,
    private val markedLocationRepository: MarkedLocationRepository,
    private val tokenPreference: TokenPreference,
    private val pinPreference: PinPreference,
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
    private var myFamilyIds: Set<Int> = emptySet()
    private var isFamilyContextLoaded: Boolean = false

    private var timeoutCleanupJob: Job? = null
    private var periodicPublishJob: Job? = null
    private var pingJob: Job? = null

    // Battery state via BroadcastReceiver
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

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        // Register battery BroadcastReceiver
        appContext.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        // Also do an immediate read to seed initial value
        appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.let { intent ->
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            batteryLevel = ((level / scale.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
        }

        loadCurrentUserProfile()
        loadMyFamilies()
        loadFavoriteLocations()
        observeWebSocketEvents()
        observeMarkedLocations()
        reloadCustomPins()
    }

    // ─── Marked Locations ────────────────────────────────────────────────────

    private fun observeMarkedLocations() {
        viewModelScope.launch {
            markedLocationRepository.getAllWithPhotos().collect { list ->
                _uiState.update { it.copy(markedLocations = list) }
            }
        }
    }

    fun reloadCustomPins() {
        val biasPath = pinPreference.getCustomPinBiasaPath()
        val favPath = pinPreference.getCustomPinFavoritePath()
        _uiState.update {
            it.copy(
                customPinBiasaPath = biasPath,
                customPinFavoritePath = favPath
            )
        }
    }

    /** Called when user long-presses the map */
    fun onMapLongPressed(latitude: Double, longitude: Double) {
        _uiState.update {
            it.copy(
                mapLongPressState = MapLongPressState(
                    latitude = latitude,
                    longitude = longitude,
                    address = "Memuat alamat..."
                )
            )
        }

        viewModelScope.launch {
            val geocoder = android.location.Geocoder(appContext, Locale.getDefault())
            var addressText: String = "Alamat tidak tersedia"
            runCatching {
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val first = addresses?.firstOrNull()
                addressText = first?.getAddressLine(0)?.takeIf { it.isNotBlank() } ?: "Alamat tidak tersedia"
            }.onFailure {
                addressText = "Alamat tidak tersedia"
            }

            _uiState.update { state ->
                val longPress = state.mapLongPressState ?: return@update state
                if (longPress.latitude == latitude && longPress.longitude == longitude) {
                    state.copy(mapLongPressState = longPress.copy(address = addressText))
                } else {
                    state
                }
            }
        }
    }

    fun dismissMapLongPress() {
        _uiState.update { it.copy(mapLongPressState = null) }
    }

    fun selectAddMarkedLocation() {
        val longPress = _uiState.value.mapLongPressState ?: return
        _uiState.update {
            it.copy(
                mapLongPressState = null,
                addEditMarkedLocation = AddEditMarkedLocationState(
                    latitude = longPress.latitude,
                    longitude = longPress.longitude
                )
            )
        }
    }

    fun selectSaveAsFavorite() {
        val longPress = _uiState.value.mapLongPressState ?: return
        _uiState.update { it.copy(mapLongPressState = null) }
        addFavoriteLocation(longPress.latitude, longPress.longitude)
    }

    /** Called to pre-fill lat/lng with current user location */
    fun useCurrentLocationForMarked() {
        val lat = currentLatitude ?: return
        val lng = currentLongitude ?: return
        _uiState.update { state ->
            val sheet = state.addEditMarkedLocation ?: return@update state
            state.copy(addEditMarkedLocation = sheet.copy(latitude = lat, longitude = lng))
        }
    }

    fun updateAddEditName(name: String) {
        _uiState.update { state ->
            val sheet = state.addEditMarkedLocation ?: return@update state
            state.copy(addEditMarkedLocation = sheet.copy(name = name))
        }
    }

    fun updateAddEditDescription(desc: String) {
        _uiState.update { state ->
            val sheet = state.addEditMarkedLocation ?: return@update state
            state.copy(addEditMarkedLocation = sheet.copy(description = desc))
        }
    }

    fun updateAddEditLatitude(lat: Double) {
        _uiState.update { state ->
            val sheet = state.addEditMarkedLocation ?: return@update state
            state.copy(addEditMarkedLocation = sheet.copy(latitude = lat))
        }
    }

    fun updateAddEditLongitude(lng: Double) {
        _uiState.update { state ->
            val sheet = state.addEditMarkedLocation ?: return@update state
            state.copy(addEditMarkedLocation = sheet.copy(longitude = lng))
        }
    }

    fun addNewPhotosToAddEdit(paths: List<String>) {
        _uiState.update { state ->
            val sheet = state.addEditMarkedLocation ?: return@update state
            state.copy(addEditMarkedLocation = sheet.copy(newPhotoPaths = sheet.newPhotoPaths + paths))
        }
    }

    fun removeExistingPhotoFromAddEdit(path: String) {
        _uiState.update { state ->
            val sheet = state.addEditMarkedLocation ?: return@update state
            state.copy(addEditMarkedLocation = sheet.copy(existingPhotoPaths = sheet.existingPhotoPaths - path))
        }
    }

    fun removeNewPhotoFromAddEdit(path: String) {
        _uiState.update { state ->
            val sheet = state.addEditMarkedLocation ?: return@update state
            state.copy(addEditMarkedLocation = sheet.copy(newPhotoPaths = sheet.newPhotoPaths - path))
        }
    }

    fun saveMarkedLocation() {
        val sheet = _uiState.value.addEditMarkedLocation ?: return
        if (sheet.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Nama lokasi tidak boleh kosong") }
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (sheet.id == null) {
                // New
                val entity = MarkedLocationEntity(
                    id = "ml-$now",
                    name = sheet.name.trim(),
                    description = sheet.description.trim(),
                    latitude = sheet.latitude,
                    longitude = sheet.longitude,
                    createdAt = now,
                    updatedAt = now
                )
                markedLocationRepository.insert(entity, sheet.newPhotoPaths)
            } else {
                // Edit
                val entity = MarkedLocationEntity(
                    id = sheet.id,
                    name = sheet.name.trim(),
                    description = sheet.description.trim(),
                    latitude = sheet.latitude,
                    longitude = sheet.longitude,
                    createdAt = now, // will be overwritten by existing createdAt if needed
                    updatedAt = now
                )
                markedLocationRepository.update(entity, sheet.existingPhotoPaths, sheet.newPhotoPaths)
            }
            _uiState.update { it.copy(addEditMarkedLocation = null) }
        }
    }

    fun dismissAddEditSheet() {
        _uiState.update { it.copy(addEditMarkedLocation = null) }
    }

    fun onMarkedLocationClicked(id: String) {
        val found = _uiState.value.markedLocations.firstOrNull { it.location.id == id }
        _uiState.update { it.copy(selectedMarkedLocation = found) }
    }

    fun dismissMarkedLocationDetail() {
        _uiState.update { it.copy(selectedMarkedLocation = null) }
    }

    fun openEditMarkedLocation(item: MarkedLocationWithPhotos) {
        _uiState.update {
            it.copy(
                selectedMarkedLocation = null,
                addEditMarkedLocation = AddEditMarkedLocationState(
                    id = item.location.id,
                    name = item.location.name,
                    description = item.location.description,
                    latitude = item.location.latitude,
                    longitude = item.location.longitude,
                    existingPhotoPaths = item.photos.map { p -> p.filePath },
                    newPhotoPaths = emptyList()
                )
            )
        }
    }

    fun deleteMarkedLocation(id: String) {
        viewModelScope.launch {
            markedLocationRepository.delete(id)
            _uiState.update { state ->
                state.copy(
                    selectedMarkedLocation = if (state.selectedMarkedLocation?.location?.id == id) null
                    else state.selectedMarkedLocation
                )
            }
        }
    }

    // ─── Realtime / WebSocket ────────────────────────────────────────────────

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

    fun onFamilyFilterChanged(familyId: Int?) {
        _uiState.update { it.copy(selectedFamilyId = familyId) }
        recomputeNearbyMembers()
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
                distance < 2.0 -> 0.45
                distance < 8.0 -> 0.75
                else -> 1.0
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

    // ─── Favorite Locations (SharedPrefs – unchanged) ─────────────────────

    fun addFavoriteLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val geocoder = android.location.Geocoder(appContext, Locale.getDefault())
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

    // ─── Private helpers ─────────────────────────────────────────────────────

    private fun loadCurrentUserProfile() {
        viewModelScope.launch {
            when (val result = userRepository.getProfile()) {
                is Result.Success -> {
                    val profile = result.data
                    currentUserName = profile?.fullName.orEmpty().ifBlank { "You" }
                    currentUserEmail = profile?.email.orEmpty().ifBlank { "you@nimons.local" }
                    currentUserId = profile?.id
                    tokenPreference.saveUserName(currentUserName)
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
        if (currentUserId != null && payload.userId == currentUserId) return

        val incomingEmail = payload.extractEmail()
        if (incomingEmail.equals(currentUserEmail, ignoreCase = true)) return
        if (!sharesFamilyWithCurrentUser(payload)) return

        val now = System.currentTimeMillis()
        val memberId = memberIdFrom(payload.userId, incomingEmail)
        val incoming = payload.toMapUi(now)
        val existing = remoteMemberStore[memberId]
        remoteMemberStore[memberId] = if (existing == null) {
            incoming
        } else {
            val distance = distanceMeters(existing.latitude, existing.longitude, incoming.latitude, incoming.longitude)
            val alpha = when {
                distance < 2.0 -> 0.35
                distance < 10.0 -> 0.65
                else -> 1.0
            }
            incoming.copy(
                latitude = existing.latitude + (incoming.latitude - existing.latitude) * alpha,
                longitude = existing.longitude + (incoming.longitude - existing.longitude) * alpha,
                rotation = smoothRotation(existing.rotation, incoming.rotation, alpha.toFloat()),
                lastUpdatedAt = now
            )
        }
        publishRemoteMembersState()
    }

    private fun sharesFamilyWithCurrentUser(payload: MemberPresencePayload): Boolean {
        if (!isFamilyContextLoaded) return true
        if (myFamilyIds.isEmpty()) return true

        val peerFamilyIds = payload.extractFamilyIdsFromMetadata()
        if (peerFamilyIds.isEmpty()) return true

        return peerFamilyIds.any { it in myFamilyIds }
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
                    "familyIds" to myFamilyIds.sorted(),
                    "familyCount" to myFamilyIds.size,
                    "source" to "android"
                )
            )
        )
    }

    private fun loadMyFamilies() {
        viewModelScope.launch {
            when (val result = familyRepository.getMyFamilies()) {
                is Result.Success -> {
                    val families = result.data.orEmpty().mapNotNull { family ->
                        val familyId = family.id ?: return@mapNotNull null
                        FamilyFilterOption(
                            id = familyId,
                            name = family.name.orEmpty().ifBlank { "Family $familyId" },
                            color = familyColorFor(familyId)
                        )
                    }
                    myFamilyIds = families.mapNotNull { it.id }.toSet()
                    val selectedFamilyId = _uiState.value.selectedFamilyId
                    val retainedSelection = if (selectedFamilyId != null && selectedFamilyId !in myFamilyIds) null else selectedFamilyId
                    _uiState.update {
                        it.copy(
                            familyOptions = listOf(FamilyFilterOption(null, "Semua Familyku", PROFILE_AVATAR_BLUE)) + families,
                            selectedFamilyId = retainedSelection
                        )
                    }
                    isFamilyContextLoaded = true
                    recomputeNearbyMembers()
                }
                is Result.Error -> {
                    isFamilyContextLoaded = false
                    _uiState.update {
                        it.copy(
                            familyOptions = listOf(FamilyFilterOption(null, "Semua Familyku", PROFILE_AVATAR_BLUE)),
                            selectedFamilyId = null
                        )
                    }
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun updateCurrentUserMarker() {
        val lat = currentLatitude ?: return
        val lng = currentLongitude ?: return
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
            familyIds = myFamilyIds,
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
            .filter { member -> memberMatchesSelectedFamily(member, state.selectedFamilyId) }
            .map { member -> member to distanceMeters(current.latitude, current.longitude, member.latitude, member.longitude) }
            .filter { (_, distance) -> distance <= state.nearbyRadiusMeters }
            .sortedBy { (_, distance) -> distance }
            .map { (member, _) -> member }

        _uiState.update { it.copy(nearbyMembers = nearby) }
    }

    private fun memberMatchesSelectedFamily(member: MemberMapUi, selectedFamilyId: Int?): Boolean {
        if (selectedFamilyId == null) return true
        return selectedFamilyId in member.familyIds
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

    private fun MemberPresencePayload.extractEmail(): String {
        if (email.isNotBlank()) return email
        return (metadata["email"] as? String).orEmpty()
    }

    private fun MemberPresencePayload.extractFamilyIdsFromMetadata(): Set<Int> {
        val idsFromList = (metadata["familyIds"] as? List<*>)
            .orEmpty()
            .mapNotNull { item ->
                when (item) {
                    is Number -> item.toInt()
                    is String -> item.toIntOrNull()
                    else -> null
                }
            }
            .filter { it > 0 }
            .toSet()

        if (idsFromList.isNotEmpty()) return idsFromList

        val single = when (val familyId = metadata["familyId"]) {
            is Number -> familyId.toInt()
            is String -> familyId.toIntOrNull()
            else -> null
        }
        return if (single != null && single > 0) setOf(single) else emptySet()
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
            familyIds = extractFamilyIdsFromMetadata(),
            lastUpdatedAt = now
        )
    }

    private fun familyColorFor(familyId: Int): Int {
        val index = kotlin.math.abs(familyId) % familyColorPalette.size
        return familyColorPalette[index]
    }

    private fun userColorFor(member: MemberMapUi): Int {
        val key = member.email.ifBlank { member.id }
        val hash = kotlin.math.abs(key.hashCode())
        return userColorPalette[hash % userColorPalette.size]
    }

    fun resolveMarkerColor(member: MemberMapUi): Int {
        val state = _uiState.value
        val selectedFamilyId = state.selectedFamilyId
        return when {
            selectedFamilyId != null -> state.familyOptions.firstOrNull { it.id == selectedFamilyId }?.color ?: PROFILE_AVATAR_BLUE
            member.isCurrentUser -> PROFILE_AVATAR_BLUE
            else -> userColorFor(member)
        }
    }

    private fun normalizeRotation(value: Float): Float {
        var result = value % 360f
        if (result < 0f) result += 360f
        return result
    }

    private fun smoothRotation(previous: Float, target: Float, alpha: Float): Float {
        val shortestDelta = (((target - previous + 540f) % 360f) - 180f)
        return normalizeRotation(previous + shortestDelta * alpha)
    }

    override fun onCleared() {
        appContext.unregisterReceiver(batteryReceiver)
        stopRealtime()
        super.onCleared()
    }
}
