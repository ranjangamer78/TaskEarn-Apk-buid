package com.example.ui.screens.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.User
import com.example.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LeaderboardViewModel : ViewModel() {
    private val repository = UserRepository()
    private val auth = FirebaseAuth.getInstance()
    
    val currentUserId: String = auth.currentUser?.uid ?: ""

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _isMyNameHidden = MutableStateFlow(false)
    val isMyNameHidden: StateFlow<Boolean> = _isMyNameHidden.asStateFlow()
    
    init {
        loadCurrentUserPrivacy()
        loadLeaderboard()
    }

    private fun loadCurrentUserPrivacy() {
        val uid = currentUserId
        if (uid.isNotEmpty()) {
            viewModelScope.launch {
                repository.getUser(uid).onSuccess { user ->
                    _isMyNameHidden.value = user.hideNameOnLeaderboard
                }
            }
        }
    }

    fun toggleHideName(hidden: Boolean) {
        val uid = currentUserId
        if (uid.isEmpty()) return
        _isMyNameHidden.value = hidden
        viewModelScope.launch {
            repository.setLeaderboardPrivacy(uid, hidden)
            loadLeaderboard()
        }
    }
    
    fun loadLeaderboard() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getTopUsers(10).onSuccess {
                _users.value = it.take(10)
            }.onFailure {
                // handle error
            }
            _isLoading.value = false
        }
    }
}
