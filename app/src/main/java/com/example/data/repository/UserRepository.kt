package com.example.data.repository

import com.example.data.model.PaymentMethod
import com.example.data.model.Transaction
import com.example.data.model.User
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import android.util.Log
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class UserRepository {
    private val db = FirebaseFirestore.getInstance()

    fun getUserFlow(uid: String): Flow<User?> = callbackFlow {
        val listener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("UserRepository", "getUserFlow error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toObject(User::class.java)
                    trySend(user)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getPaymentMethodsFlow(): Flow<List<PaymentMethod>> = callbackFlow {
        val listener = db.collection("payment_methods")
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("UserRepository", "getPaymentMethodsFlow error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val methods = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(PaymentMethod::class.java)?.copy(id = doc.id)
                    }
                    trySend(methods)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun getTopUsers(limit: Int = 50): Result<List<User>> {
        return try {
            val snapshot = db.collection("users")
                .orderBy("balance", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            val users = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUser(uid: String): Result<User> {
        return try {
            val snapshot = db.collection("users").document(uid).get().await()
            val user = snapshot.toObject(User::class.java)
            if (user != null) Result.success(user) else Result.failure(Exception("User not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserBalance(uid: String, amount: Int, title: String = "", type: String = "credit", icon: String = "star"): Result<Unit> {
        return try {
            val userRef = db.collection("users").document(uid)
            // Use FieldValue.increment with SetOptions.merge for atomic, 100% reliable balance addition/deduction
            userRef.set(
                mapOf("balance" to FieldValue.increment(amount.toLong())),
                SetOptions.merge()
            ).await()

            if (title.isNotEmpty() && amount != 0) {
                val txRef = db.collection("transactions").document()
                val tx = Transaction(
                    id = txRef.id,
                    userId = uid,
                    title = title,
                    amount = amount,
                    type = type,
                    icon = icon,
                    timestamp = System.currentTimeMillis()
                )
                txRef.set(tx).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "updateUserBalance error", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateDailyStreak(uid: String, newStreak: Int): Result<Unit> {
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "streak" to newStreak,
                    "lastClaimedAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateVideoWatched(uid: String, increment: Int): Result<Unit> {
        return try {
            val userRef = db.collection("users").document(uid)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentCount = snapshot.getLong("videosWatchedToday") ?: 0L
                transaction.update(
                    userRef,
                    "videosWatchedToday", currentCount + increment,
                    "lastVideoWatchedAt", System.currentTimeMillis()
                )
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun resetVideoLimit(uid: String): Result<Unit> {
        return try {
            db.collection("users").document(uid).update("videosWatchedToday", 0).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateSpins(uid: String, increment: Int): Result<Unit> {
        return try {
            val userRef = db.collection("users").document(uid)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentCount = snapshot.getLong("spinsToday") ?: 0L
                transaction.update(
                    userRef,
                    "spinsToday", currentCount + increment,
                    "lastSpinAt", System.currentTimeMillis()
                )
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateScratches(uid: String, increment: Int): Result<Unit> {
        return try {
            val userRef = db.collection("users").document(uid)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentCount = snapshot.getLong("scratchesToday") ?: 0L
                transaction.update(
                    userRef,
                    "scratchesToday", currentCount + increment,
                    "lastScratchAt", System.currentTimeMillis()
                )
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun resetSpinsLimit(uid: String): Result<Unit> {
        return try {
            db.collection("users").document(uid).update("spinsToday", 0).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun resetSpinCooldown(uid: String): Result<Unit> {
        return try {
            db.collection("users").document(uid).update("lastSpinAt", 0L).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetScratchCooldown(uid: String): Result<Unit> {
        return try {
            db.collection("users").document(uid).update("lastScratchAt", 0L).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetVideoCooldown(uid: String): Result<Unit> {
        return try {
            db.collection("users").document(uid).update("lastVideoWatchedAt", 0L).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetGameCooldown(uid: String): Result<Unit> {
        return try {
            db.collection("users").document(uid).update("lastGamePlayedAt", 0L).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordLastGamePlayedAt(uid: String, time: Long = System.currentTimeMillis()): Result<Unit> {
        return try {
            db.collection("users").document(uid).update("lastGamePlayedAt", time).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateGamePlayed(uid: String, increment: Int): Result<Unit> {
        return try {
            val userRef = db.collection("users").document(uid)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentCount = snapshot.getLong("gamesPlayedToday") ?: 0L
                transaction.update(
                    userRef,
                    "gamesPlayedToday", currentCount + increment,
                    "lastGamePlayedAt", System.currentTimeMillis()
                )
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetGamesLimit(uid: String): Result<Unit> {
        return try {
            db.collection("users").document(uid).update("gamesPlayedToday", 0).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun resetScratchesLimit(uid: String): Result<Unit> {
        return try {
            db.collection("users").document(uid).update("scratchesToday", 0).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun addTransaction(transaction: Transaction): Result<Unit> {
        return try {
            val ref = db.collection("transactions").document()
            val newTx = transaction.copy(id = ref.id)
            ref.set(newTx).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTransactions(uid: String): Result<List<Transaction>> {
        return try {
            val snapshot = db.collection("transactions")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            val txs = snapshot.documents.mapNotNull { it.toObject(Transaction::class.java) }
                .sortedByDescending { it.timestamp }
            Result.success(txs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getTransactionsFlow(uid: String): Flow<List<Transaction>> = callbackFlow {
        val listener = db.collection("transactions")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("UserRepository", "getTransactionsFlow error: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val txs = snapshot.documents.mapNotNull { it.toObject(Transaction::class.java) }
                        .sortedByDescending { it.timestamp }
                    trySend(txs)
                }
            }
        awaitClose { listener.remove() }
    }
    
    fun getWithdrawRequestsFlow(uid: String): kotlinx.coroutines.flow.Flow<List<com.example.data.model.WithdrawRequest>> = kotlinx.coroutines.flow.callbackFlow {
        val listener = db.collection("withdraw_requests")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("UserRepository", "getWithdrawRequestsFlow error: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val reqs = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(com.example.data.model.WithdrawRequest::class.java)?.copy(id = doc.id)
                    }.sortedByDescending { it.timestamp }
                    trySend(reqs)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun applyReferralCode(uid: String, code: String, referrerReward: Int, refereeReward: Int): Result<Unit> {
        return try {
            // Find user with this code
            val snapshot = db.collection("users")
                .whereEqualTo("referralCode", code)
                .get()
                .await()
                
            if (snapshot.isEmpty) {
                return Result.failure(Exception("Invalid referral code"))
            }
            
            val referrer = snapshot.documents.first()
            val referrerUid = referrer.id
            
            if (referrerUid == uid) {
                return Result.failure(Exception("You cannot refer yourself"))
            }
            
            // Check if current user already used a code
            val currentUserDoc = db.collection("users").document(uid).get().await()
            val referredBy = currentUserDoc.getString("referredBy")
            if (!referredBy.isNullOrEmpty()) {
                return Result.failure(Exception("You have already used a referral code"))
            }
            
            db.runTransaction { transaction ->
                val referrerRef = db.collection("users").document(referrerUid)
                val currentUserRef = db.collection("users").document(uid)
                val referrerTxRef = db.collection("transactions").document()
                val currentUserTxRef = db.collection("transactions").document()
                
                // ALL READS MUST HAPPEN FIRST
                val referrerSnap = transaction.get(referrerRef)
                val currentUserSnap = transaction.get(currentUserRef)
                
                val referrerBal = referrerSnap.getLong("balance") ?: 0L
                val currentBal = currentUserSnap.getLong("balance") ?: 0L
                
                // THEN ALL WRITES
                val currentInvited = referrerSnap.getLong("invitedCount") ?: 0L
                transaction.update(referrerRef, "balance", referrerBal + referrerReward)
                transaction.update(referrerRef, "invitedCount", currentInvited + 1L)
                transaction.update(currentUserRef, "balance", currentBal + refereeReward)
                transaction.update(currentUserRef, "referredBy", referrerUid)
                
                // Write transactions
                val refTx = Transaction(
                    id = referrerTxRef.id,
                    userId = referrerUid,
                    title = "Referral Reward",
                    amount = referrerReward,
                    type = "credit",
                    icon = "people",
                    timestamp = System.currentTimeMillis()
                )
                transaction.set(referrerTxRef, refTx)
                
                val curTx = Transaction(
                    id = currentUserTxRef.id,
                    userId = uid,
                    title = "Referral Bonus",
                    amount = refereeReward,
                    type = "credit",
                    icon = "people",
                    timestamp = System.currentTimeMillis()
                )
                transaction.set(currentUserTxRef, curTx)
            }.await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun redeemCoupon(uid: String, userEmail: String, rawCode: String): Result<Int> {
        val cleanCode = rawCode.trim().uppercase()
        if (cleanCode.isEmpty()) {
            return Result.failure(Exception("Please enter a valid coupon code"))
        }

        return try {
            val couponRef = db.collection("coupons").document(cleanCode)
            val couponSnap = couponRef.get().await()
            if (!couponSnap.exists()) {
                return Result.failure(Exception("Invalid coupon code! Please check and try again."))
            }

            val isActive = couponSnap.getBoolean("isActive") ?: true
            if (!isActive) {
                return Result.failure(Exception("This coupon code has expired or is deactivated."))
            }

            val maxUses = couponSnap.getLong("maxUses")?.toInt() ?: 0
            val usedCount = couponSnap.getLong("usedCount")?.toInt() ?: 0
            if (maxUses > 0 && usedCount >= maxUses) {
                return Result.failure(Exception("This coupon code has reached its maximum claim limit."))
            }

            // Check if user already redeemed
            val redemptionRef = couponRef.collection("redemptions").document(uid)
            val redemptionSnap = redemptionRef.get().await()
            if (redemptionSnap.exists()) {
                return Result.failure(Exception("You have already redeemed this coupon code!"))
            }

            val reward = couponSnap.getLong("reward")?.toInt() ?: 0
            if (reward <= 0) {
                return Result.failure(Exception("This coupon has no reward configured."))
            }

            // Perform batch update: record redemption, increment usedCount, credit balance, record transaction
            val batch = db.batch()
            batch.set(redemptionRef, mapOf(
                "userId" to uid,
                "userEmail" to userEmail,
                "reward" to reward,
                "redeemedAt" to System.currentTimeMillis()
            ))
            batch.update(couponRef, "usedCount", FieldValue.increment(1))
            batch.set(
                db.collection("users").document(uid),
                mapOf("balance" to FieldValue.increment(reward.toLong())),
                SetOptions.merge()
            )

            val txRef = db.collection("transactions").document()
            val tx = Transaction(
                id = txRef.id,
                userId = uid,
                title = "Coupon Code: $cleanCode",
                amount = reward,
                type = "credit",
                icon = "card",
                timestamp = System.currentTimeMillis()
            )
            batch.set(txRef, tx)

            batch.commit().await()
            Result.success(reward)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error redeeming coupon", e)
            Result.failure(Exception(e.message ?: "Failed to redeem coupon. Please try again."))
        }
    }

    fun getActiveCouponsFlow(): Flow<List<com.example.data.model.Coupon>> = callbackFlow {
        val listener = db.collection("coupons")
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("UserRepository", "getActiveCouponsFlow error: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val coupons = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(com.example.data.model.Coupon::class.java)?.copy(id = doc.id)
                    }
                    trySend(coupons)
                }
            }
        awaitClose { listener.remove() }
    }

    /**
     * Enforces single device policy:
     * - Checks if this physical device ID is already registered to a different account.
     * - If another account is already bound to this device, automatically blocks the current account.
     * - If not, binds device to this UID and records login timestamp.
     */
    suspend fun verifyDeviceAndRecordLogin(context: android.content.Context, uid: String): Result<Boolean> {
        return try {
            val deviceId = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "unknown_device"

            val deviceDocRef = db.collection("device_accounts").document(deviceId)
            val deviceSnap = deviceDocRef.get().await()

            if (deviceSnap.exists()) {
                val boundUid = deviceSnap.getString("boundUid")
                if (!boundUid.isNullOrEmpty() && boundUid != uid) {
                    // MULTIPLE ACCOUNTS DETECTED ON SAME DEVICE!
                    // Automatically block this other account!
                    val blockReason = "Multiple accounts on single device detected (Device: $deviceId). Already bound to $boundUid"
                    db.collection("users").document(uid).update(
                        mapOf(
                            "isBlocked" to true,
                            "blockReason" to blockReason,
                            "deviceId" to deviceId,
                            "lastLogin" to System.currentTimeMillis()
                        )
                    ).await()

                    // Also record violation attempt in device_accounts
                    deviceDocRef.update(
                        "violationAttempts", com.google.firebase.firestore.FieldValue.arrayUnion(uid)
                    ).await()

                    return Result.success(false) // Blocked!
                }
            } else {
                // First account on this device: Bind device
                deviceDocRef.set(
                    mapOf(
                        "deviceId" to deviceId,
                        "boundUid" to uid,
                        "firstBoundAt" to System.currentTimeMillis(),
                        "lastLoginAt" to System.currentTimeMillis()
                    )
                ).await()
            }

            // Record login and device ID on user profile
            db.collection("users").document(uid).update(
                mapOf(
                    "deviceId" to deviceId,
                    "lastLogin" to System.currentTimeMillis()
                )
            ).await()

            Result.success(true)
        } catch (e: Exception) {
            Log.e("UserRepository", "verifyDeviceAndRecordLogin error", e)
            Result.failure(e)
        }
    }

    suspend fun setLeaderboardPrivacy(uid: String, hideName: Boolean): Result<Unit> {
        return try {
            db.collection("users").document(uid).update("hideNameOnLeaderboard", hideName).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
