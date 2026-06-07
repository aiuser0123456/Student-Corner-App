package com.studentcorner.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.studentcorner.data.model.ChatCommand
import com.studentcorner.data.model.Resource
import com.studentcorner.data.model.User
import com.studentcorner.util.Result
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) {

    // ── Auth ──────────────────────────────────────────────────────────────────

    val currentUserId: String? get() = auth.currentUser?.uid

    val authStateFlow: Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser != null) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> = try {
        auth.signInWithEmailAndPassword(email, password).await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Sign in failed")
    }

    suspend fun signUpWithEmail(
        username: String,
        email: String,
        password: String,
    ): Result<Unit> = try {
        val credential = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = credential.user!!.uid
        val newUser = mapOf(
            "uid" to uid,
            "username" to username,
            "email" to email,
            "isAdmin" to false,
            "savedResources" to emptyList<String>(),
            "downloadedResources" to emptyList<String>(),
        )
        firestore.collection("users").document(uid).set(newUser).await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Sign up failed")
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = try {
        auth.sendPasswordResetEmail(email).await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Password reset failed")
    }

    fun signOut() = auth.signOut()

    // ── User profile ──────────────────────────────────────────────────────────

    suspend fun getCurrentUser(): Result<User> = try {
        val uid = auth.currentUser?.uid ?: return Result.Error("Not signed in")
        val snap = firestore.collection("users").document(uid).get().await()
        if (snap.exists()) {
            Result.Success(snap.toObject(User::class.java)!!.copy(uid = uid))
        } else {
            Result.Error("User document not found")
        }
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to get user")
    }

    suspend fun updateUsername(newUsername: String): Result<Unit> = try {
        val uid = auth.currentUser?.uid ?: return Result.Error("Not signed in")
        firestore.collection("users").document(uid)
            .update("username", newUsername).await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Update failed")
    }

    // ── Resources ─────────────────────────────────────────────────────────────

    suspend fun getAllResources(): Result<List<Resource>> = try {
        val snap = firestore.collection("resources").get().await()
        val resources = snap.documents.mapNotNull { doc ->
            doc.toObject(Resource::class.java)?.copy(id = doc.id)
        }
        Result.Success(resources)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to fetch resources")
    }

    suspend fun getResourceById(id: String): Result<Resource> = try {
        val doc = firestore.collection("resources").document(id).get().await()
        if (doc.exists()) {
            Result.Success(doc.toObject(Resource::class.java)!!.copy(id = doc.id))
        } else {
            Result.Error("Resource not found")
        }
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to fetch resource")
    }

    // ── Saved resources ───────────────────────────────────────────────────────

    suspend fun getSavedResourceIds(): Result<List<String>> = try {
        val uid = auth.currentUser?.uid ?: return Result.Error("Not signed in")
        val doc = firestore.collection("users").document(uid).get().await()
        @Suppress("UNCHECKED_CAST")
        val saved = doc.get("savedResources") as? List<String> ?: emptyList()
        Result.Success(saved)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to get saved resources")
    }

    suspend fun saveResource(resourceId: String): Result<Unit> = try {
        val uid = auth.currentUser?.uid ?: return Result.Error("Not signed in")
        firestore.collection("users").document(uid)
            .set(mapOf("savedResources" to FieldValue.arrayUnion(resourceId)), SetOptions.merge())
            .await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to save resource")
    }

    suspend fun unsaveResource(resourceId: String): Result<Unit> = try {
        val uid = auth.currentUser?.uid ?: return Result.Error("Not signed in")
        firestore.collection("users").document(uid)
            .update("savedResources", FieldValue.arrayRemove(resourceId))
            .await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to remove saved resource")
    }

    // ── Chat commands (admin-configured auto-responses) ───────────────────────

    suspend fun getAllChatCommands(): Result<List<ChatCommand>> = try {
        val snap = firestore.collection("chatCommands").get().await()
        val commands = snap.documents.mapNotNull { doc ->
            doc.toObject(ChatCommand::class.java)?.copy(id = doc.id)
        }
        Result.Success(commands)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to get chat commands")
    }
}
