package com.example.nimons360.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nimons360.data.local.preference.TokenPreference
import com.example.nimons360.data.remote.dto.common.UserData
import com.example.nimons360.data.repository.UserRepository
import com.example.nimons360.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val tokenPreference: TokenPreference
) : ViewModel() {

    private val _user = MutableStateFlow<UserData?>(null)
    val user: StateFlow<UserData?> = _user

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _event = MutableSharedFlow<ProfileEvent>()
    val event: SharedFlow<ProfileEvent> = _event

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = userRepository.getProfile()) {
                is Result.Success -> _user.value = result.data
                is Result.Error -> { }
                is Result.Loading -> { /* handle exhaust */ }
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
                    _event.emit(ProfileEvent.UpdateSuccess)
                }
                is Result.Error -> { /* handle jika perlu */ }
                is Result.Loading -> { }
            }
            _isLoading.value = false
        }
    }

    fun signOut() {
        tokenPreference.clear()
        viewModelScope.launch {
            _event.emit(ProfileEvent.SignedOut)
        }
    }
}

sealed class ProfileEvent {
    object SignedOut : ProfileEvent()
    object UpdateSuccess : ProfileEvent()
}
