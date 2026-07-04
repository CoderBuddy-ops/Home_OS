package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // Light, Thermostat, Lock, Plug, Bridge
    val status: Boolean, // On / Off
    val value: Float, // Brighness (0-100), Temp (16-30), etc.
    val room: String, // Living Room, Kitchen, etc.
    val protocol: String, // Matter, MQTT, BLE, ESP32
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "system_logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val message: String,
    val level: String, // INFO, SUCCESS, WARNING
    val timestamp: Long = System.currentTimeMillis(),
    val module: String // SECURITY, MATTER, MQTT, BLE, ESP32, SYSTEM
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val sender: String, // USER, LYRA
    val timestamp: Long = System.currentTimeMillis()
)
