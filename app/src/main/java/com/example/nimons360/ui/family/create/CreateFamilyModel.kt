package com.example.nimons360.ui.family.create

import androidx.lifecycle.ViewModel
import com.example.nimons360.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CreateFamilyViewModel : ViewModel() {

    // All Family Icons
    val availableIcons = listOf(
        R.drawable.family_icon_1,
        R.drawable.family_icon_2,
        R.drawable.family_icon_3,
        R.drawable.family_icon_4,
        R.drawable.family_icon_5,
        R.drawable.family_icon_6,
        R.drawable.family_icon_7,
        R.drawable.family_icon_8
    )

    private val _selectedIcon = MutableStateFlow(availableIcons[0])
    val selectedIcon: StateFlow<Int> = _selectedIcon

    fun setSelectedIcon(iconResId: Int) {
        _selectedIcon.value = iconResId
    }
}