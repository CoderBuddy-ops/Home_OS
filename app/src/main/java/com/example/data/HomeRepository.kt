package com.example.data

import kotlinx.coroutines.flow.Flow

class HomeRepository(private val db: AppDatabase) {
    val devices: Flow<List<DeviceEntity>> = db.deviceDao().getAllDevices()
    val logs: Flow<List<LogEntity>> = db.logDao().getRecentLogs()
    val chatMessages: Flow<List<ChatMessageEntity>> = db.chatDao().getChatMessages()

    suspend fun insertDevice(device: DeviceEntity) {
        db.deviceDao().insertDevice(device)
    }

    suspend fun updateDevice(device: DeviceEntity) {
        db.deviceDao().updateDevice(device)
    }

    suspend fun deleteDeviceById(id: Int) {
        db.deviceDao().deleteDeviceById(id)
    }

    suspend fun insertLog(log: LogEntity) {
        db.logDao().insertLog(log)
    }

    suspend fun clearLogs() {
        db.logDao().clearLogs()
    }

    suspend fun insertChatMessage(message: ChatMessageEntity) {
        db.chatDao().insertMessage(message)
    }

    suspend fun clearChat() {
        db.chatDao().clearChat()
    }

    suspend fun seedInitialDataIfEmpty() {
        if (db.deviceDao().getCount() == 0) {
            // Seed default smart devices
            val defaultDevices = listOf(
                DeviceEntity(
                    name = "Living Room Chandelier",
                    type = "Light",
                    status = true,
                    value = 80f,
                    room = "Living Room",
                    protocol = "Matter"
                ),
                DeviceEntity(
                    name = "Master Nest Thermostat",
                    type = "Thermostat",
                    status = true,
                    value = 22f,
                    room = "Master Bedroom",
                    protocol = "BLE"
                ),
                DeviceEntity(
                    name = "Front Door Deadbolt",
                    type = "Lock",
                    status = true,
                    value = 1f, // 1 = Locked, 0 = Unlocked
                    room = "Entryway",
                    protocol = "MQTT"
                ),
                DeviceEntity(
                    name = "Kitchen Smart Coffee Maker",
                    type = "Plug",
                    status = false,
                    value = 0f,
                    room = "Kitchen",
                    protocol = "ESP32"
                ),
                DeviceEntity(
                    name = "Matter Bridge Hub",
                    type = "Bridge",
                    status = true,
                    value = 100f,
                    room = "Hallway",
                    protocol = "Matter"
                )
            )
            for (device in defaultDevices) {
                db.deviceDao().insertDevice(device)
            }

            // Seed initial logs
            val defaultLogs = listOf(
                LogEntity(
                    message = "Home_OS initializing... Core version v2.4.0-Stable active.",
                    level = "INFO",
                    module = "SYSTEM"
                ),
                LogEntity(
                    message = "Matter Bridge Re-encrypted - SHA256 Handshake updated.",
                    level = "SUCCESS",
                    module = "MATTER"
                ),
                LogEntity(
                    message = "Security Patch 08A Applied - AES keys cycled.",
                    level = "SUCCESS",
                    module = "SECURITY"
                ),
                LogEntity(
                    message = "ESP32 nodes synchronized successfully via MQTT local broker.",
                    level = "SUCCESS",
                    module = "MQTT"
                ),
                LogEntity(
                    message = "BLE peripheral scan: found smart thermostat.",
                    level = "INFO",
                    module = "BLE"
                )
            )
            for (log in defaultLogs) {
                db.logDao().insertLog(log)
            }

            // Seed introductory chat message
            db.chatDao().insertMessage(
                ChatMessageEntity(
                    text = "Welcome to Home_OS. I am Lyra, the neural heart of your smart home. All systems are operating within nominal parameters. How can I assist you today?",
                    sender = "LYRA"
                )
            )
        }
    }
}
