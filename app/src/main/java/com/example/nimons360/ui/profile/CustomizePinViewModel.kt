package com.example.nimons360.ui.profile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nimons360.data.local.preference.PinInfo
import com.example.nimons360.data.local.preference.PinPreference
import com.example.nimons360.service.PinDownloadService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomizePinUiState(
    val pins: List<PinInfo> = emptyList(),
    val activeBiasaPinId: String? = null,
    val activeFavoritePinId: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class CustomizePinViewModel @Inject constructor(
    private val pinPreference: PinPreference,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomizePinUiState())
    val uiState: StateFlow<CustomizePinUiState> = _uiState.asStateFlow()

    private val staticPins = listOf(
        PinInfo("lizard", "Lizard", "https://mad.labpro.hmif.dev/assets/pin/lizard.png"),
        PinInfo("moon", "Moon", "https://mad.labpro.hmif.dev/assets/pin/moon.png"),
        PinInfo("redpin", "Red Pin", "https://mad.labpro.hmif.dev/assets/pin/redpin.png"),
        PinInfo("smile", "Smile", "https://mad.labpro.hmif.dev/assets/pin/smile.png"),
        PinInfo("star", "Star", "https://mad.labpro.hmif.dev/assets/pin/star.png")
    )

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != PinDownloadService.ACTION_PIN_DOWNLOAD_PROGRESS) return
            val pinId = intent.getStringExtra(PinDownloadService.EXTRA_PIN_ID) ?: return
            val progress = intent.getIntExtra(PinDownloadService.EXTRA_PROGRESS, 0)
            val status = intent.getStringExtra(PinDownloadService.EXTRA_STATUS) ?: return
            val localPath = intent.getStringExtra(PinDownloadService.EXTRA_LOCAL_PATH)
            val error = intent.getStringExtra(PinDownloadService.EXTRA_ERROR)

            handleDownloadProgress(pinId, progress, status, localPath, error)
        }
    }

    init {
        // Register download receiver
        val filter = IntentFilter(PinDownloadService.ACTION_PIN_DOWNLOAD_PROGRESS)
        appContext.registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED)

        loadPinsState()
    }

    private fun loadPinsState() {
        viewModelScope.launch {
            val updated = staticPins.map { pin ->
                val localPath = pinPreference.getDownloadedPinPath(pin.id)
                pin.copy(
                    localPath = localPath,
                    isDownloaded = localPath != null
                )
            }

            // Find active pin IDs
            val activeBiasaPath = pinPreference.getCustomPinBiasaPath()
            val activeFavoritePath = pinPreference.getCustomPinFavoritePath()

            val activeBiasaId = updated.firstOrNull { it.localPath == activeBiasaPath }?.id
            val activeFavId = updated.firstOrNull { it.localPath == activeFavoritePath }?.id

            _uiState.update {
                it.copy(
                    pins = updated,
                    activeBiasaPinId = activeBiasaId,
                    activeFavoritePinId = activeFavId
                )
            }
        }
    }

    fun downloadPin(pin: PinInfo) {
        // Start download service
        _uiState.update { state ->
            val updated = state.pins.map {
                if (it.id == pin.id) it.copy(isDownloading = true, downloadProgress = 0) else it
            }
            state.copy(pins = updated)
        }
        PinDownloadService.start(appContext, pin.id, pin.name, pin.url)
    }

    fun selectPinForBiasa(pin: PinInfo) {
        val state = _uiState.value
        if (state.activeFavoritePinId == pin.id) {
            showToast("Pin ini sudah digunakan untuk Favorite Location! Pilih pin yang berbeda.")
            return
        }

        val filename = "custom_pins/${pin.id}.png"
        pinPreference.saveCustomPinBiasaFilename(filename)

        _uiState.update { it.copy(activeBiasaPinId = pin.id) }
        showToast("${pin.name} diterapkan untuk Pin Biasa")
    }

    fun selectPinForFavorite(pin: PinInfo) {
        val state = _uiState.value
        if (state.activeBiasaPinId == pin.id) {
            showToast("Pin ini sudah digunakan untuk Pin Biasa! Pilih pin yang berbeda.")
            return
        }

        val filename = "custom_pins/${pin.id}.png"
        pinPreference.saveCustomPinFavoriteFilename(filename)

        _uiState.update { it.copy(activeFavoritePinId = pin.id) }
        showToast("${pin.name} diterapkan untuk Favorite Location")
    }

    fun resetBiasa() {
        pinPreference.saveCustomPinBiasaFilename(null)
        _uiState.update { it.copy(activeBiasaPinId = null) }
        showToast("Pin Biasa dikembalikan ke default")
    }

    fun resetFavorite() {
        pinPreference.saveCustomPinFavoriteFilename(null)
        _uiState.update { it.copy(activeFavoritePinId = null) }
        showToast("Favorite Location dikembalikan ke default")
    }

    private fun handleDownloadProgress(
        pinId: String,
        progress: Int,
        status: String,
        localPath: String?,
        error: String?
    ) {
        _uiState.update { state ->
            val updated = state.pins.map { pin ->
                if (pin.id == pinId) {
                    when (status) {
                        "downloading" -> pin.copy(isDownloading = true, downloadProgress = progress)
                        "success" -> pin.copy(
                            isDownloading = false,
                            isDownloaded = true,
                            localPath = localPath,
                            downloadProgress = 100
                        )
                        "error" -> pin.copy(isDownloading = false, downloadProgress = 0)
                        else -> pin
                    }
                } else pin
            }

            var activeBiasaId = state.activeBiasaPinId
            var activeFavId = state.activeFavoritePinId
            if (status == "success" && localPath != null) {
                // Check if this was previously selected (if path matches)
                val activeBiasaPath = pinPreference.getCustomPinBiasaPath()
                val activeFavoritePath = pinPreference.getCustomPinFavoritePath()
                if (localPath == activeBiasaPath) activeBiasaId = pinId
                if (localPath == activeFavoritePath) activeFavId = pinId
            }

            if (status == "error" && error != null) {
                showToast("Download gagal: $error")
            }

            state.copy(
                pins = updated,
                activeBiasaPinId = activeBiasaId,
                activeFavoritePinId = activeFavId
            )
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
    }

    override fun onCleared() {
        runCatching {
            appContext.unregisterReceiver(downloadReceiver)
        }
        super.onCleared()
    }
}