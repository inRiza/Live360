package com.example.nimons360.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nimons360.data.local.preference.LocationPreference
import com.example.nimons360.data.local.preference.NotificationPreference
import com.example.nimons360.data.local.preference.TokenPreference
import com.example.nimons360.data.remote.dto.common.UserData
import com.example.nimons360.data.repository.NotificationRepository
import com.example.nimons360.data.repository.UserRepository
import com.example.nimons360.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val tokenPreference: TokenPreference,
    private val notificationRepository: NotificationRepository,
    private val notificationPreference: NotificationPreference,
    private val locationPreference: LocationPreference
) : ViewModel() {

    private val _user = MutableStateFlow<UserData?>(null)
    val user: StateFlow<UserData?> = _user

    // Timestamp terakhir profile diupdate — dipakai sebagai Glide cache-busting key
    private val _profileUpdatedAt = MutableStateFlow(System.currentTimeMillis())
    val profileUpdatedAt: StateFlow<Long> = _profileUpdatedAt.asStateFlow()

    // Alias used by the updated ProfileFragment
    private val _profile = MutableStateFlow<Result<UserData>>(Result.Loading)
    val profile: StateFlow<Result<UserData>> = _profile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _uploadState = MutableStateFlow<Result<UserData>?>(null)
    val uploadState: StateFlow<Result<UserData>?> = _uploadState.asStateFlow()

    private val _event = MutableSharedFlow<ProfileEvent>()
    val event: SharedFlow<ProfileEvent> = _event

    val isNotificationEnabled = MutableStateFlow(
        notificationPreference.isNotificationEnabled()
    )

    val isLocationSharingEnabled = MutableStateFlow(
        locationPreference.isLocationSharingEnabled()
    )

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            _profile.value = Result.Loading
            when (val result = userRepository.getProfile()) {
                is Result.Success -> {
                    _user.value = result.data
                    _profile.value = result
                }
                is Result.Error -> {
                    _profile.value = result
                    _event.emit(ProfileEvent.Error(result.message))
                }
                is Result.Loading -> {}
            }
            _isLoading.value = false
        }
    }

    fun updateProfile(fullName: String) {
        if (fullName.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = userRepository.updateProfile(fullName)) {
                is Result.Success -> {
                    _user.value = result.data
                    _profile.value = result
                    _profileUpdatedAt.value = System.currentTimeMillis()
                    com.example.nimons360.utils.ProfileUpdateBus.notifyProfileUpdated()
                    _event.emit(ProfileEvent.UpdateSuccess)
                }
                is Result.Error -> _event.emit(ProfileEvent.Error(result.message))
                is Result.Loading -> {}
            }
            _isLoading.value = false
        }
    }

    /** Alias for updateProfile used by the updated ProfileFragment */
    fun updateName(fullName: String) = updateProfile(fullName)

    fun uploadPhoto(file: File) {
        viewModelScope.launch {
            _uploadState.value = Result.Loading
            when (val result = userRepository.uploadProfilePhoto(file)) {
                is Result.Success -> {
                    _user.value = result.data
                    _profile.value = result
                    _uploadState.value = result
                    _profileUpdatedAt.value = System.currentTimeMillis()
                    com.example.nimons360.utils.ProfileUpdateBus.notifyProfileUpdated()
                    _event.emit(ProfileEvent.PhotoUploaded)
                }
                is Result.Error -> {
                    _uploadState.value = result
                    _event.emit(ProfileEvent.Error(result.message))
                }
                is Result.Loading -> {}
            }
        }
    }

    fun toggleNotification(enabled: Boolean) {
        notificationPreference.setNotificationEnabled(enabled)
        isNotificationEnabled.value = enabled
        viewModelScope.launch {
            if (enabled) {
                com.google.firebase.messaging.FirebaseMessaging.getInstance()
                    .token.addOnSuccessListener { token ->
                        viewModelScope.launch {
                            notificationRepository.subscribeToken(token)
                        }
                    }
            } else {
                notificationRepository.unsubscribeToken()
            }
        }
    }

    fun toggleLocationSharing(enabled: Boolean) {
        locationPreference.setLocationSharingEnabled(enabled)
        isLocationSharingEnabled.value = enabled
    }

    fun signOut() {
        viewModelScope.launch {
            notificationRepository.unsubscribeToken()
            tokenPreference.clear()
            _event.emit(ProfileEvent.SignedOut)
        }
    }

    /** Alias for signOut used by the updated ProfileFragment */
    fun logout() = signOut()
}

sealed class ProfileEvent {
    object SignedOut : ProfileEvent()
    object UpdateSuccess : ProfileEvent()
    object PhotoUploaded : ProfileEvent()
    data class Error(val message: String) : ProfileEvent()
}