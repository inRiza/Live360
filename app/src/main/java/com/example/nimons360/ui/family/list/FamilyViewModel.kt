package com.example.nimons360.ui.family.list

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

// Model data sederhana untuk daftar keluarga
data class FamilyModel(
    val id: String,
    val name: String,
    val isPinned: Boolean,
    val bgColor: Color
)

@HiltViewModel
class FamilyViewModel @Inject constructor() : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter = _selectedFilter.asStateFlow()

    // Data dummy
    private val _families = MutableStateFlow(
        listOf(
            FamilyModel("1", "Maulana Family", true, Color(0xFFFFEBEE)),
            FamilyModel("2", "Study Group", true, Color(0xFFE3F2FD)),
            FamilyModel("3", "Camping Trip", false, Color(0xFFE8F5E9)),
            FamilyModel("4", "Neighborhood Watch", false, Color(0xFFFFF8E1)),
            FamilyModel("5", "ITB 2022 Batch", false, Color(0xFFEDE7F6))
        )
    )
    val families = _families.asStateFlow()

    fun updateSearchQuery(query: String) { _searchQuery.value = query }

    fun updateFilter(filter: String) { _selectedFilter.value = filter }

    fun togglePin(familyId: String) {
        _families.value = _families.value.map {
            if (it.id == familyId) it.copy(isPinned = !it.isPinned) else it
        }
    }
}