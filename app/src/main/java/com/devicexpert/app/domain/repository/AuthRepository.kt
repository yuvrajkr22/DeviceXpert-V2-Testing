package com.devicexpert.app.domain.repository

import com.devicexpert.app.domain.model.settings.AuthState
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val isCloudConfigured: Boolean
    fun observeAuthState(): Flow<AuthState>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signUp(email: String, password: String): Result<Unit>
    suspend fun signOut(): Result<Unit>
}
