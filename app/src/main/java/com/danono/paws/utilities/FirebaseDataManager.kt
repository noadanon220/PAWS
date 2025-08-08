package com.danono.paws.utilities

import com.danono.paws.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

/**
 * Central Firebase Data Manager
 * Manages all Firebase operations with proper data hierarchy:
 * users/{userId}/dogs/{dogId}/{collection}/{documentId}
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

    // DOGS OPERATIONS
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

            // Delete all subcollections first
            deleteAllSubcollections(dogRef)

            // Then delete the dog document
            dogRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // NOTES OPERATIONS
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

            val notes = snapshot.toObjects(DogNote::class.java)
            Result.success(notes)
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

    // POOP OPERATIONS
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

            val poopEntries = snapshot.toObjects(DogPoop::class.java)
            Result.success(poopEntries)
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

    // REMINDERS OPERATIONS
    suspend fun addReminder(dogId: String, reminder: Reminder): Result<Unit> {
        return try {
            val remindersCollection = getDogSubCollection(dogId, "reminders")
                ?: return Result.failure(Exception("User not logged in"))

            remindersCollection.document(reminder.id).set(reminder).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReminders(dogId: String): Result<List<Reminder>> {
        return try {
            val remindersCollection = getDogSubCollection(dogId, "reminders")
                ?: return Result.failure(Exception("User not logged in"))

            val snapshot = remindersCollection
                .orderBy("dateTime", Query.Direction.ASCENDING)
                .get().await()

            val reminders = snapshot.toObjects(Reminder::class.java)
            Result.success(reminders)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllReminders(): Result<List<Reminder>> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))

            val dogsSnapshot = firestore.collection("users")
                .document(userId)
                .collection("dogs")
                .get().await()

            val allReminders = mutableListOf<Reminder>()

            for (dogDoc in dogsSnapshot.documents) {
                val remindersSnapshot = dogDoc.reference
                    .collection("reminders")
                    .get().await()

                val reminders = remindersSnapshot.toObjects(Reminder::class.java)
                allReminders.addAll(reminders)
            }

            // Sort by date
            allReminders.sortBy { it.dateTime }
            Result.success(allReminders)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateReminder(dogId: String, reminder: Reminder): Result<Unit> {
        return try {
            val remindersCollection = getDogSubCollection(dogId, "reminders")
                ?: return Result.failure(Exception("User not logged in"))

            remindersCollection.document(reminder.id).set(reminder).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteReminder(dogId: String, reminderId: String): Result<Unit> {
        return try {
            val remindersCollection = getDogSubCollection(dogId, "reminders")
                ?: return Result.failure(Exception("User not logged in"))

            remindersCollection.document(reminderId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // WALKS OPERATIONS - Save walk completion data
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

    // IMAGE OPERATIONS
    suspend fun uploadImage(dogId: String, imageUri: android.net.Uri, folder: String): Result<String> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))

            val imageRef = storage.reference.child("$folder/${userId}/${dogId}/${System.currentTimeMillis()}.jpg")
            val uploadTask = imageRef.putFile(imageUri).await()
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

    // UTILITY FUNCTIONS
    private suspend fun deleteAllSubcollections(dogRef: DocumentReference) {
        val subcollections = listOf("notes", "poop", "reminders", "walks")

        for (collection in subcollections) {
            try {
                val snapshot = dogRef.collection(collection).get().await()
                for (doc in snapshot.documents) {
                    doc.reference.delete().await()
                }
            } catch (e: Exception) {
                // Continue even if one subcollection fails
                android.util.Log.e("FirebaseDataManager", "Failed to delete $collection", e)
            }
        }
    }

    // LISTENER FUNCTIONS FOR REAL-TIME UPDATES
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
        return getDogSubCollection(dogId, "notes")?.orderBy("lastModified", Query.Direction.DESCENDING)
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseDataManager", "Notes listener error", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val notes = snapshot.toObjects(DogNote::class.java)
                    onUpdate(notes)
                }
            }
    }

    fun addRemindersListener(onUpdate: (List<Reminder>) -> Unit): ListenerRegistration? {
        val userId = getCurrentUserId() ?: return null

        return firestore.collection("users")
            .document(userId)
            .collection("dogs")
            .addSnapshotListener { dogsSnapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseDataManager", "Reminders listener error", error)
                    return@addSnapshotListener
                }

                if (dogsSnapshot != null) {
                    val allReminders = mutableListOf<Reminder>()
                    var completedQueries = 0
                    val totalDogs = dogsSnapshot.size()

                    if (totalDogs == 0) {
                        onUpdate(emptyList())
                        return@addSnapshotListener
                    }

                    for (dogDoc in dogsSnapshot.documents) {
                        dogDoc.reference.collection("reminders")
                            .get()
                            .addOnSuccessListener { remindersSnapshot ->
                                val reminders = remindersSnapshot.toObjects(Reminder::class.java)
                                allReminders.addAll(reminders)
                                completedQueries++

                                if (completedQueries == totalDogs) {
                                    allReminders.sortBy { it.dateTime }
                                    onUpdate(allReminders)
                                }
                            }
                            .addOnFailureListener {
                                completedQueries++
                                if (completedQueries == totalDogs) {
                                    allReminders.sortBy { it.dateTime }
                                    onUpdate(allReminders)
                                }
                            }
                    }
                }
            }
    }
}