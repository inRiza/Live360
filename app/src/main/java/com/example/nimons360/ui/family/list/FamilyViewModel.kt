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
    val id: Int,
    val name: String,
    val isPinned: Boolean,
    val iconUrl: String
)

@HiltViewModel
class FamilyViewModel @Inject constructor(
    private val repository: FamilyRepository
) : ViewModel() {

    companion object {
        private const val FETCH_SIZE = 10
        private const val SEARCH_DEBOUNCE_MS = 300L
    }

    // Setup Variabel State Flow
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore = _isLoadingMore.asStateFlow()

    private val _currentSize = MutableStateFlow(1)

    private val _allFamiliesApi = MutableStateFlow<List<FamilyBasic>>(emptyList())
    private val _myFamiliesApi = MutableStateFlow<List<MyFamilyDetail>>(emptyList())
    private val _debouncedQuery = MutableStateFlow("")

    init {
        fetchFamiliesData()
        // debounce search query
        viewModelScope.launch {
            _searchQuery
                .debounce(SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collect { query ->
                    _debouncedQuery.value = query
                    _currentSize.value = 1
                }
        }
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

    fun loadNextPage() {
        if (_isLoadingMore.value || isLoading.value) return

        val currentFiltered = filteredFamilies()
        val currentShown = _currentSize.value * FETCH_SIZE

        if (currentShown >= currentFiltered.size) return // all showed

        _isLoadingMore.value = true

        viewModelScope.launch {
            kotlinx.coroutines.delay(200) // delay
            _currentSize.value += 1
            _isLoadingMore.value = false
        }
    }

    // semua families after filter sebelum load selanjutnya
    private fun filteredFamilies(): List<FamilyModel> {
        return families.value
    }

    // Combine dan Filter Data
    val families = combine(
        _allFamiliesApi,
        _myFamiliesApi,
        _selectedFilter,
        _debouncedQuery,
        repository.getPinnedFamilies()
    ) { all, my, filter, query, pinnedList ->

        val pinnedIds = mutableSetOf<Int>()
        for (pinnedFamily in pinnedList) {
            pinnedIds.add(pinnedFamily.id)
        }

        val baseList = if (filter == "All") {
            all.map { item ->
                FamilyModel(
                    id = item.id ?: 0,
                    name = item.name ?: "",
                    isPinned = pinnedIds.contains(item.id),
                    iconUrl = item.iconUrl ?: ""
                )
            }
        } else {
            my.map { item ->
                FamilyModel(
                    id = item.id ?: 0,
                    name = item.name ?: "",
                    isPinned = pinnedIds.contains(item.id),
                    iconUrl = item.iconUrl ?: ""
                )
            }
        }

        if (query.isBlank()) baseList
        else baseList.filter {
            it.name.contains(query, ignoreCase = true)
        }

    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // families that are shown
    val fetchedFamilies = combine(families, _currentSize) { all, size ->
        all.take(size * FETCH_SIZE)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val hasMore = combine(families, _currentSize) { all, size ->
        all.size > size * FETCH_SIZE
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilter(filter: String) {
        _selectedFilter.value = filter
        _currentSize.value = 1 // reset after filter change
    }

    // Pinned Family Toggle
    fun togglePin(familyId: Int, name: String, iconUrl: String, isCurrentlyPinned: Boolean) {
        viewModelScope.launch {
            if (isCurrentlyPinned) {
                repository.unpinFamily(familyId)
            } else {
                repository.pinFamily(familyId, name, iconUrl)
            }
        }
    }
}