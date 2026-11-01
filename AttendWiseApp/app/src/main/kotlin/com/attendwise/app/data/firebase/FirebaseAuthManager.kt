package com.attendwise.app.data.firebase

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FirebaseAuthManager {

    private val auth: FirebaseAuth by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            FirebaseAuth.getInstance()
        }
    }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    init {
        try {
            _currentUser.value = auth.currentUser
            auth.addAuthStateListener { firebaseAuth ->
                _currentUser.value = firebaseAuth.currentUser
            }
        } catch (e: Exception) {
            // Handled for pre-init environments
        }
    }

    fun clearError() {
        _authError.value = null
    }

    fun signInWithEmail(
        email: String,
        pass: String,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanEmail = if (!email.contains("@")) "$email@university.edu" else email
        auth.signInWithEmailAndPassword(cleanEmail, pass)
            .addOnSuccessListener { result ->
                result.user?.let { onSuccess(it) }
            }
            .addOnFailureListener { ex ->
                // If user doesn't exist yet, attempt automatic sign-up for campus users
                if (ex.message?.contains("no user record") == true || ex.message?.contains("user-not-found") == true) {
                    signUpWithEmail(cleanEmail, pass, onSuccess, onError)
                } else {
                    val msg = ex.localizedMessage ?: "Authentication failed"
                    _authError.value = msg
                    onError(msg)
                }
            }
    }

    fun signUpWithEmail(
        email: String,
        pass: String,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanEmail = if (!email.contains("@")) "$email@university.edu" else email
        auth.createUserWithEmailAndPassword(cleanEmail, pass)
            .addOnSuccessListener { result ->
                result.user?.let { onSuccess(it) }
            }
            .addOnFailureListener { ex ->
                val msg = ex.localizedMessage ?: "Sign up failed"
                _authError.value = msg
                onError(msg)
            }
    }

    suspend fun signInWithGoogle(
        context: Context,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        val serverClientId = try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else "729999642011-brv8higlu5fimssnf8j2s011grs96rre.apps.googleusercontent.com"
        } catch (e: Exception) {
            "729999642011-brv8higlu5fimssnf8j2s011grs96rre.apps.googleusercontent.com"
        }

        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val response = credentialManager.getCredential(context = context, request = request)
            val credential = response.credential

            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)

                auth.signInWithCredential(authCredential)
                    .addOnSuccessListener { result ->
                        result.user?.let { onSuccess(it) }
                    }
                    .addOnFailureListener { ex ->
                        val msg = ex.localizedMessage ?: "Google Sign-In failed"
                        _authError.value = msg
                        onError(msg)
                    }
            } else {
                onError("Unexpected credential type: ${credential.type}")
            }
        } catch (e: GetCredentialException) {
            val msg = e.localizedMessage ?: "Google Sign-In failed or was cancelled"
            _authError.value = msg
            onError(msg)
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "Google authentication error"
            _authError.value = msg
            onError(msg)
        }
    }

    fun signOut() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            // Ignored
        }
    }

    companion object {
        val instance = FirebaseAuthManager()
    }
}
