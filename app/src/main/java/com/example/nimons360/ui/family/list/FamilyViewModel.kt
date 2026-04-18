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

    fun fetchFamiliesData() {
        viewModelScope.launch {
            _isLoading.value = true
            val allRes = repository.getAllFamilies()
            val myRes = repository.getMyFamilies()
            if (allRes is Result.Success) _allFamiliesApi.value = allRes.data
            if (myRes is Result.Success) _myFamiliesApi.value = myRes.data
            _isLoading.value = false
        }
    }

    val families = combine(
        _allFamiliesApi,
        _myFamiliesApi,
        _selectedFilter,
        _searchQuery,
        repository.getPinnedFamilies()
    ) { all, my, filter, query, pinnedList ->

        val pinnedIds = pinnedList.map { it.id }.toSet()

        val baseList = if (filter == "All") {
            all.map {
                FamilyModel(
                    id = it.id.toString(),
                    name = it.name ?: "",
                    isPinned = pinnedIds.contains(it.id),
                    iconUrl = it.iconUrl ?: ""
                )
            }
        } else {
            my.map {
                FamilyModel(
                    id = it.id.toString(),
                    name = it.name ?: "",
                    isPinned = pinnedIds.contains(it.id),
                    iconUrl = it.iconUrl ?: ""
                )
            }
        }

        // Filtering
        if (query.isBlank()) {
            baseList
        } else {
            baseList.filter { it.name.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    fun updateFilter(filter: String) { _selectedFilter.value = filter }

    fun togglePin(familyId: String, name: String, iconUrl: String, isCurrentlyPinned: Boolean) {
        viewModelScope.launch {
            val id = familyId.toIntOrNull() ?: return@launch
            if (isCurrentlyPinned) {
                repository.unpinFamily(id)
            } else {
                repository.pinFamily(id, name, iconUrl)
            }
        }
    }
}