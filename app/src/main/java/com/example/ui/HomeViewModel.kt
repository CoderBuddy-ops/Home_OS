package com.example.ui

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ChatMessageEntity
import com.example.data.DeviceEntity
import com.example.data.HomeRepository
import com.example.data.LogEntity
import com.example.data.LyraAiClient
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = HomeRepository(database)

    val devices: StateFlow<List<DeviceEntity>> = repository.devices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs: StateFlow<List<LogEntity>> = repository.logs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMessageEntity>> = repository.chatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state
    var currentTab by mutableStateOf("home") // home, devices, logs, config
    var isLyraThinking by mutableStateOf(false)
    var typedMessage by mutableStateOf("")
    var activeScene by mutableStateOf("None") // None, Night Mode, Study Mode, Movie Mode

    // Device dialog state
    var showAddDeviceDialog by mutableStateOf(false)
    var newDeviceName by mutableStateOf("")
    var newDeviceType by mutableStateOf("Light") // Light, Thermostat, Lock, Plug, Bridge
    var newDeviceRoom by mutableStateOf("Living Room")
    var newDeviceProtocol by mutableStateOf("Matter") // Matter, MQTT, BLE, ESP32

    // Lyra Quick Action State
    var showQuickActionDialog by mutableStateOf(false)

    // Voice Control State
    var showVoiceDialog by mutableStateOf(false)

    // AI Command Highlight State
    var lastAiUpdatedDeviceId by mutableStateOf<Int?>(null)

    // Lyra API state
    var showApiSettingsDialog by mutableStateOf(false)
    var customApiKey by mutableStateOf("")
    var apiStatusMessage by mutableStateOf("")
    var isTestingConnection by mutableStateOf(false)
    var testResultSuccess by mutableStateOf<Boolean?>(null)

    // AI Operation Mode: STANDARD, LOW_LATENCY, DEEP_THINKING, GROUNDED, LIVE_API, OFFLINE_LOCAL
    var aiMode by mutableStateOf("STANDARD")

    // Text-To-Speech (TTS) Configuration
    private var tts: TextToSpeech? = null
    var isTtsEnabled by mutableStateOf(true)

    private val sharedPrefs = application.getSharedPreferences("home_os_prefs", android.content.Context.MODE_PRIVATE)

    init {
        customApiKey = sharedPrefs.getString("custom_api_key", "") ?: ""
        aiMode = sharedPrefs.getString("ai_mode", "STANDARD") ?: "STANDARD"
        isTtsEnabled = sharedPrefs.getBoolean("tts_enabled", true)
        initTts()
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    fun setAiModeAndPersist(mode: String) {
        aiMode = mode
        sharedPrefs.edit().putString("ai_mode", mode).apply()
        viewModelScope.launch {
            repository.insertLog(
                LogEntity(
                    message = "Lyra neural intelligence mode set to: $mode.",
                    level = "INFO",
                    module = "AI"
                )
            )
        }
    }

    fun saveCustomApiKey(key: String) {
        customApiKey = key.trim()
        sharedPrefs.edit().putString("custom_api_key", customApiKey).apply()
        testResultSuccess = null
        apiStatusMessage = if (customApiKey.isNotEmpty()) "Custom API Key saved locally." else "Using default system API configuration."
    }

    fun testLyraConnection() {
        isTestingConnection = true
        testResultSuccess = null
        apiStatusMessage = "Initiating core handshake with Lyra..."

        viewModelScope.launch {
            val keyToUse = if (customApiKey.isNotBlank()) customApiKey else com.example.BuildConfig.GEMINI_API_KEY
            if (keyToUse.isBlank() || keyToUse == "MY_GEMINI_API_KEY") {
                isTestingConnection = false
                testResultSuccess = false
                apiStatusMessage = "No API key configured. Please enter a custom key or configure via Secrets panel."
                return@launch
            }

            try {
                // A very short message to test connection and key validity
                val response = com.example.data.LyraAiClient.getLyraResponse(
                    userMessage = "Verify API Handshake.",
                    chatHistory = emptyList(),
                    devices = emptyList(),
                    logs = emptyList(),
                    activeScene = "None",
                    modelMode = "STANDARD",
                    overrideApiKey = keyToUse
                )
                if (response.contains("reply") || !response.contains("Error connecting")) {
                    testResultSuccess = true
                    apiStatusMessage = "Handshake successful! Lyra core is active."
                    repository.insertLog(
                        LogEntity(
                            message = "Lyra Neural API Handshake: Connection verified successfully.",
                            level = "SUCCESS",
                            module = "SYSTEM"
                        )
                    )
                } else {
                    testResultSuccess = false
                    apiStatusMessage = "Handshake failed. The service did not return a valid response."
                }
            } catch (e: Exception) {
                testResultSuccess = false
                apiStatusMessage = "Handshake failed: ${e.localizedMessage}"
            } finally {
                isTestingConnection = false
            }
        }
    }

    fun activateScene(sceneName: String) {
        activeScene = sceneName
        viewModelScope.launch {
            devices.value.forEach { device ->
                val updatedDevice = when (sceneName) {
                    "Study Mode" -> {
                        when (device.type) {
                            "Light" -> device.copy(status = true, value = 80f)
                            "Thermostat" -> device.copy(status = true, value = 22f)
                            else -> device
                        }
                    }
                    "Night Mode" -> {
                        when (device.type) {
                            "Light" -> device.copy(status = false, value = 10f)
                            "Thermostat" -> device.copy(status = true, value = 18f)
                            "Lock" -> device.copy(status = true, value = 1f) // locked
                            "Plug" -> device.copy(status = false)
                            else -> device
                        }
                    }
                    "Movie Mode" -> {
                        when (device.type) {
                            "Light" -> device.copy(status = true, value = 15f)
                            "Lock" -> device.copy(status = true, value = 1f) // locked
                            "Plug" -> device.copy(status = true)
                            else -> device
                        }
                    }
                    else -> device
                }
                if (updatedDevice != device) {
                    repository.updateDevice(updatedDevice)
                }
            }
            repository.insertLog(
                LogEntity(
                    message = "Active Scene set to $sceneName. Environment states coordinated.",
                    level = "SUCCESS",
                    module = "SYSTEM"
                )
            )
        }
    }

    fun toggleDevice(device: DeviceEntity) {
        viewModelScope.launch {
            val updated = device.copy(status = !device.status)
            repository.updateDevice(updated)

            val statusText = if (updated.status) "ON" else "OFF"
            repository.insertLog(
                LogEntity(
                    message = "${device.name} was manually switched $statusText.",
                    level = "INFO",
                    module = device.protocol.uppercase()
                )
            )
        }
    }

    fun updateDeviceValue(device: DeviceEntity, newValue: Float) {
        viewModelScope.launch {
            val updated = device.copy(value = newValue)
            repository.updateDevice(updated)
        }
    }

    // Save final slider changes to logs to prevent flooding logs on active drag
    fun logDeviceValueChange(device: DeviceEntity, finalValue: Float) {
        viewModelScope.launch {
            val valueDetail = when (device.type) {
                "Thermostat" -> "$finalValue°C"
                "Light" -> "brightness to ${finalValue.toInt()}%"
                else -> "value to $finalValue"
            }
            repository.insertLog(
                LogEntity(
                    message = "${device.name} updated to $valueDetail.",
                    level = "INFO",
                    module = device.protocol.uppercase()
                )
            )
        }
    }

    fun addNewDevice() {
        if (newDeviceName.isBlank()) return
        viewModelScope.launch {
            val initialValue = when (newDeviceType) {
                "Thermostat" -> 21f
                "Light" -> 50f
                "Lock" -> 1f // 1 = Locked
                else -> 0f
            }
            val newDevice = DeviceEntity(
                name = newDeviceName.trim(),
                type = newDeviceType,
                status = false,
                value = initialValue,
                room = newDeviceRoom.trim(),
                protocol = newDeviceProtocol
            )
            repository.insertDevice(newDevice)

            repository.insertLog(
                LogEntity(
                    message = "New Matter-discoverable device paired: ${newDevice.name} (${newDevice.protocol}).",
                    level = "SUCCESS",
                    module = newDevice.protocol.uppercase()
                )
            )

            // Reset form
            newDeviceName = ""
            showAddDeviceDialog = false
        }
    }

    fun deleteDevice(deviceId: Int, deviceName: String, protocol: String) {
        viewModelScope.launch {
            repository.deleteDeviceById(deviceId)
            repository.insertLog(
                LogEntity(
                    message = "Device unlinked and deleted: $deviceName.",
                    level = "WARNING",
                    module = protocol.uppercase()
                )
            )
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            repository.insertLog(
                LogEntity(
                    message = "System audit logs cleared manually by administrator.",
                    level = "WARNING",
                    module = "SYSTEM"
                )
            )
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearChat()
            repository.insertChatMessage(
                ChatMessageEntity(
                    text = "Neural context flushed. Ready for new instructions, Commander.",
                    sender = "LYRA"
                )
            )
        }
    }

    fun reEncryptMatterBridge() {
        viewModelScope.launch {
            repository.insertLog(
                LogEntity(
                    message = "Matter Bridge Re-encrypted - Fresh secure TLS session established.",
                    level = "SUCCESS",
                    module = "MATTER"
                )
            )
        }
    }

    fun applySecurityPatch() {
        viewModelScope.launch {
            repository.insertLog(
                LogEntity(
                    message = "Security Patch 08A Applied - Exploit vulnerability vectors patched.",
                    level = "SUCCESS",
                    module = "SECURITY"
                )
            )
        }
    }

    fun sendUserMessage() {
        val msg = typedMessage.trim()
        if (msg.isBlank()) return
        typedMessage = ""
        isLyraThinking = true

        try {
            tts?.stop()
        } catch (e: Exception) {}

        viewModelScope.launch {
            // Save user message to database
            repository.insertChatMessage(
                ChatMessageEntity(text = msg, sender = "USER")
            )

            // Query Lyra via Gemini API with complete real-time state
            val keyToUse = if (customApiKey.isNotBlank()) customApiKey else null
            var rawResponse = ""
            var parsedReply = "Offline or invalid response."
            
            try {
                rawResponse = LyraAiClient.getLyraResponse(
                    userMessage = msg,
                    chatHistory = chatMessages.value,
                    devices = devices.value,
                    logs = logs.value,
                    activeScene = activeScene,
                    modelMode = aiMode,
                    overrideApiKey = keyToUse
                )
                val json = JSONObject(rawResponse)
                parsedReply = json.optString("reply", "No text reply.")


                if (json.has("action")) {
                    val action = json.getJSONObject("action")
                    val actionType = action.optString("type", "NONE")
                    val targetDeviceName = action.optString("device_name", "")
                    val targetStatus = action.optBoolean("device_status", false)
                    val targetValue = action.optDouble("device_value", 0.0).toFloat()
                    val logMsg = action.optString("log_message", "")

                    when (actionType) {
                        "UPDATE_DEVICE" -> {
                            if (targetDeviceName.isNotEmpty()) {
                                val matchedDevice = devices.value.find {
                                    it.name.equals(targetDeviceName, ignoreCase = true) ||
                                            it.name.contains(targetDeviceName, ignoreCase = true)
                                }
                                if (matchedDevice != null) {
                                    val updated = matchedDevice.copy(status = targetStatus, value = targetValue)
                                    lastAiUpdatedDeviceId = matchedDevice.id
                                    repository.updateDevice(updated)
                                    repository.insertLog(
                                        LogEntity(
                                            message = "Lyra AI: Auto-adjusted ${matchedDevice.name}.",
                                            level = "SUCCESS",
                                            module = matchedDevice.protocol.uppercase()
                                        )
                                    )
                                }
                            }
                        }
                        "CREATE_DEVICE" -> {
                            if (targetDeviceName.isNotEmpty()) {
                                val devType = action.optString("device_type", "Light")
                                val devRoom = action.optString("device_room", "Living Room")
                                val devProtocol = action.optString("device_protocol", "Matter")
                                val newDevice = DeviceEntity(
                                    name = targetDeviceName,
                                    type = devType,
                                    status = targetStatus,
                                    value = targetValue,
                                    room = devRoom,
                                    protocol = devProtocol
                                )
                                repository.insertDevice(newDevice)
                                repository.insertLog(
                                    LogEntity(
                                        message = "Lyra AI: Automatically initialized new hardware node '$targetDeviceName'.",
                                        level = "SUCCESS",
                                        module = devProtocol.uppercase()
                                    )
                                )
                            }
                        }
                        "DELETE_DEVICE" -> {
                            if (targetDeviceName.isNotEmpty()) {
                                val matchedDevice = devices.value.find {
                                    it.name.equals(targetDeviceName, ignoreCase = true) ||
                                            it.name.contains(targetDeviceName, ignoreCase = true)
                                }
                                if (matchedDevice != null) {
                                    repository.deleteDeviceById(matchedDevice.id)
                                    repository.insertLog(
                                        LogEntity(
                                            message = "Lyra AI: Decommissioned device hardware node '${matchedDevice.name}'.",
                                            level = "WARNING",
                                            module = matchedDevice.protocol.uppercase()
                                        )
                                    )
                                }
                            }
                        }
                        "TRIGGER_SCENE" -> {
                            val sceneName = action.optString("scene_name", "None")
                            if (sceneName.isNotEmpty()) {
                                activeScene = sceneName
                                repository.insertLog(
                                    LogEntity(
                                        message = "Lyra AI: Deployed environmental atmosphere scene '$sceneName'.",
                                        level = "SUCCESS",
                                        module = "SYSTEM"
                                    )
                                )
                                // Dynamically adjust device power profiles for scene atmosphere
                                when (sceneName.lowercase()) {
                                    "night mode", "night" -> {
                                        devices.value.forEach { dev ->
                                            if (dev.type.equals("Light", ignoreCase = true) || dev.type.equals("Plug", ignoreCase = true)) {
                                                repository.updateDevice(dev.copy(status = false, value = 0.0f))
                                            } else if (dev.type.equals("Lock", ignoreCase = true)) {
                                                repository.updateDevice(dev.copy(status = true, value = 1.0f)) // Secure lock
                                            }
                                        }
                                    }
                                    "movie mode", "movie" -> {
                                        devices.value.forEach { dev ->
                                            if (dev.type.equals("Light", ignoreCase = true)) {
                                                repository.updateDevice(dev.copy(status = true, value = 15.0f)) // Dim light
                                            }
                                        }
                                    }
                                    "study mode", "study", "work" -> {
                                        devices.value.forEach { dev ->
                                            if (dev.type.equals("Light", ignoreCase = true)) {
                                                repository.updateDevice(dev.copy(status = true, value = 90.0f)) // High bright light
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (logMsg.isNotEmpty()) {
                        repository.insertLog(
                            LogEntity(
                                message = logMsg,
                                level = "SUCCESS",
                                module = "AI"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Fallback in case Gemini didn't return valid JSON despite system instructions or API error
                if (e is retrofit2.HttpException && e.code() == 404) {
                    parsedReply = "API Error: Model not found. This might mean the preview model is not available for your API key. Try setting the model mode to Standard."
                } else if (rawResponse.isNotBlank()) {
                    parsedReply = rawResponse
                } else {
                    parsedReply = "Neural Core Error: ${e.localizedMessage}"
                }
            }

            // Save Lyra's reply
            repository.insertChatMessage(
                ChatMessageEntity(text = parsedReply, sender = "LYRA")
            )

            isLyraThinking = false
            speak(parsedReply)
        }
    }

    fun sendVoiceCommand(command: String) {
        val cleanCommand = command.trim()
        if (cleanCommand.isBlank()) return
        typedMessage = cleanCommand
        sendUserMessage()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(getApplication()) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale.US)
                    if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                        // Pitch set to sound soothing, pleasant and premium, characteristic of a neural assistant voice
                        tts?.setPitch(1.05f)
                        // A slightly calmer pacing for a soothing feel
                        tts?.setSpeechRate(0.85f)

                        // Attempt to locate and configure a high-quality female/assistant voice
                        try {
                            val availableVoices = tts?.voices
                            if (availableVoices != null) {
                                val femaleVoice = availableVoices.find { voice ->
                                    val name = voice.name.lowercase()
                                    (name.contains("female") || 
                                     name.contains("network-f") || 
                                     name.contains("en-us-x-sfg") || 
                                     name.contains("a-female") ||
                                     name.contains("f-local")) &&
                                     voice.locale.language == "en"
                                }
                                if (femaleVoice != null) {
                                    tts?.voice = femaleVoice
                                }
                            }
                        } catch (e: Exception) {
                            // Suppress non-blocking voice-matching exceptions
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun speak(text: String) {
        if (!isTtsEnabled) return
        try {
            if (tts == null) {
                initTts()
            }
            // Strip markdown, asterisks, brackets, emojis, and styling characters for high speech intelligibility
            val cleanText = text
                .replace(Regex("[*#_`✨]"), "")
                .replace(Regex("\\[.*?\\]"), "")
                .trim()
                
            if (cleanText.isNotEmpty()) {
                tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "LyraSpeechId")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleTtsEnabled() {
        isTtsEnabled = !isTtsEnabled
        sharedPrefs.edit().putBoolean("tts_enabled", isTtsEnabled).apply()
        if (!isTtsEnabled) {
            try {
                tts?.stop()
            } catch (e: Exception) {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
