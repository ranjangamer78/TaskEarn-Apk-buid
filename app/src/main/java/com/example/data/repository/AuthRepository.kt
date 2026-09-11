package com.example.data.repository

import android.util.Log
import com.example.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun getCurrentUser(): Flow<User?> = callbackFlow {
        var userDocListener: com.google.firebase.firestore.ListenerRegistration? = null
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser == null) {
                userDocListener?.remove()
                trySend(null)
            } else {
                userDocListener?.remove()
                try {
                    userDocListener = db.collection("users").document(firebaseUser.uid)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                Log.w("AuthRepository", "User snapshot listener error: ${error.message}")
                                val fallbackUser = User(
                                    uid = firebaseUser.uid,
                                    email = firebaseUser.email ?: "",
                                    name = firebaseUser.displayName ?: (firebaseUser.email ?: "").substringBefore("@"),
                                    photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                                    balance = 0,
                                    streak = 0,
                                    referralCode = firebaseUser.uid.take(6).uppercase()
                                )
                                trySend(fallbackUser)
                                return@addSnapshotListener
                            }
                            if (snapshot != null && snapshot.exists()) {
                                try {
                                    val user = snapshot.toObject(User::class.java)
                                    trySend(user)
                                } catch (parseEx: Exception) {
                                    Log.w("AuthRepository", "Error parsing user", parseEx)
                                }
                            } else {
                                val user = User(
                                    uid = firebaseUser.uid,
                                    email = firebaseUser.email ?: "",
                                    name = firebaseUser.displayName ?: (firebaseUser.email ?: "").substringBefore("@"),
                                    photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                                    balance = 0,
                                    streak = 0,
                                    referralCode = firebaseUser.uid.take(6).uppercase()
                                )
                                trySend(user)
                            }
                        }
                } catch (e: Exception) {
                    Log.w("AuthRepository", "Error setting up user listener: ${e.message}")
                }
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose { 
            auth.removeAuthStateListener(listener)
            userDocListener?.remove()
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: throw Exception("Sign in failed")

            val user = try {
                val userRef = db.collection("users").document(firebaseUser.uid)
                val snapshot = userRef.get().await()

                if (!snapshot.exists()) {
                    val newUser = User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        name = firebaseUser.displayName ?: "",
                        photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                        balance = 0,
                        streak = 0,
                        joinedAt = System.currentTimeMillis(),
                        referralCode = firebaseUser.uid.take(6).uppercase()
                    )
                    userRef.set(newUser).await()
                    newUser
                } else {
                    snapshot.toObject(User::class.java) ?: User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        name = firebaseUser.displayName ?: ""
                    )
                }
            } catch (firestoreEx: Exception) {
                Log.w("AuthRepository", "Firestore user sync warning: ${firestoreEx.message}")
                User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    name = firebaseUser.displayName ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                    balance = 0,
                    streak = 0,
                    joinedAt = System.currentTimeMillis(),
                    referralCode = firebaseUser.uid.take(6).uppercase()
                )
            }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email.trim(), password.trim()).await()
            val firebaseUser = authResult.user ?: throw Exception("Sign in failed")

            val user = try {
                val userRef = db.collection("users").document(firebaseUser.uid)
                val snapshot = userRef.get().await()

                if (!snapshot.exists()) {
                    val newUser = User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: email.trim(),
                        name = firebaseUser.displayName ?: email.trim().substringBefore("@"),
                        photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                        balance = 0,
                        streak = 0,
                        joinedAt = System.currentTimeMillis(),
                        referralCode = firebaseUser.uid.take(6).uppercase()
                    )
                    userRef.set(newUser).await()
                    newUser
                } else {
                    snapshot.toObject(User::class.java) ?: User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: email.trim(),
                        name = firebaseUser.displayName ?: email.trim().substringBefore("@")
                    )
                }
            } catch (firestoreEx: Exception) {
                Log.w("AuthRepository", "Firestore user sync warning: ${firestoreEx.message}")
                User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: email.trim(),
                    name = firebaseUser.displayName ?: email.trim().substringBefore("@"),
                    photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                    balance = 0,
                    streak = 0,
                    joinedAt = System.currentTimeMillis(),
                    referralCode = firebaseUser.uid.take(6).uppercase()
                )
            }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, password: String, name: String): Result<User> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), password.trim()).await()
            val firebaseUser = authResult.user ?: throw Exception("Sign up failed")

            val displayName = name.trim().ifBlank { email.trim().substringBefore("@") }
            val newUser = User(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: email.trim(),
                name = displayName,
                photoUrl = "",
                balance = 0,
                streak = 0,
                joinedAt = System.currentTimeMillis(),
                referralCode = firebaseUser.uid.take(6).uppercase()
            )
            try {
                val userRef = db.collection("users").document(firebaseUser.uid)
                userRef.set(newUser).await()
            } catch (firestoreEx: Exception) {
                Log.w("AuthRepository", "Firestore user set warning: ${firestoreEx.message}")
            }
            Result.success(newUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInAnonymously(): Result<User> {
        return try {
            val authResult = auth.signInAnonymously().await()
            val firebaseUser = authResult.user ?: throw Exception("Anonymous Sign in failed")

            val userRef = db.collection("users").document(firebaseUser.uid)
            val snapshot = userRef.get().await()

            val user = if (!snapshot.exists()) {
                val newUser = User(
                    uid = firebaseUser.uid,
                    email = "guest@example.com",
                    name = "Guest User",
                    photoUrl = "",
                    balance = 500,
                    streak = 1,
                    joinedAt = System.currentTimeMillis(),
                    referralCode = firebaseUser.uid.take(6).uppercase()
                )
                userRef.set(newUser).await()
                newUser
            } else {
                snapshot.toObject(User::class.java) ?: throw Exception("Failed to parse user")
            }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
