package com.example.nimons360.ui.auth.login

import androidx.lifecycle.ViewModel
import com.example.nimons360.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel()
