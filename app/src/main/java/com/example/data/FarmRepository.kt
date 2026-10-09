package com.example.data

import android.content.Context
import com.example.notifications.ReminderNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.*

object FarmRepository {

    private var appContext: Context? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private const val DATA_FILE_NAME = "uliveto_user_data.json"

    // Property (Boundary)
    private val _property = MutableStateFlow<FarmProperty?>(null)
    val property: StateFlow<FarmProperty?> = _property.asStateFlow()

    // Zones / Fields (EMPTY by default)
    private val _fields = MutableStateFlow<List<Field>>(emptyList())
    val fields: StateFlow<List<Field>> = _fields.asStateFlow()

    // Notes (EMPTY by default)
    private val _notes = MutableStateFlow<List<NoteItem>>(emptyList())
    val notes: StateFlow<List<NoteItem>> = _notes.asStateFlow()

    // Tasks (EMPTY by default)
    private val _tasks = MutableStateFlow<List<TaskItem>>(emptyList())
    val tasks: StateFlow<List<TaskItem>> = _tasks.asStateFlow()

    // Economic Movements (EMPTY by default)
    private val _movements = MutableStateFlow<List<EconomicMovement>>(emptyList())
    val movements: StateFlow<List<EconomicMovement>> = _movements.asStateFlow()

    // Products / Warehouse (EMPTY by default)
    private val _products = MutableStateFlow<List<ProductItem>>(emptyList())
    val products: StateFlow<List<ProductItem>> = _products.asStateFlow()

    // Diary Entries (EMPTY by default)
    private val _diaryEntries = MutableStateFlow<List<DiaryEntry>>(emptyList())
    val diaryEntries: StateFlow<List<DiaryEntry>> = _diaryEntries.asStateFlow()

    // Reminders (EMPTY by default)
    private val _reminders = MutableStateFlow<List<ReminderItem>>(emptyList())
    val reminders: StateFlow<List<ReminderItem>> = _reminders.asStateFlow()

    // Due In-App Reminder for Popups (Requirement 44)
    private val _dueInAppReminder = MutableStateFlow<ReminderItem?>(null)
    val dueInAppReminder: StateFlow<ReminderItem?> = _dueInAppReminder.asStateFlow()

    fun dismissInAppReminder() {
        _dueInAppReminder.value = null
    }

    fun checkForDueReminders() {
        if (!_settings.value.remindersEnabled) return
        val today = getCurrentDateFormatted()
        val cal = Calendar.getInstance()
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val due = _reminders.value.firstOrNull { r ->
            if (r.status != ReminderStatus.ATTIVO) return@firstOrNull false
            if (r.date != today) return@firstOrNull false
            val parts = r.time.split(":")
            val rMinutes = if (parts.isNotEmpty()) (parts[0].toIntOrNull() ?: 0) * 60 + (if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0) else 0
            // Due now or within last 45 minutes
            currentMinutes >= rMinutes && (currentMinutes - rMinutes) <= 45
        }
        if (due != null && _dueInAppReminder.value?.id != due.id) {
            _dueInAppReminder.value = due
        }
    }

    // Settings (Default, empty userName)
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
        loadFromDisk()
        ReminderNotificationManager.rescheduleAllActiveReminders(context)
    }

    private fun getCurrentDateFormatted(): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN).format(Date())
    }

    // Geodesic Polygon Area Calculation in m² and Hectares
    fun calculatePolygonArea(points: List<GeoPoint>): Pair<Double, Double> {
        if (points.size < 3) return Pair(0.0, 0.0)
        val radius = 6378137.0 // Earth radius in meters
        var area = 0.0
        val n = points.size
        for (i in 0 until n) {
            val p1 = points[i]
            val p2 = points[(i + 1) % n]
            val lat1 = Math.toRadians(p1.latitude)
            val lat2 = Math.toRadians(p2.latitude)
            val lon1 = Math.toRadians(p1.longitude)
            val lon2 = Math.toRadians(p2.longitude)
            area += (lon2 - lon1) * (2.0 + sin(lat1) + sin(lat2))
        }
        area = abs(area * radius * radius / 2.0)
        val hectares = area / 10000.0
        return Pair(area, hectares)
    }

    // PROPERTY ACTIONS
    fun saveProperty(points: List<GeoPoint>, name: String = "La mia proprietà") {
        val (m2, ha) = calculatePolygonArea(points)
        val prop = FarmProperty(
            id = _property.value?.id ?: UUID.randomUUID().toString(),
            name = name,
            polygon = points,
            areaSquareMeters = m2,
            areaHectares = ha,
            createdAt = _property.value?.createdAt ?: getCurrentDateFormatted(),
            updatedAt = getCurrentDateFormatted()
        )
        _property.value = prop
        persistAsync()
    }

    fun deleteProperty() {
        _property.value = null
        persistAsync()
    }

    // FIELD (ZONE) ACTIONS
    fun addField(
        name: String,
        crop: String,
        notes: String,
        polygon: List<GeoPoint>,
        colorHex: Long = 0xFF2E7D32
    ) {
        val (m2, ha) = calculatePolygonArea(polygon)
        val center = if (polygon.isNotEmpty()) {
            GeoPoint(
                polygon.map { it.latitude }.average(),
                polygon.map { it.longitude }.average()
            )
        } else null

        val newField = Field(
            id = UUID.randomUUID().toString(),
            propertyId = _property.value?.id,
            name = name,
            crop = crop,
            areaSquareMeters = m2,
            areaHectares = ha,
            notes = notes,
            polygon = polygon,
            center = center,
            colorHex = colorHex,
            orderIndex = _fields.value.size,
            createdAt = getCurrentDateFormatted(),
            updatedAt = getCurrentDateFormatted()
        )
        _fields.update { it + newField }
        persistAsync()
    }

    fun updateField(
        id: String,
        name: String,
        crop: String,
        notes: String,
        polygon: List<GeoPoint>? = null,
        colorHex: Long? = null
    ) {
        _fields.update { current ->
            current.map { f ->
                if (f.id == id) {
                    val finalPoly = polygon ?: f.polygon
                    val (m2, ha) = if (polygon != null) calculatePolygonArea(finalPoly) else Pair(f.areaSquareMeters, f.areaHectares)
                    val center = if (finalPoly.isNotEmpty()) {
                        GeoPoint(
                            finalPoly.map { it.latitude }.average(),
                            finalPoly.map { it.longitude }.average()
                        )
                    } else f.center

                    f.copy(
                        name = name,
                        crop = crop,
                        notes = notes,
                        polygon = finalPoly,
                        areaSquareMeters = m2,
                        areaHectares = ha,
                        center = center,
                        colorHex = colorHex ?: f.colorHex,
                        updatedAt = getCurrentDateFormatted()
                    )
                } else f
            }
        }
        persistAsync()
    }

    fun moveFieldUp(id: String) {
        val list = _fields.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index > 0) {
            val item = list.removeAt(index)
            list.add(index - 1, item)
            _fields.value = list.mapIndexed { idx, field -> field.copy(orderIndex = idx) }
            persistAsync()
        }
    }

    fun moveFieldDown(id: String) {
        val list = _fields.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index in 0 until list.size - 1) {
            val item = list.removeAt(index)
            list.add(index + 1, item)
            _fields.value = list.mapIndexed { idx, field -> field.copy(orderIndex = idx) }
            persistAsync()
        }
    }

    fun deleteField(id: String) {
        _fields.update { current -> current.filterNot { it.id == id } }
        persistAsync()
    }

    // NOTES (APPUNTI) CRUD
    fun addNote(text: String, fieldId: String? = null, fieldName: String? = null) {
        val newNote = NoteItem(
            id = UUID.randomUUID().toString(),
            text = text,
            fieldId = fieldId,
            fieldName = fieldName,
            orderIndex = _notes.value.size,
            createdAt = getCurrentDateFormatted(),
            updatedAt = getCurrentDateFormatted()
        )
        _notes.update { listOf(newNote) + it }
        persistAsync()
    }

    fun updateNote(id: String, newText: String, fieldId: String? = null, fieldName: String? = null) {
        _notes.update { current ->
            current.map { note ->
                if (note.id == id) {
                    note.copy(
                        text = newText,
                        fieldId = fieldId ?: note.fieldId,
                        fieldName = fieldName ?: note.fieldName,
                        updatedAt = getCurrentDateFormatted()
                    )
                } else note
            }
        }
        persistAsync()
    }

    fun moveNoteUp(id: String) {
        val list = _notes.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index > 0) {
            val item = list.removeAt(index)
            list.add(index - 1, item)
            _notes.value = list.mapIndexed { idx, note -> note.copy(orderIndex = idx) }
            persistAsync()
        }
    }

    fun moveNoteDown(id: String) {
        val list = _notes.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index in 0 until list.size - 1) {
            val item = list.removeAt(index)
            list.add(index + 1, item)
            _notes.value = list.mapIndexed { idx, note -> note.copy(orderIndex = idx) }
            persistAsync()
        }
    }

    fun deleteNote(id: String) {
        _notes.update { current -> current.filterNot { it.id == id } }
        persistAsync()
    }

    // TASKS CRUD
    fun addTask(title: String, fieldName: String, fieldId: String? = null, category: DueCategory, dueDateStr: String) {
        val newTask = TaskItem(
            id = UUID.randomUUID().toString(),
            title = title,
            fieldName = fieldName,
            fieldId = fieldId,
            dueCategory = category,
            dueDateStr = dueDateStr,
            isCompleted = false,
            createdAt = getCurrentDateFormatted()
        )
        _tasks.update { listOf(newTask) + it }
        persistAsync()
    }

    fun toggleTask(taskId: String) {
        _tasks.update { current ->
            current.map {
                if (it.id == taskId) {
                    it.copy(
                        isCompleted = !it.isCompleted,
                        completedAt = if (!it.isCompleted) getCurrentDateFormatted() else null
                    )
                } else it
            }
        }
        persistAsync()
    }

    fun deleteTask(taskId: String) {
        _tasks.update { current -> current.filterNot { it.id == taskId } }
        persistAsync()
    }

    // ECONOMIC MOVEMENTS CRUD
    fun addMovement(
        type: EconomicMovementType,
        description: String,
        amount: Double,
        dateStr: String,
        category: String,
        fieldId: String? = null,
        fieldName: String? = null,
        destination: String? = null,
        notes: String = ""
    ) {
        val mov = EconomicMovement(
            id = UUID.randomUUID().toString(),
            type = type,
            description = description,
            amount = amount,
            dateStr = dateStr,
            category = category,
            fieldId = fieldId,
            fieldName = fieldName,
            destination = destination,
            notes = notes,
            createdAt = getCurrentDateFormatted(),
            updatedAt = getCurrentDateFormatted()
        )
        _movements.update { listOf(mov) + it }
        persistAsync()
    }

    fun updateMovement(
        id: String,
        type: EconomicMovementType,
        description: String,
        amount: Double,
        dateStr: String,
        category: String,
        fieldId: String? = null,
        fieldName: String? = null,
        destination: String? = null,
        notes: String = ""
    ) {
        _movements.update { current ->
            current.map { mov ->
                if (mov.id == id) {
                    mov.copy(
                        type = type,
                        description = description,
                        amount = amount,
                        dateStr = dateStr,
                        category = category,
                        fieldId = fieldId,
                        fieldName = fieldName,
                        destination = destination,
                        notes = notes,
                        updatedAt = getCurrentDateFormatted()
                    )
                } else mov
            }
        }
        persistAsync()
    }

    fun deleteMovement(id: String) {
        _movements.update { current -> current.filterNot { it.id == id } }
        persistAsync()
    }

    // PRODUCTS (MAGAZZINO)
    fun addProduct(name: String, category: String, qty: String, expiryLabel: String, withdrawalDays: Int) {
        val newProd = ProductItem(
            id = UUID.randomUUID().toString(),
            name = name,
            category = category,
            quantity = qty,
            expiryLabel = expiryLabel,
            expiryDate = expiryLabel,
            expiryStatus = ExpiryStatus.OK,
            withdrawalPeriodDays = withdrawalDays,
            createdAt = getCurrentDateFormatted()
        )
        _products.update { it + newProd }
        persistAsync()
    }

    fun deleteProduct(id: String) {
        _products.update { current -> current.filterNot { it.id == id } }
        persistAsync()
    }

    fun recordProductUsage(
        fieldName: String,
        productName: String,
        amount: String,
        dateStr: String,
        withdrawalDays: Int,
        safeHarvestDate: String,
        reminder: Boolean
    ) {
        // Record in diary
        val diaryEntry = DiaryEntry(
            id = UUID.randomUUID().toString(),
            zone = fieldName,
            dateStr = dateStr,
            task = "Trattamento $productName",
            quantity = amount,
            product = productName,
            rawText = "Applicato $productName ($amount) su $fieldName. Periodo di carenza: $withdrawalDays giorni.",
            timestamp = System.currentTimeMillis()
        )
        _diaryEntries.update { listOf(diaryEntry) + it }

        // If reminder is enabled, create harvest safety task
        if (reminder && safeHarvestDate.isNotBlank()) {
            addTask(
                title = "Raccolta sicura post carenza $productName",
                fieldName = fieldName,
                category = DueCategory.SELEZIONA_DATA,
                dueDateStr = safeHarvestDate
            )
        }
        persistAsync()
    }

    // DIARY ACTIONS
    fun addDiaryEntry(raw: String, zone: String, task: String, product: String, qty: String, date: String) {
        val entry = DiaryEntry(
            id = UUID.randomUUID().toString(),
            zone = zone,
            dateStr = date,
            task = task,
            quantity = qty,
            product = product,
            rawText = raw,
            timestamp = System.currentTimeMillis()
        )
        _diaryEntries.update { listOf(entry) + it }
        persistAsync()
    }

    // REMINDER ACTIONS
    fun addReminder(reminder: ReminderItem, context: Context? = appContext) {
        val itemWithDate = if (reminder.createdAt.isBlank()) {
            reminder.copy(createdAt = getCurrentDateFormatted(), updatedAt = getCurrentDateFormatted())
        } else reminder
        _reminders.update { listOf(itemWithDate) + it }
        persistAsync()
        context?.let { ctx ->
            ReminderNotificationManager.scheduleReminderAlarm(ctx, itemWithDate)
        }
    }

    fun updateReminder(reminder: ReminderItem, context: Context? = appContext) {
        val updated = reminder.copy(updatedAt = getCurrentDateFormatted())
        _reminders.update { list ->
            list.map { if (it.id == updated.id) updated else it }
        }
        persistAsync()
        context?.let { ctx ->
            ReminderNotificationManager.scheduleReminderAlarm(ctx, updated)
        }
    }

    fun toggleReminderCompleted(reminderId: String, context: Context? = appContext) {
        val found = _reminders.value.find { it.id == reminderId } ?: return
        val newStatus = if (found.status == ReminderStatus.COMPLETATO) ReminderStatus.ATTIVO else ReminderStatus.COMPLETATO
        val updated = found.copy(
            status = newStatus,
            completedAt = if (newStatus == ReminderStatus.COMPLETATO) getCurrentDateFormatted() else null,
            updatedAt = getCurrentDateFormatted()
        )
        updateReminder(updated, context)
    }

    fun deleteReminder(reminderId: String, context: Context? = appContext) {
        context?.let { ctx ->
            ReminderNotificationManager.cancelReminderAlarm(ctx, reminderId)
        }
        _reminders.update { list -> list.filterNot { it.id == reminderId } }
        persistAsync()
    }

    fun archiveReminder(reminderId: String, context: Context? = appContext) {
        context?.let { ctx ->
            ReminderNotificationManager.cancelReminderAlarm(ctx, reminderId)
        }
        _reminders.update { list ->
            list.map {
                if (it.id == reminderId) it.copy(
                    status = ReminderStatus.ARCHIVIATO,
                    archivedAt = getCurrentDateFormatted(),
                    updatedAt = getCurrentDateFormatted()
                ) else it
            }
        }
        persistAsync()
    }

    fun snoozeReminder(
        reminderId: String,
        offsetMinutes: Int? = null,
        newDateStr: String? = null,
        newTimeStr: String? = null,
        context: Context? = appContext
    ) {
        val found = _reminders.value.find { it.id == reminderId } ?: return
        var finalDate = found.date
        var finalTime = found.time

        if (offsetMinutes != null) {
            val cal = Calendar.getInstance()
            val reminderTriggerMs = ReminderNotificationManager.calculateTriggerTimeMs(found.date, found.time, 0)
            if (reminderTriggerMs > 0L) {
                cal.timeInMillis = reminderTriggerMs
            }
            cal.add(Calendar.MINUTE, offsetMinutes)
            finalDate = String.format("%02d/%02d/%04d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
            finalTime = String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        } else {
            if (!newDateStr.isNullOrBlank()) finalDate = newDateStr
            if (!newTimeStr.isNullOrBlank()) finalTime = newTimeStr
        }

        val updated = found.copy(
            date = finalDate,
            time = finalTime,
            status = ReminderStatus.ATTIVO,
            updatedAt = getCurrentDateFormatted()
        )
        updateReminder(updated, context)
        if (_dueInAppReminder.value?.id == reminderId) {
            _dueInAppReminder.value = null
        }
    }

    // PROMEMORIA AUTOMATICI DALL'APP (Requirement 51 - Proposte basate su dati reali)
    fun getSuggestedReminders(): List<ReminderSuggestion> {
        val suggestions = mutableListOf<ReminderSuggestion>()
        val existingTitles = _reminders.value.map { it.title.lowercase(Locale.ROOT) }.toSet()

        // 1. Attività con data programmata ma senza promemoria
        _tasks.value.filter { !it.isCompleted && it.dueDateStr.isNotBlank() }.take(2).forEach { task ->
            val title = "Attività: ${task.title}"
            if (!existingTitles.contains(title.lowercase(Locale.ROOT))) {
                val date = if (task.dueDateStr.contains("/")) task.dueDateStr else getCurrentDateFormatted()
                suggestions.add(
                    ReminderSuggestion(
                        title = title,
                        description = if (task.fieldName.isNotBlank()) "Zona: ${task.fieldName}" else "Attività programmata",
                        suggestedDate = date,
                        suggestedTime = "08:00",
                        category = "Lavoro",
                        priority = ReminderPriority.ALTA,
                        linkedEntityType = LinkedEntityType.ATTIVITA,
                        linkedEntityId = task.id,
                        linkedEntityTitle = task.title,
                        reason = "Attività in programma senza promemoria"
                    )
                )
            }
        }

        // 2. Prodotti di magazzino in scadenza o scaduti
        _products.value.filter { it.expiryStatus == ExpiryStatus.EXPIRING_SOON || it.expiryStatus == ExpiryStatus.EXPIRED }.take(2).forEach { prod ->
            val title = "Controllo magazzino: ${prod.name}"
            if (!existingTitles.contains(title.lowercase(Locale.ROOT))) {
                suggestions.add(
                    ReminderSuggestion(
                        title = title,
                        description = "Verifica prodotto (${prod.expiryDate})",
                        suggestedDate = getCurrentDateFormatted(),
                        suggestedTime = "09:00",
                        category = "Magazzino",
                        priority = ReminderPriority.NORMALE,
                        linkedEntityType = LinkedEntityType.PROPRIETA,
                        linkedEntityId = prod.id,
                        linkedEntityTitle = prod.name,
                        reason = "Scadenza prodotto da verificare"
                    )
                )
            }
        }

        return suggestions
    }

    // SETTINGS
    fun updateAssistant(userName: String, assistantName: String, greetingPhrase: String, tone: String) {
        _settings.update {
            it.copy(
                assistant = MyAssistantSettings(
                    userName = userName,
                    assistantName = assistantName,
                    greetingPhrase = greetingPhrase,
                    tone = tone
                )
            )
        }
        persistAsync()
    }

    fun setTextSize(size: TextSizeOption) {
        _settings.update { it.copy(textSize = size) }
        persistAsync()
    }

    fun updateNotificationSettings(settings: NotificationSettings) {
        _settings.update { it.copy(notificationSettings = settings) }
        persistAsync()
    }

    fun toggleReminders(context: Context? = appContext) {
        val newState = !_settings.value.remindersEnabled
        _settings.update { it.copy(remindersEnabled = newState) }
        persistAsync()
        context?.let { ctx ->
            if (newState) {
                ReminderNotificationManager.rescheduleAllActiveReminders(ctx)
            } else {
                for (rem in _reminders.value) {
                    ReminderNotificationManager.cancelReminderAlarm(ctx, rem.id)
                }
            }
        }
    }

    // DISK PERSISTENCE (JSON)
    private fun persistAsync() {
        val ctx = appContext ?: return
        scope.launch {
            try {
                val root = JSONObject()

                // Property
                _property.value?.let { p ->
                    val pObj = JSONObject().apply {
                        put("id", p.id)
                        put("name", p.name)
                        put("areaM2", p.areaSquareMeters)
                        put("areaHa", p.areaHectares)
                        put("createdAt", p.createdAt)
                        put("updatedAt", p.updatedAt)
                        val ptsArray = JSONArray()
                        p.polygon.forEach { pt ->
                            ptsArray.put(JSONObject().apply {
                                put("lat", pt.latitude)
                                put("lng", pt.longitude)
                            })
                        }
                        put("polygon", ptsArray)
                    }
                    root.put("property", pObj)
                }

                // Fields (Zones)
                val fieldsArr = JSONArray()
                _fields.value.forEach { f ->
                    val fObj = JSONObject().apply {
                        put("id", f.id)
                        put("propertyId", f.propertyId ?: "")
                        put("name", f.name)
                        put("crop", f.crop)
                        put("areaHa", f.areaHectares)
                        put("areaM2", f.areaSquareMeters)
                        put("notes", f.notes)
                        put("colorHex", f.colorHex)
                        put("orderIndex", f.orderIndex)
                        put("createdAt", f.createdAt)
                        put("updatedAt", f.updatedAt)
                        val pts = JSONArray()
                        f.polygon.forEach { pt ->
                            pts.put(JSONObject().apply {
                                put("lat", pt.latitude)
                                put("lng", pt.longitude)
                            })
                        }
                        put("polygon", pts)
                    }
                    fieldsArr.put(fObj)
                }
                root.put("fields", fieldsArr)

                // Notes
                val notesArr = JSONArray()
                _notes.value.forEach { n ->
                    notesArr.put(JSONObject().apply {
                        put("id", n.id)
                        put("text", n.text)
                        put("fieldId", n.fieldId ?: "")
                        put("fieldName", n.fieldName ?: "")
                        put("orderIndex", n.orderIndex)
                        put("createdAt", n.createdAt)
                        put("updatedAt", n.updatedAt)
                    })
                }
                root.put("notes", notesArr)

                // Tasks
                val tasksArr = JSONArray()
                _tasks.value.forEach { t ->
                    tasksArr.put(JSONObject().apply {
                        put("id", t.id)
                        put("title", t.title)
                        put("fieldName", t.fieldName)
                        put("fieldId", t.fieldId ?: "")
                        put("dueCategory", t.dueCategory.name)
                        put("dueDateStr", t.dueDateStr)
                        put("isCompleted", t.isCompleted)
                        put("completedAt", t.completedAt ?: "")
                        put("createdAt", t.createdAt)
                    })
                }
                root.put("tasks", tasksArr)

                // Economic Movements
                val movArr = JSONArray()
                _movements.value.forEach { m ->
                    movArr.put(JSONObject().apply {
                        put("id", m.id)
                        put("type", m.type.name)
                        put("description", m.description)
                        put("amount", m.amount)
                        put("dateStr", m.dateStr)
                        put("category", m.category)
                        put("fieldId", m.fieldId ?: "")
                        put("fieldName", m.fieldName ?: "")
                        put("destination", m.destination ?: "")
                        put("notes", m.notes)
                        put("createdAt", m.createdAt)
                        put("updatedAt", m.updatedAt)
                    })
                }
                root.put("movements", movArr)

                // Products
                val prodArr = JSONArray()
                _products.value.forEach { p ->
                    prodArr.put(JSONObject().apply {
                        put("id", p.id)
                        put("name", p.name)
                        put("category", p.category)
                        put("quantity", p.quantity)
                        put("expiryDate", p.expiryDate)
                        put("expiryStatus", p.expiryStatus.name)
                        put("expiryLabel", p.expiryLabel)
                        put("withdrawalPeriodDays", p.withdrawalPeriodDays)
                        put("notes", p.notes)
                        put("createdAt", p.createdAt)
                    })
                }
                root.put("products", prodArr)

                // Diary Entries
                val diaryArr = JSONArray()
                _diaryEntries.value.forEach { d ->
                    diaryArr.put(JSONObject().apply {
                        put("id", d.id)
                        put("zone", d.zone)
                        put("dateStr", d.dateStr)
                        put("task", d.task)
                        put("quantity", d.quantity)
                        put("product", d.product)
                        put("rawText", d.rawText)
                        put("timestamp", d.timestamp)
                    })
                }
                root.put("diaryEntries", diaryArr)

                // Reminders
                val remArr = JSONArray()
                _reminders.value.forEach { r ->
                    remArr.put(JSONObject().apply {
                        put("id", r.id)
                        put("title", r.title)
                        put("description", r.description)
                        put("date", r.date)
                        put("time", r.time)
                        put("priority", r.priority.name)
                        put("status", r.status.name)
                        put("zoneId", r.zoneId ?: "")
                        put("zoneName", r.zoneName ?: "")
                        put("plantId", r.plantId ?: "")
                        put("category", r.category)
                        put("recurrence", r.recurrence.name)
                        put("customInterval", r.customRecurrenceInterval)
                        put("customUnit", r.customRecurrenceUnit)
                        put("recurrenceEndDate", r.recurrenceEndDate ?: "")
                        put("notificationEnabled", r.notificationEnabled)
                        put("notificationOffset", r.notificationOffset)
                        put("linkedType", r.linkedEntityType.name)
                        put("linkedId", r.linkedEntityId ?: "")
                        put("linkedTitle", r.linkedEntityTitle ?: "")
                        put("createdAt", r.createdAt)
                        put("updatedAt", r.updatedAt)
                        put("completedAt", r.completedAt ?: "")
                        put("archivedAt", r.archivedAt ?: "")
                        put("deletedAt", r.deletedAt ?: "")
                    })
                }
                root.put("reminders", remArr)

                // Settings
                val s = _settings.value
                val sObj = JSONObject().apply {
                    put("userName", s.assistant.userName)
                    put("assistantName", s.assistant.assistantName)
                    put("greetingPhrase", s.assistant.greetingPhrase)
                    put("tone", s.assistant.tone)
                    put("textSize", s.textSize.name)
                    put("remindersEnabled", s.remindersEnabled)
                    val nsObj = JSONObject().apply {
                        put("soundEnabled", s.notificationSettings.soundEnabled)
                        put("vibrationEnabled", s.notificationSettings.vibrationEnabled)
                        put("privacyMode", s.notificationSettings.privacyMode)
                        put("recurringRemindersEnabled", s.notificationSettings.recurringRemindersEnabled)
                    }
                    put("notificationSettings", nsObj)
                }
                root.put("settings", sObj)

                val file = File(ctx.filesDir, DATA_FILE_NAME)
                file.writeText(root.toString())
            } catch (_: Exception) {
            }
        }
    }

    private fun loadFromDisk() {
        val ctx = appContext ?: return
        try {
            val file = File(ctx.filesDir, DATA_FILE_NAME)
            if (!file.exists()) {
                // Starts completely empty! No dummy data.
                return
            }
            val content = file.readText()
            if (content.isBlank()) return
            val root = JSONObject(content)

            // Property
            if (root.has("property")) {
                val pObj = root.getJSONObject("property")
                val pts = mutableListOf<GeoPoint>()
                val polyArr = pObj.optJSONArray("polygon")
                if (polyArr != null) {
                    for (i in 0 until polyArr.length()) {
                        val pt = polyArr.getJSONObject(i)
                        pts.add(GeoPoint(pt.getDouble("lat"), pt.getDouble("lng")))
                    }
                }
                _property.value = FarmProperty(
                    id = pObj.getString("id"),
                    name = pObj.getString("name"),
                    polygon = pts,
                    areaSquareMeters = pObj.optDouble("areaM2", 0.0),
                    areaHectares = pObj.optDouble("areaHa", 0.0),
                    createdAt = pObj.optString("createdAt", ""),
                    updatedAt = pObj.optString("updatedAt", "")
                )
            }

            // Fields (Zones)
            val fieldsList = mutableListOf<Field>()
            val fArr = root.optJSONArray("fields")
            if (fArr != null) {
                for (i in 0 until fArr.length()) {
                    val fObj = fArr.getJSONObject(i)
                    val pts = mutableListOf<GeoPoint>()
                    val polyArr = fObj.optJSONArray("polygon")
                    if (polyArr != null) {
                        for (j in 0 until polyArr.length()) {
                            val pt = polyArr.getJSONObject(j)
                            pts.add(GeoPoint(pt.getDouble("lat"), pt.getDouble("lng")))
                        }
                    }
                    val center = if (pts.isNotEmpty()) GeoPoint(pts.map { it.latitude }.average(), pts.map { it.longitude }.average()) else null
                    fieldsList.add(
                        Field(
                            id = fObj.getString("id"),
                            propertyId = fObj.optString("propertyId").ifBlank { null },
                            name = fObj.getString("name"),
                            crop = fObj.optString("crop", ""),
                            areaHectares = fObj.optDouble("areaHa", 0.0),
                            areaSquareMeters = fObj.optDouble("areaM2", 0.0),
                            notes = fObj.optString("notes", ""),
                            polygon = pts,
                            center = center,
                            colorHex = fObj.optLong("colorHex", 0xFF2E7D32),
                            orderIndex = fObj.optInt("orderIndex", i),
                            createdAt = fObj.optString("createdAt", ""),
                            updatedAt = fObj.optString("updatedAt", "")
                        )
                    )
                }
            }
            _fields.value = fieldsList.sortedBy { it.orderIndex }

            // Notes
            val notesList = mutableListOf<NoteItem>()
            val nArr = root.optJSONArray("notes")
            if (nArr != null) {
                for (i in 0 until nArr.length()) {
                    val nObj = nArr.getJSONObject(i)
                    notesList.add(
                        NoteItem(
                            id = nObj.getString("id"),
                            text = nObj.getString("text"),
                            fieldId = nObj.optString("fieldId").ifBlank { null },
                            fieldName = nObj.optString("fieldName").ifBlank { null },
                            orderIndex = nObj.optInt("orderIndex", i),
                            createdAt = nObj.optString("createdAt", ""),
                            updatedAt = nObj.optString("updatedAt", "")
                        )
                    )
                }
            }
            _notes.value = notesList.sortedBy { it.orderIndex }

            // Tasks
            val tasksList = mutableListOf<TaskItem>()
            val tArr = root.optJSONArray("tasks")
            if (tArr != null) {
                for (i in 0 until tArr.length()) {
                    val tObj = tArr.getJSONObject(i)
                    val catStr = tObj.optString("dueCategory", DueCategory.OGGI.name)
                    val cat = try { DueCategory.valueOf(catStr) } catch (_: Exception) { DueCategory.OGGI }
                    tasksList.add(
                        TaskItem(
                            id = tObj.getString("id"),
                            title = tObj.getString("title"),
                            fieldName = tObj.optString("fieldName", ""),
                            fieldId = tObj.optString("fieldId").ifBlank { null },
                            dueCategory = cat,
                            dueDateStr = tObj.optString("dueDateStr", ""),
                            isCompleted = tObj.optBoolean("isCompleted", false),
                            completedAt = tObj.optString("completedAt").ifBlank { null },
                            createdAt = tObj.optString("createdAt", "")
                        )
                    )
                }
            }
            _tasks.value = tasksList

            // Economic Movements
            val movList = mutableListOf<EconomicMovement>()
            val mArr = root.optJSONArray("movements")
            if (mArr != null) {
                for (i in 0 until mArr.length()) {
                    val mObj = mArr.getJSONObject(i)
                    val typeStr = mObj.optString("type", EconomicMovementType.SPESA.name)
                    val type = try { EconomicMovementType.valueOf(typeStr) } catch (_: Exception) { EconomicMovementType.SPESA }
                    movList.add(
                        EconomicMovement(
                            id = mObj.getString("id"),
                            type = type,
                            description = mObj.getString("description"),
                            amount = mObj.getDouble("amount"),
                            dateStr = mObj.getString("dateStr"),
                            category = mObj.getString("category"),
                            fieldId = mObj.optString("fieldId").ifBlank { null },
                            fieldName = mObj.optString("fieldName").ifBlank { null },
                            destination = mObj.optString("destination").ifBlank { null },
                            notes = mObj.optString("notes", ""),
                            createdAt = mObj.optString("createdAt", ""),
                            updatedAt = mObj.optString("updatedAt", "")
                        )
                    )
                }
            }
            _movements.value = movList

            // Products
            val prodList = mutableListOf<ProductItem>()
            val prodArr = root.optJSONArray("products")
            if (prodArr != null) {
                for (i in 0 until prodArr.length()) {
                    val pObj = prodArr.getJSONObject(i)
                    val statusStr = pObj.optString("expiryStatus", ExpiryStatus.OK.name)
                    val status = try { ExpiryStatus.valueOf(statusStr) } catch (_: Exception) { ExpiryStatus.OK }
                    prodList.add(
                        ProductItem(
                            id = pObj.getString("id"),
                            name = pObj.getString("name"),
                            category = pObj.optString("category", ""),
                            quantity = pObj.optString("quantity", ""),
                            expiryDate = pObj.optString("expiryDate", ""),
                            expiryStatus = status,
                            expiryLabel = pObj.optString("expiryLabel", ""),
                            withdrawalPeriodDays = pObj.optInt("withdrawalPeriodDays", 0),
                            notes = pObj.optString("notes", ""),
                            createdAt = pObj.optString("createdAt", "")
                        )
                    )
                }
            }
            _products.value = prodList

            // Diary Entries
            val dList = mutableListOf<DiaryEntry>()
            val dArr = root.optJSONArray("diaryEntries")
            if (dArr != null) {
                for (i in 0 until dArr.length()) {
                    val dObj = dArr.getJSONObject(i)
                    dList.add(
                        DiaryEntry(
                            id = dObj.getString("id"),
                            zone = dObj.optString("zone", ""),
                            dateStr = dObj.optString("dateStr", ""),
                            task = dObj.optString("task", ""),
                            quantity = dObj.optString("quantity", ""),
                            product = dObj.optString("product", ""),
                            rawText = dObj.optString("rawText", ""),
                            timestamp = dObj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }
            _diaryEntries.value = dList

            // Reminders
            val remList = mutableListOf<ReminderItem>()
            val remArr = root.optJSONArray("reminders")
            if (remArr != null) {
                for (i in 0 until remArr.length()) {
                    val rObj = remArr.getJSONObject(i)
                    val priorityStr = rObj.optString("priority", ReminderPriority.NORMALE.name)
                    val priority = try { ReminderPriority.valueOf(priorityStr) } catch (_: Exception) { ReminderPriority.NORMALE }
                    val statusStr = rObj.optString("status", ReminderStatus.ATTIVO.name)
                    val status = try { ReminderStatus.valueOf(statusStr) } catch (_: Exception) { ReminderStatus.ATTIVO }
                    val recStr = rObj.optString("recurrence", RecurrenceType.NESSUNA.name)
                    val recurrence = try { RecurrenceType.valueOf(recStr) } catch (_: Exception) { RecurrenceType.NESSUNA }
                    val linkedTypeStr = rObj.optString("linkedType", LinkedEntityType.NESSUNA.name)
                    val linkedType = try { LinkedEntityType.valueOf(linkedTypeStr) } catch (_: Exception) { LinkedEntityType.NESSUNA }

                    remList.add(
                        ReminderItem(
                            id = rObj.getString("id"),
                            title = rObj.getString("title"),
                            description = rObj.optString("description", ""),
                            date = rObj.getString("date"),
                            time = rObj.optString("time", "08:00"),
                            priority = priority,
                            status = status,
                            zoneId = rObj.optString("zoneId").ifBlank { null },
                            zoneName = rObj.optString("zoneName").ifBlank { null },
                            plantId = rObj.optString("plantId").ifBlank { null },
                            category = rObj.optString("category", "Generale"),
                            recurrence = recurrence,
                            customRecurrenceInterval = rObj.optInt("customInterval", 1),
                            customRecurrenceUnit = rObj.optString("customUnit", "GIORNI"),
                            recurrenceEndDate = rObj.optString("recurrenceEndDate").ifBlank { null },
                            notificationEnabled = rObj.optBoolean("notificationEnabled", true),
                            notificationOffset = rObj.optInt("notificationOffset", 0),
                            linkedEntityType = linkedType,
                            linkedEntityId = rObj.optString("linkedId").ifBlank { null },
                            linkedEntityTitle = rObj.optString("linkedTitle").ifBlank { null },
                            createdAt = rObj.optString("createdAt", ""),
                            updatedAt = rObj.optString("updatedAt", ""),
                            completedAt = rObj.optString("completedAt").ifBlank { null },
                            archivedAt = rObj.optString("archivedAt").ifBlank { null },
                            deletedAt = rObj.optString("deletedAt").ifBlank { null }
                        )
                    )
                }
            }
            _reminders.value = remList

            // Settings
            if (root.has("settings")) {
                val sObj = root.getJSONObject("settings")
                val textSizeStr = sObj.optString("textSize", TextSizeOption.NORMALE.name)
                val textSize = try { TextSizeOption.valueOf(textSizeStr) } catch (_: Exception) { TextSizeOption.NORMALE }
                var notifSettings = NotificationSettings()
                if (sObj.has("notificationSettings")) {
                    val nsObj = sObj.getJSONObject("notificationSettings")
                    notifSettings = NotificationSettings(
                        soundEnabled = nsObj.optBoolean("soundEnabled", true),
                        vibrationEnabled = nsObj.optBoolean("vibrationEnabled", true),
                        privacyMode = nsObj.optString("privacyMode", "DETTAGLI"),
                        recurringRemindersEnabled = nsObj.optBoolean("recurringRemindersEnabled", true)
                    )
                }
                _settings.value = AppSettings(
                    assistant = MyAssistantSettings(
                        userName = sObj.optString("userName", ""),
                        assistantName = sObj.optString("assistantName", "Il mio assistente"),
                        greetingPhrase = sObj.optString("greetingPhrase", "Ciao"),
                        tone = sObj.optString("tone", "Amichevole")
                    ),
                    textSize = textSize,
                    remindersEnabled = sObj.optBoolean("remindersEnabled", true),
                    notificationSettings = notifSettings,
                    lastSyncTime = getCurrentDateFormatted()
                )
            }
        } catch (_: Exception) {
        }
    }
}
