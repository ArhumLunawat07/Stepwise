package com.example.data.ai

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ParsedStepResult(
    val stepNumber: Int,
    val title: String,
    val plainInstruction: String,
    val details: String,
    val warningNote: String?,
    val isAmbiguous: Boolean,
    val ambiguityReason: String?
)

data class ChatMessage(
    val role: String, // "user" or "model"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

object GeminiService {
    private const val TAG = "GeminiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val isConfigured: Boolean
        get() = try {
            BuildConfig.GEMINI_API_KEY.isNotBlank() &&
                    !BuildConfig.GEMINI_API_KEY.contains("MY_GEMINI_API_KEY")
        } catch (e: Exception) {
            false
        }

    /**
     * Converts raw text instructions into plain-language actionable steps.
     * Uses gemini-4-argon with high thinking mode when [useDeepThinking] is true,
     * or gemini-3.8-flash for fast standard simplification.
     */
    suspend fun generateChecklist(
        rawText: String,
        useDeepThinking: Boolean = false
    ): List<ParsedStepResult> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
        if (apiKey.isBlank() || apiKey.contains("MY_GEMINI_API_KEY")) {
            throw IllegalStateException("Gemini API key is not configured in Secrets.")
        }

        val model = if (useDeepThinking) "gemini-4-argon" else "gemini-3.8-flash"
        val endpoint = "$BASE_URL/$model:generateContent?key=$apiKey"

        val systemInstruction = """
            You are Stepwise AI, an expert accessibility product designer and plain-language specialist.
            Your purpose is: "Clear steps. More independence."
            Analyze user instructions and convert them into an orderly, sequential checklist of bite-sized steps.
            Rules:
            1. Use plain, easy-to-read language.
            2. Never invent legal advice, official requirements, or critical facts.
            3. Preserve all dates, dollar amounts, warning notes, names, and postal or web requirements.
            4. If any instruction is vague, conditional without explanation, or missing key facts, set isAmbiguous = true and describe why in ambiguityReason without guessing.
            5. Output STRICT JSON conforming to the requested schema.
        """.trimIndent()

        val prompt = "Transform the following instructions into a sequential, accessible checklist:\n\n$rawText"

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemInstruction)
                    })
                })
            })

            val genConfig = JSONObject().apply {
                put("responseMimeType", "application/json")
                if (useDeepThinking) {
                    put("thinkingConfig", JSONObject().apply {
                        put("thinkingLevel", "HIGH")
                    })
                }
            }
            put("generationConfig", genConfig)
        }

        val body = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(endpoint)
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            Log.e(TAG, "Gemini call failed with code ${response.code}: $responseBody")
            throw IllegalStateException("Gemini error ${response.code}: $responseBody")
        }

        parseStepsFromJson(responseBody)
    }

    private fun parseStepsFromJson(jsonString: String): List<ParsedStepResult> {
        val root = JSONObject(jsonString)
        val candidates = root.optJSONArray("candidates") ?: return emptyList()
        val firstCand = candidates.optJSONObject(0) ?: return emptyList()
        val content = firstCand.optJSONObject("content") ?: return emptyList()
        val parts = content.optJSONArray("parts") ?: return emptyList()

        val rawText = parts.optJSONObject(0)?.optString("text") ?: ""
        if (rawText.isBlank()) return emptyList()

        // Clean any potential markdown wrapper
        val cleanedJson = rawText.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val stepsArray = if (cleanedJson.startsWith("[")) {
            JSONArray(cleanedJson)
        } else {
            val obj = JSONObject(cleanedJson)
            obj.optJSONArray("steps") ?: obj.optJSONArray("checklist") ?: JSONArray()
        }

        val result = mutableListOf<ParsedStepResult>()
        for (i in 0 until stepsArray.length()) {
            val item = stepsArray.optJSONObject(i) ?: continue
            val num = item.optInt("stepNumber", i + 1)
            val title = item.optString("title", "Step $num")
            val plain = item.optString("plainInstruction", item.optString("instruction", title))
            val details = item.optString("details", "")
            val warning = item.optString("warningNote", "").takeIf { it.isNotBlank() }
            val isAmbiguous = item.optBoolean("isAmbiguous", false)
            val ambiguityReason = item.optString("ambiguityReason", "").takeIf { it.isNotBlank() }

            result.add(
                ParsedStepResult(
                    stepNumber = num,
                    title = title,
                    plainInstruction = plain,
                    details = details,
                    warningNote = warning,
                    isAmbiguous = isAmbiguous,
                    ambiguityReason = ambiguityReason
                )
            )
        }
        return result
    }

    /**
     * Low-latency step breakdown using gemini-3.1-flash-lite.
     * Takes a single step and breaks it into smaller sub-tasks.
     */
    suspend fun breakdownStep(
        stepTitle: String,
        stepInstruction: String
    ): List<String> = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        if (apiKey.isBlank() || apiKey.contains("MY_GEMINI_API_KEY")) {
            // Local fallback breakdown
            return@withContext listOf(
                "Read the instruction carefully: $stepTitle",
                "Prepare required items or documents",
                "Execute the main action: $stepInstruction",
                "Verify completion before proceeding"
            )
        }

        val endpoint = "$BASE_URL/gemini-3.1-flash-lite:generateContent?key=$apiKey"
        val prompt = """
            Break down this single task step into 2 to 4 very short, ultra-clear micro-actions:
            Step Title: $stepTitle
            Step Instruction: $stepInstruction
            
            Return JSON format: ["micro-action 1", "micro-action 2", "micro-action 3"]
        """.trimIndent()

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
            })
        }

        val body = requestJson.toString().toRequestBody("application/json".toMediaType())
        val response = client.newCall(Request.Builder().url(endpoint).post(body).build()).execute()
        val responseBody = response.body?.string() ?: ""

        try {
            val root = JSONObject(responseBody)
            val cand = root.getJSONArray("candidates").getJSONObject(0)
            val text = cand.getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
            val cleaned = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val array = if (cleaned.startsWith("[")) JSONArray(cleaned) else JSONObject(cleaned).getJSONArray("substeps")
            val subSteps = mutableListOf<String>()
            for (i in 0 until array.length()) {
                val s = array.optString(i)
                if (s.isNotBlank()) subSteps.add(s)
            }
            if (subSteps.isNotEmpty()) subSteps else listOf(stepInstruction)
        } catch (e: Exception) {
            listOf("Review instruction: $stepTitle", "Carry out action: $stepInstruction", "Double check results")
        }
    }

    /**
     * Multi-turn chat assistant for answering step-specific questions in plain language.
     * Uses gemini-3.8-flash for general inquiries or gemini-4-argon for complex logic.
     */
    suspend fun askAssistant(
        question: String,
        currentStepTitle: String,
        currentStepInstruction: String,
        history: List<ChatMessage>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        if (apiKey.isBlank() || apiKey.contains("MY_GEMINI_API_KEY")) {
            return@withContext "You are currently offline or in local mode. Please review the step instruction: '$currentStepInstruction'. If you have doubts about official rules, consult the issuing organization directly."
        }

        val isComplex = question.length > 120 || question.contains("legal", ignoreCase = true) || question.contains("penalty", ignoreCase = true)
        val model = if (isComplex) "gemini-4-argon" else "gemini-3.8-flash"
        val endpoint = "$BASE_URL/$model:generateContent?key=$apiKey"

        val contents = JSONArray()

        // System context as first turn or systemInstruction
        val systemInstruction = """
            You are the Stepwise Task Companion. You help users navigate their current step:
            Current Step Title: "$currentStepTitle"
            Current Step Instruction: "$currentStepInstruction"
            
            Guidelines:
            - Respond in plain, calm, comforting, simple language.
            - Answer the user's specific question directly.
            - Explain technical words simply.
            - Never invent facts or legal advice. If unsure, state clearly where the user can find official confirmation.
            - Keep responses under 4 sentences whenever possible to maintain low cognitive load.
        """.trimIndent()

        // Append conversation history
        for (msg in history.takeLast(6)) {
            contents.put(JSONObject().apply {
                put("role", msg.role)
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", msg.text) })
                })
            })
        }

        // Append current user message
        contents.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", question) })
            })
        })

        val requestJson = JSONObject().apply {
            put("contents", contents)
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemInstruction) })
                })
            })
            if (isComplex) {
                put("generationConfig", JSONObject().apply {
                    put("thinkingConfig", JSONObject().apply {
                        put("thinkingLevel", "HIGH")
                    })
                })
            }
        }

        val body = requestJson.toString().toRequestBody("application/json".toMediaType())
        val response = client.newCall(Request.Builder().url(endpoint).post(body).build()).execute()
        val responseBody = response.body?.string() ?: ""

        try {
            val root = JSONObject(responseBody)
            val cand = root.getJSONArray("candidates").getJSONObject(0)
            cand.getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
        } catch (e: Exception) {
            "I could not retrieve an answer right now. Please refer to your step details: $currentStepInstruction"
        }
    }
}
