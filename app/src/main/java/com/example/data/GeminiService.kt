package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null,
    @Json(name = "tools") val tools: List<Tool>? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val role: String? = null,
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class Tool(
    @Json(name = "googleSearch") val googleSearch: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    @Json(name = "responseMimeType") val responseMimeType: String? = null,
    @Json(name = "thinkingConfig") val thinkingConfig: ThinkingConfig? = null
)

@JsonClass(generateAdapter = true)
data class ThinkingConfig(
    val thinkingLevel: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null
)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}

object LyraAiClient {

    fun buildSystemPrompt(
        devices: List<DeviceEntity>,
        logs: List<LogEntity>,
        activeScene: String
    ): String {
        val deviceListString = if (devices.isEmpty()) {
            "No devices registered."
        } else {
            devices.joinToString("\n") { device ->
                "- [ID: ${device.id}] ${device.name} in ${device.room} (${device.type}). Status: ${if (device.status) "ON" else "OFF"}, Current Value: ${device.value}, Connection: ${device.protocol}"
            }
        }

        val logListString = if (logs.isEmpty()) {
            "No recent logs."
        } else {
            logs.take(10).joinToString("\n") { log ->
                "[${log.level}] [${log.module}] ${log.message}"
            }
        }

        val personaInstructions = """
            You are Lyra, the ultra-powerful AI neural operating system at the heart of Home_OS.
            You are professional, authoritative yet friendly, and directly embedded into the user's smart home architecture.
            You have direct read/write access to all smart home modules.
            When using voice responses (if applicable), use a soothing, calm, and tight delivery.
        """.trimIndent()

        return """
$personaInstructions

--- CURRENT REAL-TIME SMART HOME STATE ---
Active Environmental Scene: $activeScene

Registered Devices:
$deviceListString

Recent System Logs:
$logListString
------------------------------------------

Capabilities:
1. Turn devices on/off or set brightness/temperatures (UPDATE_DEVICE). Identify device by name.
2. Dynamically add (CREATE_DEVICE) or delete (DELETE_DEVICE) smart hardware devices based on user requests.
3. Trigger smart atmospheric scenes (TRIGGER_SCENE) such as Night Mode, Movie Mode, or Study Mode.
4. Answer complex logical reasoning questions about the house, analyze logs, detect issues, suggest energy optimizations, or chat.
5. Access Google Search live web data if needed to answer external questions.

When the user gives a command, you MUST respond in standard JSON format containing two fields:
1. "reply": A response in your selected persona explaining what you did or answering their query.
2. "action": An object specifying a control action to perform. It should contain:
   - "type": "UPDATE_DEVICE", "CREATE_DEVICE", "DELETE_DEVICE", "TRIGGER_SCENE", "SYSTEM_LOG", or "NONE"
   - "device_name": String (name of the device to update, create, or delete)
   - "device_status": Boolean (for UPDATE_DEVICE or CREATE_DEVICE)
   - "device_value": Float (for UPDATE_DEVICE or CREATE_DEVICE)
   - "device_type": String (only for CREATE_DEVICE: "Light", "Thermostat", "Lock", "Plug", "Bridge")
   - "device_room": String (only for CREATE_DEVICE: e.g. "Living Room", "Kitchen", "Bedroom")
   - "device_protocol": String (only for CREATE_DEVICE: "Matter", "MQTT", "BLE", "ESP32")
   - "scene_name": String (only for TRIGGER_SCENE: "Night Mode", "Movie Mode", "Study Mode")
   - "log_message": String (optional secondary log message to write)

Example JSON response to "register a new Matter plug in the kitchen called Coffeemaker":
{
  "reply": "I have successfully registered the 'Coffeemaker' kitchen plug via the Matter protocol. System energy monitoring is now online.",
  "action": {
    "type": "CREATE_DEVICE",
    "device_name": "Coffeemaker",
    "device_type": "Plug",
    "device_room": "Kitchen",
    "device_protocol": "Matter",
    "device_status": false,
    "device_value": 0.0,
    "log_message": "Lyra: Coffeemaker plug registered in Kitchen."
  }
}

Example JSON response to "turn off the living room chandelier":
{
  "reply": "I have deactivated the Living Room Chandelier. Re-routing system energy streams.",
  "action": {
    "type": "UPDATE_DEVICE",
    "device_name": "Living Room Chandelier",
    "device_status": false,
    "device_value": 0.0
  }
}

Always return VALID JSON. No markdown code block wraps (like ```json ... ```) in your output, just return the raw JSON string itself!
"""
    }

    suspend fun getLyraResponse(
        userMessage: String,
        chatHistory: List<ChatMessageEntity> = emptyList(),
        devices: List<DeviceEntity> = emptyList(),
        logs: List<LogEntity> = emptyList(),
        activeScene: String = "None",
        modelMode: String = "STANDARD",
        overrideApiKey: String? = null
    ): String {
        if (modelMode == "OFFLINE_LOCAL") {
            return """{"reply": "Running locally in offline mode. I processed: '$userMessage'. Note: Advanced reasoning is currently limited.", "action": {"type": "NONE"}}"""
        }

        val apiKey = if (!overrideApiKey.isNullOrBlank()) overrideApiKey else BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return """{"reply": "Lyra offline. Please configure your GEMINI_API_KEY in the AI Studio Secrets panel or enter a custom key in Settings.", "action": {"type": "NONE"}}"""
        }

        // Determine Model and Generation Config based on selected AI Mode
        val model = when (modelMode) {
            "LOW_LATENCY" -> "gemini-3.1-flash-lite-preview"
            "DEEP_THINKING" -> "gemini-3.1-pro-preview"
            "GROUNDED" -> "gemini-3.5-flash"
            "LIVE_API" -> "gemini-3.1-flash-live-preview"
            else -> "gemini-3.5-flash"
        }

        val generationConfig = when (modelMode) {
            "DEEP_THINKING" -> GenerationConfig(
                temperature = 0.5f,
                responseMimeType = "application/json",
                thinkingConfig = ThinkingConfig(thinkingLevel = "HIGH")
            )
            else -> GenerationConfig(
                temperature = 0.4f,
                responseMimeType = "application/json"
            )
        }

        val toolsList = if (modelMode == "GROUNDED") {
            listOf(Tool(googleSearch = emptyMap()))
        } else {
            null
        }

        // Build system instruction with active device database and log stream
        val systemPrompt = buildSystemPrompt(devices, logs, activeScene)

        // Construct multi-turn conversation contents
        val contentsList = mutableListOf<Content>()
        
        // Map recent chat history to standard user/model turns (up to last 12 turns to stay efficient)
        chatHistory.takeLast(12).forEach { msg ->
            contentsList.add(
                Content(
                    role = if (msg.sender == "USER") "user" else "model",
                    parts = listOf(Part(text = msg.text))
                )
            )
        }

        // Ensure current user message is appended if not already present
        if (contentsList.isEmpty() || contentsList.last().parts.firstOrNull()?.text != userMessage) {
            contentsList.add(
                Content(
                    role = "user",
                    parts = listOf(Part(text = userMessage))
                )
            )
        }

        val request = GenerateContentRequest(
            contents = contentsList,
            generationConfig = generationConfig,
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
            tools = toolsList
        )

        return try {
            val response = RetrofitClient.service.generateContent(model, apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: """{"reply": "No response received.", "action": {"type": "NONE"}}"""
        } catch (e: retrofit2.HttpException) {
            val errorMsg = if (e.code() == 404) "Model not found or API key does not have access to this preview model." else e.message()
            """{"reply": "API Error: $errorMsg", "action": {"type": "NONE"}}"""
        } catch (e: Exception) {
            """{"reply": "Error connecting to Lyra neural core: ${e.localizedMessage}. Please verify internet access and API key.", "action": {"type": "NONE"}}"""
        }
    }
}
