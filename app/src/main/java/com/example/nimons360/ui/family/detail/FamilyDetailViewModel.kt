package com.example.nimons360.ui.family.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nimons360.data.remote.dto.response.FamilyDetailResponse
import com.example.nimons360.data.repository.FamilyRepository
import com.example.nimons360.data.repository.UserRepository
import com.example.nimons360.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FamilyDetailViewModel @Inject constructor(
    private val repository: FamilyRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    // State Data
    private var currentFamilyId: Int = -1

    private val _familyDetailState = MutableStateFlow<Result<FamilyDetailResponse>>(Result.Loading)
    val familyDetailState = _familyDetailState.asStateFlow()

    private val _actionState = MutableStateFlow<Result<String>?>(null)
    val actionState = _actionState.asStateFlow()

    private val _currentUserEmail = MutableStateFlow<String?>(null)
    val currentUserEmail = _currentUserEmail.asStateFlow()

    // Inisialisasi
    fun initFamilyId(id: Int) {
        if (currentFamilyId != id) {
            currentFamilyId = id
            loadFamilyDetail()
            loadUserProfile()
        }
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            val result = userRepository.getProfile()
            if (result is Result.Success) {
                _currentUserEmail.value = result.data.email
            }
        }
    }

    private fun loadFamilyDetail() {
        viewModelScope.launch {
            _familyDetailState.value = Result.Loading
            _familyDetailState.value = repository.getFamilyDetail(currentFamilyId)
        }
    }

    // Join Keluarga
    fun joinFamily(code: String) {
        viewModelScope.launch {
            _actionState.value = Result.Loading

            val result = repository.joinFamily(currentFamilyId, code)

            // Cek Hasil
            if (result is Result.Success && result.data.joined == true) {
                _actionState.value = Result.Success("Berhasil bergabung!")
                loadFamilyDetail() // refresh data UI
            } else if (result is Result.Error) {
                _actionState.value = Result.Error(result.message)
            } else {
                _actionState.value = Result.Error("Unknown Error.")
            }
        }
    }

    // Leave Keluarga
    fun leaveFamily() {
        viewModelScope.launch {
            _actionState.value = Result.Loading

            val result = repository.leaveFamily(currentFamilyId)

            // Cek Hasil
            if (result is Result.Success && result.data.left == true) {
                _actionState.value = Result.Success("Berhasil keluar dari keluarga.")
                loadFamilyDetail() // refresh data UI
            } else if (result is Result.Error) {
                _actionState.value = Result.Error(result.message)
            } else {
                _actionState.value = Result.Error("Unknown Error.")
            }
        }
    }

    fun clearActionState() {
        _actionState.value = null
    }
}