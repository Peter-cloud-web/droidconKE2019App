package com.android254.droidconke19.repository

import com.android254.droidconke19.database.AppDatabase
import com.android254.droidconke19.database.dao.SessionsDao
import com.android254.droidconke19.datastates.Result
import com.android254.droidconke19.datastates.runCatching
import com.android254.droidconke19.models.SessionsModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await

interface SessionDataRepo {
    suspend fun getSessionData(dayNumber: String, sessionId: Int): Result<SessionsModel>

    suspend fun getStarredSessions(userId: String): Result<List<String>>

    suspend fun starrSession(dayNumber: String, sessionId: Int, userId: String, slug: String): Result<String>

    suspend fun unstarrSession(dayNumber: String, sessionId: Int, userId: String): Result<String>

    suspend fun clearStarredSessions(userId: String)
}

class SessionDataRepoImpl(db: AppDatabase, private val firestore: FirebaseFirestore) : SessionDataRepo {
    private val sessionsDao: SessionsDao = db.sessionsDao()

    private val starredSessionCollection = "starred_sessions"

    override suspend fun getSessionData(dayNumber: String, sessionId: Int): Result<SessionsModel> =
            runCatching {
                val snapshot = firestore.collection(dayNumber)
                        .whereEqualTo("id", sessionId)
                        .get()
                        .await()
                val doc = snapshot.documents.first()
                val sessionsModel = doc.toObject(SessionsModel::class.java)
                val newSessionsModel = sessionsModel?.copy(documentId = doc.id)
                newSessionsModel!!

            }

    private suspend fun isSessionStarred(dayNumber: String, sessionId: Int, userId: String): Boolean {
        return try {
            val snapshot = firestore.collection(starredSessionCollection)
                    .whereEqualTo("day", dayNumber)
                    .whereEqualTo("session_id", sessionId)
                    .whereEqualTo("user_id", userId)
                    .get()
                    .await()
            !snapshot.isEmpty
        } catch (e: FirebaseFirestoreException) {
            false
        }
    }

    override suspend fun getStarredSessions(userId: String): Result<List<String>> =
            runCatching {
                val snapshot = firestore.collection(starredSessionCollection)
                        .whereEqualTo("user_id", userId)
                        .get()
                        .await()
                val slugs = mutableListOf<String>()
                if (snapshot.isEmpty) {
                    println("No starred sessions found")
                } else {
                    println("Found ${snapshot.size()} starred session(s)")
                }
                snapshot.forEach {
                    val slug = it["slug"] as String?
                    slug?.let {
                        slugs.add(it)
                    } ?: run {
                        println("No slug found")
                    }

                }
                slugs
            }

    override suspend fun starrSession(dayNumber: String, sessionId: Int, userId: String, slug: String): Result<String> {
        return if(!isSessionStarred(dayNumber, sessionId, userId)) {
            runCatching {
                val data = hashMapOf(
                        "day" to dayNumber,
                        "session_id" to sessionId,
                        "user_id" to userId,
                        "starred" to true,
                        "slug" to slug
                )

                val starredSessionRef = firestore.collection(starredSessionCollection).document()
                starredSessionRef.set(data).await()
                "Session added to favourites"
            }
        } else {
            Result.Error("Session already starred")
        }
    }

    override suspend fun unstarrSession(dayNumber: String, sessionId: Int, userId: String): Result<String> =
            runCatching {
                val snapshot = firestore.collection(starredSessionCollection)
                        .whereEqualTo("day", dayNumber)
                        .whereEqualTo("session_id", sessionId)
                        .whereEqualTo("user_id", userId)
                        .get()
                        .await()
                if (!snapshot.isEmpty) {
                    snapshot.documents.first().reference.delete().await()
                }
                "Session removed from favourites"
            }

    override suspend fun clearStarredSessions(userId: String) {
        val snapshot = firestore.collection(starredSessionCollection)
                .whereEqualTo("user_id", userId)
                .get()
                .await()
        val batch = firestore.batch()
        snapshot.forEach {
            batch.delete(it.reference)
        }
        batch.commit().await()
    }
}
