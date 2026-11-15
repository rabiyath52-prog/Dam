package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.PythonScript
import com.example.data.model.ThemeFinish
import com.example.data.model.WebsiteProject
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StudioRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeWebsites(userId: String): Flow<List<WebsiteProject>> = flow {
        val path = "users/$userId/websites"
        emitAll(
            db.collection("users")
                .document(userId)
                .collection("websites")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WebsiteProject::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path, auth)
                    }
                    throw error
                }
        )
    }

    suspend fun getWebsite(websiteId: String): Result<WebsiteProject?> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("websites").document(websiteId)
        return try {
            val snap = docRef.get().await()
            val item = snap.toObject(
                WebsiteProject::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(item)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun createWebsite(
        title: String,
        heading: String,
        content: String,
        themeFinish: ThemeFinish,
        generatedHtml: String
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("websites").document()
        val payload = mapOf(
            "userId" to uid,
            "title" to title.trim().take(120),
            "heading" to heading.trim().take(180),
            "content" to content.trim().take(5000),
            "themeFinish" to themeFinish.name,
            "generatedHtml" to generatedHtml.take(50000),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun updateWebsite(website: WebsiteProject): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        require(website.id.isNotBlank()) { "Website ID must not be blank for update" }
        requireNotNull(website.createdAt) { "Website createdAt must not be null for update" }

        val docRef = db.collection("users").document(uid).collection("websites").document(website.id)
        return try {
            docRef.set(
                website.copy(
                    userId = uid,
                    title = website.title.trim().take(120),
                    heading = website.heading.trim().take(180),
                    content = website.content.trim().take(5000),
                    generatedHtml = website.generatedHtml.take(50000),
                    updatedAt = null
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun deleteWebsite(websiteId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("websites").document(websiteId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            Result.failure(e)
        }
    }

    fun observeScripts(userId: String): Flow<List<PythonScript>> = flow {
        val path = "users/$userId/scripts"
        emitAll(
            db.collection("users")
                .document(userId)
                .collection("scripts")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PythonScript::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path, auth)
                    }
                    throw error
                }
        )
    }

    suspend fun createScript(
        name: String,
        code: String,
        lastOutput: String
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("scripts").document()
        val payload = mapOf(
            "userId" to uid,
            "name" to name.trim().take(120),
            "code" to code.take(25000),
            "lastOutput" to lastOutput.take(25000),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun updateScript(script: PythonScript): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        require(script.id.isNotBlank()) { "Script ID must not be blank for update" }
        requireNotNull(script.createdAt) { "Script createdAt must not be null for update" }

        val docRef = db.collection("users").document(uid).collection("scripts").document(script.id)
        return try {
            docRef.set(
                script.copy(
                    userId = uid,
                    name = script.name.trim().take(120),
                    code = script.code.take(25000),
                    lastOutput = script.lastOutput.take(25000),
                    updatedAt = null
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun deleteScript(scriptId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("scripts").document(scriptId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            Result.failure(e)
        }
    }
}
