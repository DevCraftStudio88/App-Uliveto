package com.example.data

import java.util.UUID

enum class DueCategory {
    OGGI, DOMANI, SELEZIONA_DATA, FATTO
}

enum class EconomicMovementType {
    ENTRATA, SPESA, INVESTIMENTO
}

enum class TextSizeOption {
    NORMALE, GRANDE, ENORME
}

data class GeoPoint(
    val latitude: Double,
    val longitude: Double
)

data class FarmProperty(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "La mia proprietà",
    val polygon: List<GeoPoint> = emptyList(),
    val areaSquareMeters: Double = 0.0,
    val areaHectares: Double = 0.0,
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class Field(
    val id: String = UUID.randomUUID().toString(),
    val propertyId: String? = null,
    val name: String,
    val crop: String = "",
    val areaHectares: Double = 0.0,
    val areaSquareMeters: Double = 0.0,
    val notes: String = "",
    val polygon: List<GeoPoint> = emptyList(),
    val center: GeoPoint? = null,
    val colorHex: Long = 0xFF2E7D32,
    val orderIndex: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class NoteItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val fieldId: String? = null,
    val fieldName: String? = null,
    val orderIndex: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class TaskItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val fieldName: String = "",
    val fieldId: String? = null,
    val dueCategory: DueCategory = DueCategory.OGGI,
    val dueDateStr: String = "", // Format: GG/MM/AAAA or Oggi/Domani
    val isCompleted: Boolean = false,
    val completedAt: String? = null,
    val createdAt: String = ""
)

data class EconomicMovement(
    val id: String = UUID.randomUUID().toString(),
    val type: EconomicMovementType,
    val description: String,
    val amount: Double,
    val dateStr: String, // Format: GG/MM/AAAA
    val category: String,
    val fieldId: String? = null,
    val fieldName: String? = null,
    val destination: String? = null, // Specific for INVESTIMENTO
    val notes: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class MyAssistantSettings(
    val userName: String = "",
    val assistantName: String = "Il mio assistente",
    val greetingPhrase: String = "Ciao",
    val tone: String = "Amichevole" // Amichevole, Sintetico, Professionale
)

data class NotificationSettings(
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val privacyMode: String = "DETTAGLI", // DETTAGLI, SOLO_TITOLO, NASCONDI
    val recurringRemindersEnabled: Boolean = true
)

data class AppSettings(
    val assistant: MyAssistantSettings = MyAssistantSettings(),
    val textSize: TextSizeOption = TextSizeOption.NORMALE,
    val remindersEnabled: Boolean = true,
    val notificationSettings: NotificationSettings = NotificationSettings(),
    val lastSyncTime: String = ""
)

enum class ReminderPriority {
    BASSA, NORMALE, ALTA, URGENTE
}

enum class ReminderStatus {
    ATTIVO, COMPLETATO, ARCHIVIATO, ELIMINATO
}

enum class RecurrenceType {
    NESSUNA, OGNI_GIORNO, OGNI_SETTIMANA, OGNI_MESE, OGNI_ANNO, PERSONALIZZATA
}

enum class LinkedEntityType {
    NESSUNA, PROPRIETA, ZONA, COLTURA, PIANTA, ATTIVITA, MOVIMENTO, APPUNTO
}

data class ReminderItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val date: String, // Format: GG/MM/AAAA
    val time: String = "08:00", // Format: HH:mm
    val priority: ReminderPriority = ReminderPriority.NORMALE,
    val status: ReminderStatus = ReminderStatus.ATTIVO,
    val zoneId: String? = null,
    val zoneName: String? = null,
    val plantId: String? = null,
    val category: String = "Generale", // es. Irrigazione, Trattamento, Raccolta, Manutenzione, Fiscale
    val recurrence: RecurrenceType = RecurrenceType.NESSUNA,
    val customRecurrenceInterval: Int = 1, // e.g. every X
    val customRecurrenceUnit: String = "GIORNI", // GIORNI, SETTIMANE, MESI
    val recurrenceEndDate: String? = null, // Format: GG/MM/AAAA
    val notificationEnabled: Boolean = true,
    val notificationOffset: Int = 0, // Minutes before event, default 0 (at time)
    val linkedEntityType: LinkedEntityType = LinkedEntityType.NESSUNA,
    val linkedEntityId: String? = null,
    val linkedEntityTitle: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
    val completedAt: String? = null,
    val archivedAt: String? = null,
    val deletedAt: String? = null
)

data class ReminderSuggestion(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val suggestedDate: String,
    val suggestedTime: String = "08:00",
    val category: String = "Generale",
    val priority: ReminderPriority = ReminderPriority.NORMALE,
    val linkedEntityType: LinkedEntityType = LinkedEntityType.NESSUNA,
    val linkedEntityId: String? = null,
    val linkedEntityTitle: String? = null,
    val reason: String = ""
)

enum class ExpiryStatus {
    OK, EXPIRING_SOON, EXPIRED
}

data class ProductItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String = "",
    val quantity: String = "",
    val expiryDate: String = "",
    val expiryStatus: ExpiryStatus = ExpiryStatus.OK,
    val expiryLabel: String = "",
    val withdrawalPeriodDays: Int = 0,
    val notes: String = "",
    val createdAt: String = ""
)

data class ExtractedDiary(
    val zone: String = "",
    val task: String = "",
    val product: String = "",
    val quantity: String = "",
    val date: String = ""
)

data class DiaryEntry(
    val id: String = UUID.randomUUID().toString(),
    val zone: String = "",
    val dateStr: String = "",
    val task: String = "",
    val quantity: String = "",
    val product: String = "",
    val rawText: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

