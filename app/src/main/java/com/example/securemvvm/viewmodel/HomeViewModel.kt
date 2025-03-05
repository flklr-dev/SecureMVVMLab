package com.example.securemvvm.viewmodel

import androidx.lifecycle.ViewModel
import com.example.securemvvm.model.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    fun logout() {
        userRepository.clearAuthData() // Clear session data
    }
} 