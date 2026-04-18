package com.example.nimons360.ui.family.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nimons360.data.remote.dto.response.FamilyDetailResponse
import com.example.nimons360.data.repository.FamilyRepository
import com.example.nimons360.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FamilyDetailViewModel @Inject constructor(
    private val repository: FamilyRepository
) : ViewModel() {

    private var currentFamilyId: Int = -1
    private val _familyDetailState = MutableStateFlow<Result<FamilyDetailResponse>>(Result.Loading)
    val familyDetailState = _familyDetailState.asStateFlow()
    private val _actionState = MutableStateFlow<Result<String>?>(null)
    val actionState = _actionState.asStateFlow()

    fun initFamilyId(id: Int) {
        if (currentFamilyId == id) return
        currentFamilyId = id
        loadFamilyDetail()
    }

    private fun loadFamilyDetail() {
        viewModelScope.launch {
            _familyDetailState.value = Result.Loading
            _familyDetailState.value = repository.getFamilyDetail(currentFamilyId)
        }
    }

    fun joinFamily(code: String) {
        viewModelScope.launch {
            _actionState.value = Result.Loading
            val result = repository.joinFamily(currentFamilyId, code)
            if (result is Result.Success && result.data.joined == true) {
                _actionState.value = Result.Success("Berhasil bergabung!")
                loadFamilyDetail() // refresh data ui agar menampilkan daftar anggota asli
            } else if (result is Result.Error) {
                _actionState.value = Result.Error(result.message)
            }
        }
    }

    fun leaveFamily() {
        viewModelScope.launch {
            _actionState.value = Result.Loading
            val result = repository.leaveFamily(currentFamilyId)
            if (result is Result.Success && result.data.left == true) {
                _actionState.value = Result.Success("Berhasil keluar dari keluarga.")
                loadFamilyDetail() // refresh data ui agar kembali nge-blur
            } else if (result is Result.Error) {
                _actionState.value = Result.Error(result.message)
            }
        }
    }

    fun clearActionState() {
        _actionState.value = null
    }
}