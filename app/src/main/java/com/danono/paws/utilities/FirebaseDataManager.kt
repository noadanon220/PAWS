package com.danono.paws.utilities

import com.danono.paws.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

/**
 * Central Firebase Data Manager
 * - Dogs and their subcollections are nested under users/{uid}/dogs/{dogId}/...
 * - Reminders for calendar/home are stored at root level: users/{uid}/reminders/{reminderId}
 */
class FirebaseDataManager private constructor() {

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        @Volatile
        private var INSTANCE: FirebaseDataManager? = null

        fun getInstance(): FirebaseDataManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseDataManager().also { INSTANCE = it }
            }
        }
    }

    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    private fun getUserDogsCollection(): CollectionReference? {
        return getCurrentUserId()?.let { userId ->
            firestore.collection("users").document(userId).collection("dogs")
        }
    }

    private fun getDogSubCollection(dogId: String, collectionName: String): CollectionReference? {
        return getCurrentUserId()?.let { userId ->
            firestore.collection("users")
                .document(userId)
                .collection("dogs")
                .document(dogId)
                .collection(collectionName)
        }
    }

    // -------------------- DOGS --------------------

    suspend fun addDog(dog: Dog): Result<String> {
        return try {
            val dogsCollection = getUserDogsCollection()
                ?: return Result.failure(Exception("User not logged in"))
            val docRef = dogsCollection.add(dog).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDogs(): Result<List<Pair<Dog, String>>> {
        return try {
            val dogsCollection = getUserDogsCollection()
                ?: return Result.failure(Exception("User not logged in"))
            val snapshot = dogsCollection.get().await()
            val dogs = snapshot.documents.mapNotNull { doc ->
                val dog = doc.toObject(Dog::class.java)
                if (dog != null) Pair(dog, doc.id) else null
            }
            Result.success(dogs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDog(dogId: String, dog: Dog): Result<Unit> {
        return try {
            val dogsCollection = getUserDogsCollection()
                ?: return Result.failure(Exception("User not logged in"))
            dogsCollection.document(dogId).set(dog).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDog(dogId: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))

            val dogRef = firestore.collection("users")
                .document(userId)
                .collection("dogs")
                .document(dogId)

            // Delete subcollections first
            deleteAllSubcollections(dogRef)

            // Delete dog document
            dogRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------- NOTES --------------------

    suspend fun addNote(dogId: String, note: DogNote): Result<Unit> {
        return try {
            val notesCollection = getDogSubCollection(dogId, "notes")
                ?: return Result.failure(Exception("User not logged in"))
            notesCollection.document(note.id).set(note).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNotes(dogId: String): Result<List<DogNote>> {
        return try {
            val notesCollection = getDogSubCollection(dogId, "notes")
                ?: return Result.failure(Exception("User not logged in"))
            val snapshot = notesCollection
                .orderBy("lastModified", Query.Direction.DESCENDING)
                .get().await()
            Result.success(snapshot.toObjects(DogNote::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateNote(dogId: String, note: DogNote): Result<Unit> {
        return try {
            val notesCollection = getDogSubCollection(dogId, "notes")
                ?: return Result.failure(Exception("User not logged in"))
            notesCollection.document(note.id).set(note).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteNote(dogId: String, noteId: String): Result<Unit> {
        return try {
            val notesCollection = getDogSubCollection(dogId, "notes")
                ?: return Result.failure(Exception("User not logged in"))
            notesCollection.document(noteId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------- POOP --------------------

    suspend fun addPoop(dogId: String, poop: DogPoop): Result<Unit> {
        return try {
            val poopCollection = getDogSubCollection(dogId, "poop")
                ?: return Result.failure(Exception("User not logged in"))
            poopCollection.document(poop.id).set(poop).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPoopEntries(dogId: String): Result<List<DogPoop>> {
        return try {
            val poopCollection = getDogSubCollection(dogId, "poop")
                ?: return Result.failure(Exception("User not logged in"))
            val snapshot = poopCollection
                .orderBy("lastModified", Query.Direction.DESCENDING)
                .get().await()
            Result.success(snapshot.toObjects(DogPoop::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePoop(dogId: String, poop: DogPoop): Result<Unit> {
        return try {
            val poopCollection = getDogSubCollection(dogId, "poop")
                ?: return Result.failure(Exception("User not logged in"))
            poopCollection.document(poop.id).set(poop).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePoop(dogId: String, poopId: String): Result<Unit> {
        return try {
            val poopCollection = getDogSubCollection(dogId, "poop")
                ?: return Result.failure(Exception("User not logged in"))
            poopCollection.document(poopId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------- WEIGHTS --------------------

    suspend fun addWeight(dogId: String, weight: DogWeight): Result<Unit> {
        return try {
            val weightsCollection = getDogSubCollection(dogId, "weights")
                ?: return Result.failure(Exception("User not logged in"))
            weightsCollection.document(weight.id).set(weight).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWeights(dogId: String): Result<List<DogWeight>> {
        return try {
            val weightsCollection = getDogSubCollection(dogId, "weights")
                ?: return Result.failure(Exception("User not logged in"))
            val snapshot = weightsCollection
                .orderBy("lastModified", Query.Direction.DESCENDING)
                .get()
                .await()
            Result.success(snapshot.toObjects(DogWeight::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateWeight(dogId: String, weight: DogWeight): Result<Unit> {
        return try {
            val weightsCollection = getDogSubCollection(dogId, "weights")
                ?: return Result.failure(Exception("User not logged in"))
            weightsCollection.document(weight.id).set(weight).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteWeight(dogId: String, weightId: String): Result<Unit> {
        return try {
            val weightsCollection = getDogSubCollection(dogId, "weights")
                ?: return Result.failure(Exception("User not logged in"))
            weightsCollection.document(weightId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun addWeightsListener(
        dogId: String,
        onUpdate: (List<DogWeight>) -> Unit
    ): ListenerRegistration? {
        return getDogSubCollection(dogId, "weights")
            ?.orderBy("lastModified", Query.Direction.DESCENDING)
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseDataManager", "Weights listener error", error)
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(DogWeight::class.java).orEmpty()
                onUpdate(list)
            }
    }


    // -------------------- REMINDERS (root: users/{uid}/reminders) --------------------

    suspend fun getUpcomingReminders(limit: Int = 5): Result<List<Reminder>> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
            val now = System.currentTimeMillis()
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("reminders")
                .whereGreaterThanOrEqualTo("dateTime", now)
                .orderBy("dateTime", Query.Direction.ASCENDING)
                .limit(limit.toLong())
                .get().await()
            Result.success(snapshot.toObjects(Reminder::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveReminderToRoot(reminder: Reminder): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
            firestore.collection("users")
                .document(userId)
                .collection("reminders")
                .document(reminder.id)
                .set(reminder)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateReminderInRoot(reminder: Reminder): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
            firestore.collection("users")
                .document(userId)
                .collection("reminders")
                .document(reminder.id)
                .set(reminder)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteReminderFromRoot(reminderId: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
            firestore.collection("users")
                .document(userId)
                .collection("reminders")
                .document(reminderId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun addRemindersListener(onUpdate: (List<Reminder>) -> Unit): ListenerRegistration? {
        val userId = getCurrentUserId() ?: return null
        return firestore.collection("users")
            .document(userId)
            .collection("reminders")
            .orderBy("dateTime", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseDataManager", "Reminders listener error", error)
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(Reminder::class.java).orEmpty()
                onUpdate(list)
            }
    }

    // -------------------- REAL-TIME LISTENERS (dogs/notes) --------------------

    fun addDogsListener(onUpdate: (List<Pair<Dog, String>>) -> Unit): ListenerRegistration? {
        return getUserDogsCollection()?.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("FirebaseDataManager", "Dogs listener error", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val dogs = snapshot.documents.mapNotNull { doc ->
                    val dog = doc.toObject(Dog::class.java)
                    if (dog != null) Pair(dog, doc.id) else null
                }
                onUpdate(dogs)
            }
        }
    }

    fun addNotesListener(dogId: String, onUpdate: (List<DogNote>) -> Unit): ListenerRegistration? {
        return getDogSubCollection(dogId, "notes")
            ?.orderBy("lastModified", Query.Direction.DESCENDING)
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseDataManager", "Notes listener error", error)
                    return@addSnapshotListener
                }
                val notes = snapshot?.toObjects(DogNote::class.java).orEmpty()
                onUpdate(notes)
            }
    }

    // -------------------- WALKS --------------------

    suspend fun saveWalkCompletion(dogId: String, date: String, walkType: String, isCompleted: Boolean): Result<Unit> {
        return try {
            val walksCollection = getDogSubCollection(dogId, "walks")
                ?: return Result.failure(Exception("User not logged in"))
            val walkData = mapOf(
                "date" to date,
                "walkType" to walkType,
                "isCompleted" to isCompleted,
                "timestamp" to System.currentTimeMillis()
            )
            walksCollection.document("${date}_${walkType}").set(walkData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWalkCompletion(dogId: String, date: String, walkType: String): Result<Boolean> {
        return try {
            val walksCollection = getDogSubCollection(dogId, "walks")
                ?: return Result.failure(Exception("User not logged in"))
            val docRef = walksCollection.document("${date}_${walkType}")
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val isCompleted = snapshot.getBoolean("isCompleted") ?: false
                Result.success(isCompleted)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------- IMAGES --------------------

    suspend fun uploadImage(dogId: String, imageUri: android.net.Uri, folder: String): Result<String> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))
            val imageRef = storage.reference.child("$folder/${userId}/${dogId}/${System.currentTimeMillis()}.jpg")
            imageRef.putFile(imageUri).await()
            val downloadUrl = imageRef.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteImage(imageUrl: String): Result<Unit> {
        return try {
            val imageRef = storage.getReferenceFromUrl(imageUrl)
            imageRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------- INTERNAL UTILS --------------------

    private suspend fun deleteAllSubcollections(dogRef: DocumentReference) {
        val subcollections = listOf("notes", "poop", "weights", "reminders", "walks")
        for (collection in subcollections) {
            try {
                val snapshot = dogRef.collection(collection).get().await()
                for (doc in snapshot.documents) {
                    doc.reference.delete().await()
                }
            } catch (e: Exception) {
                android.util.Log.e("FirebaseDataManager", "Failed to delete $collection", e)
            }
        }
    }
}
