package com.example.ui.studio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiStudioService
import com.example.data.model.PythonScript
import com.example.data.model.ThemeFinish
import com.example.data.model.WebsiteProject
import com.example.data.repository.StudioRepository
import com.example.util.HtmlGenerator
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

sealed interface AsyncListState<out T> {
    data object Loading : AsyncListState<Nothing>
    data class Success<T>(val data: T) : AsyncListState<T>
    data class Error(val message: String) : AsyncListState<Nothing>
}

sealed interface ActionUiState {
    data object Idle : ActionUiState
    data object Loading : ActionUiState
    data class Streaming(val text: String) : ActionUiState
    data class Success(val message: String) : ActionUiState
    data class Error(val message: String) : ActionUiState
}

data class WebsiteMakerUiState(
    val editingId: String? = null,
    val editingCreatedAt: Timestamp? = null,
    val title: String = "Ekativa Gold",
    val heading: String = "Welcome to My Shop",
    val content: String = "Best Gold Chains in Kerala crafted with certified 22K purity and timeless artistry.",
    val themeFinish: ThemeFinish = ThemeFinish.GOLD,
    val photoHint: String = "",
    val selectedPhotoBitmap: Bitmap? = null,
    val showHtmlSource: Boolean = false,
    val photoAnalysisState: ActionUiState = ActionUiState.Idle,
    val saveState: ActionUiState = ActionUiState.Idle
) {
    val generatedHtml: String
        get() = HtmlGenerator.buildWebsiteHtml(title, heading, content, themeFinish)
}

data class PythonTemplate(
    val title: String,
    val fileName: String,
    val code: String
)

data class PythonCoderUiState(
    val editingId: String? = null,
    val editingCreatedAt: Timestamp? = null,
    val scriptName: String = "ekativa_shop.py",
    val code: String = """
        # Ekativa Studio - Python Script
        shop = "Ekativa Gold"
        finishes = ["Gold", "Silver", "Platinum"]
        prices_per_gram = {"Gold": 7450, "Silver": 98, "Platinum": 3250}

        print(f"=== {shop} Rate Card ===")
        for item in finishes:
            print(f"{item:<10} : INR {prices_per_gram[item]}/g")
    """.trimIndent(),
    val terminalOutput: String = "Tap Run Code to execute your Python script.",
    val runnerState: ActionUiState = ActionUiState.Idle,
    val saveState: ActionUiState = ActionUiState.Idle
)

class StudioViewModel(
    private val repository: StudioRepository,
    private val aiService: GeminiStudioService,
    private val currentUserId: String
) : ViewModel() {

    val savedWebsites: StateFlow<AsyncListState<List<WebsiteProject>>> =
        repository.observeWebsites(currentUserId)
            .map<List<WebsiteProject>, AsyncListState<List<WebsiteProject>>> {
                AsyncListState.Success(it)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing websites", root)
                emit(AsyncListState.Error(root.message ?: "Failed to load saved websites."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = AsyncListState.Loading
            )

    val savedScripts: StateFlow<AsyncListState<List<PythonScript>>> =
        repository.observeScripts(currentUserId)
            .map<List<PythonScript>, AsyncListState<List<PythonScript>>> {
                AsyncListState.Success(it)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing scripts", root)
                emit(AsyncListState.Error(root.message ?: "Failed to load saved scripts."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = AsyncListState.Loading
            )

    private val _websiteState = MutableStateFlow(WebsiteMakerUiState())
    val websiteState: StateFlow<WebsiteMakerUiState> = _websiteState.asStateFlow()

    private val _pythonState = MutableStateFlow(PythonCoderUiState())
    val pythonState: StateFlow<PythonCoderUiState> = _pythonState.asStateFlow()

    private var pythonJob: Job? = null

    val pythonTemplates: List<PythonTemplate> = listOf(
        PythonTemplate(
            title = "Rate Card",
            fileName = "ekativa_rates.py",
            code = """
                shop = "Ekativa Gold"
                finishes = ["Gold", "Silver", "Platinum"]
                prices_per_gram = {"Gold": 7450, "Silver": 98, "Platinum": 3250}

                print(f"=== {shop} Rate Card ===")
                for item in finishes:
                    print(f"{item:<10} : INR {prices_per_gram[item]}/g")
            """.trimIndent()
        ),
        PythonTemplate(
            title = "HTML Card",
            fileName = "card_builder.py",
            code = """
                def make_card(title, finish="Gold"):
                    return f"<div class='card {finish.lower()}'><h2>{title}</h2></div>"

                for name in ["Heritage Necklace", "Silver Anklet", "Platinum Band"]:
                    print(make_card(name))
            """.trimIndent()
        ),
        PythonTemplate(
            title = "Fibonacci",
            fileName = "fibonacci.py",
            code = """
                def fib(n):
                    a, b = 0, 1
                    seq = []
                    for _ in range(n):
                        seq.append(a)
                        a, b = b, a + b
                    return seq

                print("First 10:", fib(10))
            """.trimIndent()
        )
    )

    fun updateTitle(value: String) {
        _websiteState.update { it.copy(title = value, saveState = ActionUiState.Idle) }
    }

    fun updateHeading(value: String) {
        _websiteState.update { it.copy(heading = value, saveState = ActionUiState.Idle) }
    }

    fun updateContent(value: String) {
        _websiteState.update { it.copy(content = value, saveState = ActionUiState.Idle) }
    }

    fun updateThemeFinish(finish: ThemeFinish) {
        _websiteState.update { it.copy(themeFinish = finish, saveState = ActionUiState.Idle) }
    }

    fun updatePhotoHint(hint: String) {
        _websiteState.update { it.copy(photoHint = hint) }
    }

    fun toggleHtmlSource() {
        _websiteState.update { it.copy(showHtmlSource = !it.showHtmlSource) }
    }

    fun startNewWebsiteDraft() {
        _websiteState.value = WebsiteMakerUiState(
            title = "",
            heading = "",
            content = "",
            themeFinish = ThemeFinish.GOLD
        )
    }

    fun loadWebsite(project: WebsiteProject) {
        _websiteState.update {
            it.copy(
                editingId = project.id,
                editingCreatedAt = project.createdAt,
                title = project.title,
                heading = project.heading,
                content = project.content,
                themeFinish = ThemeFinish.fromString(project.themeFinish),
                saveState = ActionUiState.Idle,
                photoAnalysisState = ActionUiState.Idle
            )
        }
    }

    fun analyzePhotoAndGenerateWebsite(context: Context, uri: Uri) {
        viewModelScope.launch {
            _websiteState.update { it.copy(photoAnalysisState = ActionUiState.Loading) }
            try {
                val bitmap = decodeScaledBitmap(context.applicationContext, uri)
                    ?: throw IllegalStateException("Could not decode the selected photo.")
                _websiteState.update { it.copy(selectedPhotoBitmap = bitmap) }

                val hint = _websiteState.value.photoHint
                val generated = aiService.generateWebsiteFromPhoto(bitmap, hint)
                _websiteState.update {
                    it.copy(
                        title = generated.title,
                        heading = generated.heading,
                        content = generated.content,
                        themeFinish = generated.themeFinish,
                        photoAnalysisState = ActionUiState.Success(
                            "Generated ${generated.themeFinish.displayName} website draft from photo."
                        ),
                        saveState = ActionUiState.Idle
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Photo analysis timed out", e)
                _websiteState.update {
                    it.copy(
                        photoAnalysisState = ActionUiState.Error(
                            "Photo analysis timed out after 60 seconds. Please try again."
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Photo analysis failed", e)
                _websiteState.update {
                    it.copy(
                        photoAnalysisState = ActionUiState.Error(
                            e.localizedMessage ?: "Failed to analyze photo with Gemini."
                        )
                    )
                }
            }
        }
    }

    fun saveCurrentWebsite() {
        val current = _websiteState.value
        if (current.title.isBlank() || current.heading.isBlank() || current.content.isBlank()) {
            _websiteState.update {
                it.copy(
                    saveState = ActionUiState.Error("Title, heading, and content are all required before saving.")
                )
            }
            return
        }

        viewModelScope.launch {
            _websiteState.update { it.copy(saveState = ActionUiState.Loading) }
            val html = current.generatedHtml
            val existingId = current.editingId
            val existingCreatedAt = current.editingCreatedAt

            val result = if (!existingId.isNullOrBlank() && existingCreatedAt != null) {
                repository.updateWebsite(
                    WebsiteProject(
                        id = existingId,
                        userId = currentUserId,
                        title = current.title,
                        heading = current.heading,
                        content = current.content,
                        themeFinish = current.themeFinish.name,
                        generatedHtml = html,
                        createdAt = existingCreatedAt,
                        updatedAt = null
                    )
                ).map { existingId }
            } else {
                repository.createWebsite(
                    title = current.title,
                    heading = current.heading,
                    content = current.content,
                    themeFinish = current.themeFinish,
                    generatedHtml = html
                )
            }

            result.fold(
                onSuccess = { savedId ->
                    _websiteState.update {
                        it.copy(
                            editingId = savedId,
                            editingCreatedAt = it.editingCreatedAt ?: Timestamp.now(),
                            saveState = ActionUiState.Success("Website synced to cloud.")
                        )
                    }
                },
                onFailure = { error ->
                    _websiteState.update {
                        it.copy(
                            saveState = ActionUiState.Error(
                                error.localizedMessage ?: "Failed to save website."
                            )
                        )
                    }
                }
            )
        }
    }

    fun deleteWebsite(websiteId: String) {
        viewModelScope.launch {
            repository.deleteWebsite(websiteId).onSuccess {
                if (_websiteState.value.editingId == websiteId) {
                    _websiteState.update { it.copy(editingId = null, editingCreatedAt = null) }
                }
            }
        }
    }

    fun updateScriptName(name: String) {
        _pythonState.update { it.copy(scriptName = name, saveState = ActionUiState.Idle) }
    }

    fun updatePythonCode(code: String) {
        _pythonState.update { it.copy(code = code, saveState = ActionUiState.Idle) }
    }

    fun applyPythonTemplate(template: PythonTemplate) {
        _pythonState.update {
            it.copy(
                editingId = null,
                editingCreatedAt = null,
                scriptName = template.fileName,
                code = template.code,
                terminalOutput = "Loaded template: ${template.fileName}",
                runnerState = ActionUiState.Idle,
                saveState = ActionUiState.Idle
            )
        }
    }

    fun loadSavedScript(script: PythonScript) {
        _pythonState.update {
            it.copy(
                editingId = script.id,
                editingCreatedAt = script.createdAt,
                scriptName = script.name,
                code = script.code,
                terminalOutput = script.lastOutput.ifBlank { "Loaded ${script.name}." },
                runnerState = ActionUiState.Idle,
                saveState = ActionUiState.Idle
            )
        }
    }

    fun runPythonCode() {
        val code = _pythonState.value.code
        if (code.isBlank()) {
            _pythonState.update {
                it.copy(runnerState = ActionUiState.Error("Write some Python code before running."))
            }
            return
        }
        pythonJob?.cancel()
        pythonJob = viewModelScope.launch {
            _pythonState.update {
                it.copy(
                    runnerState = ActionUiState.Loading,
                    terminalOutput = "Running with Python 3.12 AI Runner…"
                )
            }
            try {
                aiService.runPythonCodeStream(code).collect { partial ->
                    _pythonState.update {
                        it.copy(
                            runnerState = ActionUiState.Streaming(partial),
                            terminalOutput = partial
                        )
                    }
                }
                val finalOutput = _pythonState.value.terminalOutput
                _pythonState.update {
                    it.copy(runnerState = ActionUiState.Success("Execution finished"))
                }
                if (finalOutput.isBlank()) {
                    _pythonState.update {
                        it.copy(terminalOutput = "(Program exited with code 0)")
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Python run timed out", e)
                _pythonState.update {
                    it.copy(
                        runnerState = ActionUiState.Error("Execution timed out after 60 seconds.")
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Python run failed", e)
                _pythonState.update {
                    it.copy(
                        runnerState = ActionUiState.Error(
                            e.localizedMessage ?: "Failed to run Python script."
                        )
                    )
                }
            }
        }
    }

    fun reviewPythonCode() {
        val current = _pythonState.value
        if (current.code.isBlank()) {
            _pythonState.update {
                it.copy(runnerState = ActionUiState.Error("Write some Python code to review."))
            }
            return
        }
        pythonJob?.cancel()
        pythonJob = viewModelScope.launch {
            _pythonState.update {
                it.copy(
                    runnerState = ActionUiState.Loading,
                    terminalOutput = "Reviewing script with Gemini…"
                )
            }
            try {
                aiService.reviewPythonCodeStream(current.code, current.terminalOutput)
                    .collect { partial ->
                        _pythonState.update {
                            it.copy(
                                runnerState = ActionUiState.Streaming(partial),
                                terminalOutput = partial
                            )
                        }
                    }
                _pythonState.update {
                    it.copy(runnerState = ActionUiState.Success("Code review complete"))
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Python review timed out", e)
                _pythonState.update {
                    it.copy(runnerState = ActionUiState.Error("Code review timed out after 60 seconds."))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Python review failed", e)
                _pythonState.update {
                    it.copy(
                        runnerState = ActionUiState.Error(
                            e.localizedMessage ?: "Failed to review Python script."
                        )
                    )
                }
            }
        }
    }

    fun saveCurrentScript() {
        val current = _pythonState.value
        if (current.scriptName.isBlank() || current.code.isBlank()) {
            _pythonState.update {
                it.copy(saveState = ActionUiState.Error("Script name and code cannot be empty."))
            }
            return
        }

        viewModelScope.launch {
            _pythonState.update { it.copy(saveState = ActionUiState.Loading) }
            val existingId = current.editingId
            val existingCreatedAt = current.editingCreatedAt

            val result = if (!existingId.isNullOrBlank() && existingCreatedAt != null) {
                repository.updateScript(
                    PythonScript(
                        id = existingId,
                        userId = currentUserId,
                        name = current.scriptName,
                        code = current.code,
                        lastOutput = current.terminalOutput,
                        createdAt = existingCreatedAt,
                        updatedAt = null
                    )
                ).map { existingId }
            } else {
                repository.createScript(
                    name = current.scriptName,
                    code = current.code,
                    lastOutput = current.terminalOutput
                )
            }

            result.fold(
                onSuccess = { savedId ->
                    _pythonState.update {
                        it.copy(
                            editingId = savedId,
                            editingCreatedAt = it.editingCreatedAt ?: Timestamp.now(),
                            saveState = ActionUiState.Success("Script saved to cloud.")
                        )
                    }
                },
                onFailure = { error ->
                    _pythonState.update {
                        it.copy(
                            saveState = ActionUiState.Error(
                                error.localizedMessage ?: "Failed to save script."
                            )
                        )
                    }
                }
            )
        }
    }

    fun deleteScript(scriptId: String) {
        viewModelScope.launch {
            repository.deleteScript(scriptId).onSuccess {
                if (_pythonState.value.editingId == scriptId) {
                    _pythonState.update { it.copy(editingId = null, editingCreatedAt = null) }
                }
            }
        }
    }

    private suspend fun decodeScaledBitmap(context: Context, uri: Uri): Bitmap? =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            val maxDim = max(bounds.outWidth, bounds.outHeight)
            var sampleSize = 1
            while (maxDim / sampleSize > MAX_IMAGE_DIM) {
                sampleSize *= 2
            }
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        }

    private companion object {
        const val TAG = "StudioViewModel"
        const val STOP_TIMEOUT_MS = 5000L
        const val MAX_IMAGE_DIM = 1280
    }
}
