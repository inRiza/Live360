package com.example.nimons360.ui.family.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nimons360.data.remote.dto.common.FamilyBasic
import com.example.nimons360.data.remote.dto.common.MyFamilyDetail
import com.example.nimons360.data.repository.FamilyRepository
import com.example.nimons360.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FamilyModel(
    val id: String,
    val name: String,
    val isPinned: Boolean,
    val iconUrl: String
)

@HiltViewModel
class FamilyViewModel @Inject constructor(
    private val repository: FamilyRepository
) : ViewModel() {

    // Setup Variabel State Flow
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _allFamiliesApi = MutableStateFlow<List<FamilyBasic>>(emptyList())
    private val _myFamiliesApi = MutableStateFlow<List<MyFamilyDetail>>(emptyList())

    init {
        fetchFamiliesData()
    }

    // Fetch Families Data
    fun fetchFamiliesData() {
        viewModelScope.launch {
            _isLoading.value = true

            val allRes = repository.getAllFamilies()
            val myRes = repository.getMyFamilies()

            if (allRes is Result.Success) {
                _allFamiliesApi.value = allRes.data
            }
            if (myRes is Result.Success) {
                _myFamiliesApi.value = myRes.data
            }

            _isLoading.value = false
        }
    }

    // Combine dan Filter Data
    val families = combine(
        _allFamiliesApi,
        _myFamiliesApi,
        _selectedFilter,
        _searchQuery,
        repository.getPinnedFamilies()
    ) { all, my, filter, query, pinnedList ->

        val pinnedIds = mutableSetOf<Int>()
        for (pinnedFamily in pinnedList) {
            pinnedIds.add(pinnedFamily.id)
        }

        val baseList = mutableListOf<FamilyModel>()
        // Filter All dan MyFamilies
        if (filter == "All") {
            for (item in all) {
                val familyModel = FamilyModel(
                    id = item.id.toString(),
                    name = item.name ?: "",
                    isPinned = pinnedIds.contains(item.id),
                    iconUrl = item.iconUrl ?: ""
                )
                baseList.add(familyModel)
            }
        } else {
            for (item in my) {
                val familyModel = FamilyModel(
                    id = item.id.toString(),
                    name = item.name ?: "",
                    isPinned = pinnedIds.contains(item.id),
                    iconUrl = item.iconUrl ?: ""
                )
                baseList.add(familyModel)
            }
        }

        // Search Family
        val isQueryEmpty = query.isBlank()

        if (isQueryEmpty) {
            baseList
        } else {
            baseList.filter { family ->
                family.name.contains(query, ignoreCase = true)
            }
        }

    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilter(filter: String) {
        _selectedFilter.value = filter
    }

    // Pinned Family Toggle
    fun togglePin(familyId: String, name: String, iconUrl: String, isCurrentlyPinned: Boolean) {
        viewModelScope.launch {
            val id = familyId.toIntOrNull()

            if (id != null) {
                if (isCurrentlyPinned) {
                    repository.unpinFamily(id)
                } else {
                    repository.pinFamily(id, name, iconUrl)
                }
            }
        }
    }
}