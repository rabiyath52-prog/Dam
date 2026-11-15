package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.GeneratedWebsiteDraft
import com.example.data.model.ThemeFinish
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiStudioService"
private const val AI_TIMEOUT_MS = 60_000L

class GeminiStudioService {

    private val websiteSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(
                description = "Concise, memorable website title (2 to 5 words) inspired by the photo"
            ),
            "heading" to Schema.string(
                description = "Bold hero headline for the landing page (4 to 10 words)"
            ),
            "content" to Schema.string(
                description = "Engaging 2-4 sentence promotional or descriptive body copy tailored to the product, storefront, or scene in the photo"
            ),
            "themeFinish" to Schema.enumeration(
                values = listOf("GOLD", "SILVER", "PLATINUM"),
                description = "Best matching metallic website finish: GOLD, SILVER, or PLATINUM"
            )
        )
    )

    // Required by user specification: use model gemini-4-argon for image understanding
    private val imageWebsiteModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-4-argon",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = websiteSchema
            }
        )
    }

    private val pythonRunnerModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    suspend fun generateWebsiteFromPhoto(
        bitmap: Bitmap,
        userHint: String = ""
    ): GeneratedWebsiteDraft {
        val hintClause = if (userHint.isNotBlank()) {
            "Additional brand or shop context from the user: \"$userHint\"."
        } else {
            ""
        }
        val prompt = """
            Analyze this uploaded photo carefully and design a promotional website draft for it.
            Identify the subject, craft, tone, and aesthetic palette visible in the photo.
            $hintClause
            Return structured JSON with:
            - title: concise brand/page title (max 5 words)
            - heading: welcoming hero headline (max 10 words)
            - content: 2 to 4 vivid sentences describing what is showcased and why customers will love it
            - themeFinish: choose GOLD for warm/luxury/traditional/golden visuals, SILVER for sleek/cool/industrial/metallic visuals, or PLATINUM for crisp/minimal/bridal/modern visuals.
        """.trimIndent()

        try {
            val response = withTimeout(AI_TIMEOUT_MS) {
                imageWebsiteModel.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                )
            }
            val jsonString = response.text
                ?: throw IllegalStateException("Gemini returned an empty response.")
            val json = JSONObject(jsonString)
            return GeneratedWebsiteDraft(
                title = json.getString("title").trim(),
                heading = json.getString("heading").trim(),
                content = json.getString("content").trim(),
                themeFinish = ThemeFinish.fromString(json.optString("themeFinish", "GOLD"))
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Photo-to-website analysis failed", e)
            throw e
        }
    }

    fun runPythonCodeStream(code: String): Flow<String> = flow {
        val prompt = """
            You are a deterministic Python 3.12 terminal runner.
            Evaluate the Python script below mentally step-by-step and output ONLY what stdout (and stderr traceback if there is a syntax or runtime error) would print in a real Python console.
            Do NOT wrap the output in markdown code fences or add commentary before/after the console output.
            If the script produces no output and has no errors, output: (Program exited with code 0 — no stdout output)
            If the script tries to call input(), explain briefly in stderr that interactive stdin is unsupported in mobile runner mode.

            Python script:
            $code
        """.trimIndent()

        val accumulated = StringBuilder()
        withTimeout(AI_TIMEOUT_MS) {
            pythonRunnerModel.generateContentStream(prompt).collect { chunk ->
                val textChunk = chunk.text.orEmpty()
                if (textChunk.isNotEmpty()) {
                    accumulated.append(textChunk)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("Python runner returned an empty response.")
        }
    }

    fun reviewPythonCodeStream(code: String, lastOutput: String): Flow<String> = flow {
        val prompt = """
            Review this Python 3 script concisely for an Android mobile coder.
            1) Explain what it does in 1-2 sentences.
            2) Point out any bugs, edge cases, or improvements.
            3) Provide a clean corrected snippet only if needed.
            Keep the response crisp and readable on a small screen.

            Script:
            $code

            Last output:
            $lastOutput
        """.trimIndent()

        val accumulated = StringBuilder()
        withTimeout(AI_TIMEOUT_MS) {
            pythonRunnerModel.generateContentStream(prompt).collect { chunk ->
                val textChunk = chunk.text.orEmpty()
                if (textChunk.isNotEmpty()) {
                    accumulated.append(textChunk)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("Code reviewer returned an empty response.")
        }
    }
}
