package com.example.nimons360.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nimons360.data.remote.dto.common.FamilyDiscover
import com.example.nimons360.data.remote.dto.common.MyFamilyDetail
import com.example.nimons360.data.repository.FamilyRepository
import com.example.nimons360.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val familyRepository: FamilyRepository
) : ViewModel() {

    private val _myFamilies = MutableStateFlow<List<MyFamilyDetail>>(emptyList())
    val myFamilies: StateFlow<List<MyFamilyDetail>> = _myFamilies

    private val _discoverFamilies = MutableStateFlow<List<FamilyDiscover>>(emptyList())
    val discoverFamilies: StateFlow<List<FamilyDiscover>> = _discoverFamilies

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true

            when (val result = familyRepository.getMyFamilies()) {
                is Result.Success -> _myFamilies.value = result.data ?: emptyList()
                is Result.Error -> _error.value = result.message
                is Result.Loading -> { /* required */ }
            }

            when (val result = familyRepository.discoverFamilies()) {
                is Result.Success -> _discoverFamilies.value = result.data ?: emptyList()
                is Result.Error -> _error.value = result.message
                is Result.Loading -> { /* required */ }
            }

            _isLoading.value = false
        }
    }
}
