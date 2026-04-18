package com.example.nimons360.ui.family.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nimons360.R
import com.example.nimons360.data.remote.dto.response.FamilyDetailResponse
import com.example.nimons360.data.repository.FamilyRepository
import com.example.nimons360.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateFamilyViewModel @Inject constructor(
    private val familyRepository: FamilyRepository
) : ViewModel() {

    val availableIcons = listOf(
        R.drawable.family_icon_1, R.drawable.family_icon_2,
        R.drawable.family_icon_3, R.drawable.family_icon_4,
        R.drawable.family_icon_5, R.drawable.family_icon_6,
        R.drawable.family_icon_7, R.drawable.family_icon_8
    )

    private val _selectedIcon = MutableStateFlow(availableIcons[0])
    val selectedIcon: StateFlow<Int> = _selectedIcon
    private val _createState = MutableStateFlow<Result<FamilyDetailResponse>?>(null)
    val createState: StateFlow<Result<FamilyDetailResponse>?> = _createState

    fun setSelectedIcon(iconResId: Int) {
        _selectedIcon.value = iconResId
    }

    fun createFamily(familyName: String) {
        if (familyName.isBlank()) {
            _createState.value = Result.Error("Nama keluarga tidak boleh kosong")
            return
        }

        viewModelScope.launch {
            _createState.value = Result.Loading

            val iconIndex = availableIcons.indexOf(_selectedIcon.value) + 1
            val iconUrl = "https://mad.labpro.hmif.dev/assets/family_icon_$iconIndex.png"

            val result = familyRepository.createFamily(familyName, iconUrl)
            _createState.value = result
        }
    }
}