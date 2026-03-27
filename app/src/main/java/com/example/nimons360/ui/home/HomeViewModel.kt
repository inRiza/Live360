package com.example.nimons360.ui.home

import androidx.lifecycle.ViewModel
import com.example.nimons360.data.repository.FamilyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val familyRepository: FamilyRepository
) : ViewModel()
