package com.devicexpert.app.data.repository

import com.devicexpert.app.BuildConfig
import com.devicexpert.app.domain.model.settings.AuthState
import com.devicexpert.app.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : AuthRepository {

    override val isCloudConfigured: Boolean
        get() = !BuildConfig.SUPABASE_URL.contains("placeholder") &&
            !BuildConfig.SUPABASE_ANON_KEY.contains("placeholder")

    override fun observeAuthState(): Flow<AuthState> =
        supabaseClient.auth.sessionStatus.map { status ->
            when (status) {
                is SessionStatus.Authenticated -> {
                    val user = status.session.user
                    AuthState.SignedIn(userId = user?.id.orEmpty(), email = user?.email ?: "Signed In")
                }
                is SessionStatus.Initializing -> AuthState.Loading
                else -> AuthState.SignedOut
            }
        }

    override suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        check(isCloudConfigured) { "Configure SUPABASE_URL and SUPABASE_ANON_KEY in GitHub Secrets first." }
        supabaseClient.auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
    }

    override suspend fun signUp(email: String, password: String): Result<Unit> = runCatching {
        check(isCloudConfigured) { "Configure SUPABASE_URL and SUPABASE_ANON_KEY in GitHub Secrets first." }
        supabaseClient.auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = password
        }
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        supabaseClient.auth.signOut()
    }
}
