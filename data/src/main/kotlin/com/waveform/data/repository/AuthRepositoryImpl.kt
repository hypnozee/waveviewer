package com.waveform.data.repository

import android.util.Log
import com.waveform.domain.core.Result
import com.waveform.domain.model.AuthState
import com.waveform.domain.model.UserInfo
import com.waveform.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Supabase Auth-backed implementation of [AuthRepository].
 */
class AuthRepositoryImpl(
    private val client: SupabaseClient,
) : AuthRepository {

    companion object {
        private const val TAG = "AuthRepository"
    }

    override suspend fun signIn(email: String, password: String): Result<UserInfo> {
        return withContext(Dispatchers.IO) {
            try {
                client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                val user = client.auth.currentUserOrNull()
                    ?: return@withContext Result.Error("Sign in succeeded but no user returned.")
                Result.Success(
                    UserInfo(
                        id = user.id,
                        email = user.email,
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Sign in failed", e)
                Result.Error("Sign in failed: ${e.message}")
            }
        }
    }

    override suspend fun signUp(email: String, password: String): Result<UserInfo> {
        return withContext(Dispatchers.IO) {
            try {
                client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
                // User won't have an active session until OTP is confirmed.
                // Return placeholder so the caller knows sign-up was initiated.
                Result.Success(UserInfo(id = "", email = email))
            } catch (e: Exception) {
                Log.e(TAG, "Sign up failed", e)
                Result.Error("Sign up failed: ${e.message}")
            }
        }
    }

    override suspend fun verifyOtp(email: String, token: String): Result<UserInfo> {
        return withContext(Dispatchers.IO) {
            try {
                client.auth.verifyEmailOtp(
                    type = OtpType.Email.SIGNUP,
                    email = email,
                    token = token,
                )
                val user = client.auth.currentUserOrNull()
                    ?: return@withContext Result.Error("OTP verified but no session was created.")
                Result.Success(UserInfo(id = user.id, email = user.email))
            } catch (e: Exception) {
                Log.e(TAG, "OTP verification failed", e)
                Result.Error("OTP verification failed: ${e.message}")
            }
        }
    }

    override suspend fun signOut() {
        try {
            client.auth.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Sign out failed", e)
        }
    }

    override fun getCurrentUser(): UserInfo? {
        val user = client.auth.currentUserOrNull() ?: return null
        return UserInfo(
            id = user.id,
            email = user.email,
        )
    }

    override fun observeAuthState(): Flow<AuthState> {
        return client.auth.sessionStatus.map { status ->
            when (status) {
                is SessionStatus.Authenticated -> {
                    val user = status.session.user
                        ?: return@map AuthState.NotAuthenticated
                    AuthState.Authenticated(
                        UserInfo(
                            id = user.id,
                            email = user.email,
                        )
                    )
                }
                is SessionStatus.NotAuthenticated -> AuthState.NotAuthenticated
                is SessionStatus.Initializing -> AuthState.Loading
                is SessionStatus.RefreshFailure -> AuthState.NotAuthenticated
            }
        }
    }
}
