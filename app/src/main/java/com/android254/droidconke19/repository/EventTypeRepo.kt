package com.android254.droidconke19.repository

import com.android254.droidconke19.datastates.Result
import com.android254.droidconke19.datastates.runCatching
import com.android254.droidconke19.models.EventTypeModel
import com.android254.droidconke19.utils.await
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObjects

interface EventTypeRepo {
    suspend fun getSessionData(): Result<List<EventTypeModel>>
}

class EventTypeRepoImpl(val firestore: FirebaseFirestore) : EventTypeRepo {

    override suspend fun getSessionData(): Result<List<EventTypeModel>> =
            runCatching {
                val snapshots = firestore.collection("event_types")
                        .orderBy("id", Query.Direction.ASCENDING)
                        .get()
                        .await()
                snapshots.toObjects<EventTypeModel>()

            }

}
