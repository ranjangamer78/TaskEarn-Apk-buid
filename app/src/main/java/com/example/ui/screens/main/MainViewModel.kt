package com.example.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.User
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.catch
import android.util.Log

import com.example.data.repository.ConfigRepository
import com.example.data.repository.AppConfig

class MainViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val configRepository: ConfigRepository = ConfigRepository(),
    private val userRepository: com.example.data.repository.UserRepository = com.example.data.repository.UserRepository()
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _appConfig = MutableStateFlow<AppConfig>(AppConfig())
    val appConfig: StateFlow<AppConfig> = _appConfig.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.getCurrentUser()
                .catch { e -> Log.w("MainViewModel", "Error in user flow: ${e.message}") }
                .collectLatest {
                    _user.value = it
                }
        }
        viewModelScope.launch {
            configRepository.getConfigFlow()
                .catch { e -> Log.w("MainViewModel", "Error in config flow: ${e.message}") }
                .collectLatest {
                    _appConfig.value = it
                }
        }
    }

    fun checkDeviceAndRecordLogin(context: android.content.Context, uid: String) {
        viewModelScope.launch {
            userRepository.verifyDeviceAndRecordLogin(context, uid)
        }
    }

    fun signOut() {
        authRepository.signOut()
    }
}
