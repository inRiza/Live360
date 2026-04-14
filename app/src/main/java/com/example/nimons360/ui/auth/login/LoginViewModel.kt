package com.example.nimons360.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nimons360.data.remote.dto.common.LoginData
import com.example.nimons360.data.repository.AuthRepository
import com.example.nimons360.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _loginState = MutableStateFlow<Result<LoginData>?>(null)
    val loginState: StateFlow<Result<LoginData>?> = _loginState.asStateFlow()

    fun login(email: String, password: String) {
        // Call AuthRepository
        viewModelScope.launch {
            _loginState.value = Result.Loading
            val result = authRepository.login(email, password)
            _loginState.value = result
        }
    }
}
