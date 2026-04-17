package com.example.nimons360.ui.family.detail

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class FamilyDetailViewModel @Inject constructor() : ViewModel() {
    private val _isJoined = MutableStateFlow(false)
    val isJoined = _isJoined.asStateFlow()

    fun joinFamily(code: String) {
        // Simulasi sukses API
        _isJoined.value = true
    }

    fun leaveFamily() {
        _isJoined.value = false
    }
}