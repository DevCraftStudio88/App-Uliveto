package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.*
import com.example.ui.components.FarmBackButton
import com.example.ui.components.FarmDateSelector
import com.example.ui.components.FarmPrimaryButton
import com.example.ui.components.FarmTimeSelector
import com.example.ui.theme.*
import java.util.Calendar
import java.util.UUID

enum class PromemoriaTab {
    OGGI, PROSSIME, COMPLETATE, ARCHIVIATE
}

@Composable
fun PromemoriaScreen(
    onBack: () -> Unit,
    onNavigateToZone: ((Field) -> Unit)? = null,
    initialFilter: PromemoriaTab = PromemoriaTab.OGGI,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val reminders by FarmRepository.reminders.collectAsState()
    val fields by FarmRepository.fields.collectAsState()
    val tasks by FarmRepository.tasks.collectAsState()
    val notes by FarmRepository.notes.collectAsState()
    val movements by FarmRepository.movements.collectAsState()
    val settings by FarmRepository.settings.collectAsState()

    var selectedTab by remember { mutableStateOf(initialFilter) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<ReminderItem?>(null) }
    var snoozeReminderTarget by remember { mutableStateOf<ReminderItem?>(null) }
    var deleteConfirmTarget by remember { mutableStateOf<ReminderItem?>(null) }
    var detailTarget by remember { mutableStateOf<ReminderItem?>(null) }

    // Android 13+ Notification Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val todayStr = remember {
        val cal = Calendar.getInstance()
        String.format("%02d/%02d/%04d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
    }

    // Filter reminders by status and tab
    val filteredReminders = remember(reminders, selectedTab, todayStr) {
        when (selectedTab) {
            PromemoriaTab.OGGI -> {
                reminders.filter { it.status == ReminderStatus.ATTIVO && it.date == todayStr }
                    .sortedWith(compareBy({ it.time }, { it.priority.ordinal }))
            }
            PromemoriaTab.PROSSIME -> {
                reminders.filter { it.status == ReminderStatus.ATTIVO && it.date != todayStr }
                    .sortedWith(compareBy({ it.date }, { it.time }, { it.priority.ordinal }))
            }
            PromemoriaTab.COMPLETATE -> {
                reminders.filter { it.status == ReminderStatus.COMPLETATO }
                    .sortedByDescending { it.completedAt ?: it.updatedAt }
            }
            PromemoriaTab.ARCHIVIATE -> {
                reminders.filter { it.status == ReminderStatus.ARCHIVIATO }
                    .sortedByDescending { it.archivedAt ?: it.updatedAt }
            }
        }
    }

    val suggestedReminders = remember(reminders, tasks, fields) {
        FarmRepository.getSuggestedReminders()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FarmBgCream)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FarmBackButton(onBack = onBack)

            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .height(42.dp)
                    .testTag("nuovo_promemoria_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Nuovo promemoria", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Promemoria & Notifiche",
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = FarmTextPrimary
        )

        Text(
            text = "Gestione intelligente di scadenze agricole e promemoria locali",
            fontSize = 14.sp,
            color = FarmTextSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                PromemoriaTab.OGGI to "Oggi",
                PromemoriaTab.PROSSIME to "Prossime",
                PromemoriaTab.COMPLETATE to "Completate",
                PromemoriaTab.ARCHIVIATE to "Archivio"
            ).forEach { (tab, label) ->
                val isSelected = selectedTab == tab
                val count = when (tab) {
                    PromemoriaTab.OGGI -> reminders.count { it.status == ReminderStatus.ATTIVO && it.date == todayStr }
                    PromemoriaTab.PROSSIME -> reminders.count { it.status == ReminderStatus.ATTIVO && it.date != todayStr }
                    PromemoriaTab.COMPLETATE -> reminders.count { it.status == ReminderStatus.COMPLETATO }
                    PromemoriaTab.ARCHIVIATE -> reminders.count { it.status == ReminderStatus.ARCHIVIATO }
                }

                FilterChip(
                    selected = isSelected,
                    onClick = { selectedTab = tab },
                    label = {
                        Text(
                            text = if (count > 0) "$label ($count)" else label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FarmGreenDark,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Suggestions Card (If any and on OGGI tab)
        if (selectedTab == PromemoriaTab.OGGI && suggestedReminders.isNotEmpty()) {
            val suggestion = suggestedReminders.first()
            Surface(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, Color(0xFFF9A825)),
                color = Color(0xFFFFF9C4),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFFF57F17),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Suggerimento automatico",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                            Text(
                                text = suggestion.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = FarmTextPrimary
                            )
                            Text(
                                text = suggestion.reason,
                                fontSize = 11.sp,
                                color = FarmTextSecondary
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val newItem = ReminderItem(
                                title = suggestion.title,
                                description = suggestion.description,
                                date = suggestion.suggestedDate,
                                time = suggestion.suggestedTime,
                                category = suggestion.category,
                                priority = suggestion.priority,
                                linkedEntityType = suggestion.linkedEntityType,
                                linkedEntityId = suggestion.linkedEntityId,
                                linkedEntityTitle = suggestion.linkedEntityTitle
                            )
                            FarmRepository.addReminder(newItem, context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Attiva", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // List of Reminders
        if (filteredReminders.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.NotificationsNone,
                        contentDescription = null,
                        tint = FarmTextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = when (selectedTab) {
                            PromemoriaTab.OGGI -> "Nessun promemoria in programma per oggi."
                            PromemoriaTab.PROSSIME -> "Nessun promemoria futuro programmato."
                            PromemoriaTab.COMPLETATE -> "Nessun promemoria completato."
                            PromemoriaTab.ARCHIVIATE -> "Archivio promemoria vuoto."
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FarmTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tocca 'Nuovo promemoria' per programmare un avviso.",
                        fontSize = 13.sp,
                        color = FarmTextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredReminders, key = { it.id }) { reminder ->
                    ReminderCardItem(
                        reminder = reminder,
                        onToggleComplete = { FarmRepository.toggleReminderCompleted(reminder.id, context) },
                        onSnooze = { snoozeReminderTarget = reminder },
                        onEdit = { editingReminder = reminder },
                        onArchive = { FarmRepository.archiveReminder(reminder.id, context) },
                        onDelete = { deleteConfirmTarget = reminder },
                        onDetail = { detailTarget = reminder },
                        onNavigateToLinked = {
                            if (reminder.linkedEntityType == LinkedEntityType.ZONA && reminder.linkedEntityId != null) {
                                val field = fields.find { it.id == reminder.linkedEntityId }
                                if (field != null && onNavigateToZone != null) {
                                    onNavigateToZone(field)
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // Modal: Crea o Modifica Promemoria
    if (showCreateDialog || editingReminder != null) {
        val target = editingReminder
        CreateOrEditReminderDialog(
            initialReminder = target,
            fields = fields,
            onDismiss = {
                showCreateDialog = false
                editingReminder = null
            },
            onSave = { reminder ->
                if (target != null) {
                    FarmRepository.updateReminder(reminder, context)
                } else {
                    FarmRepository.addReminder(reminder, context)
                }
                showCreateDialog = false
                editingReminder = null
            }
        )
    }

    // Modal: Posticipa (Requirement 45)
    snoozeReminderTarget?.let { rem ->
        SnoozeDialog(
            reminder = rem,
            onDismiss = { snoozeReminderTarget = null },
            onSnoozeMinutes = { minutes ->
                FarmRepository.snoozeReminder(rem.id, offsetMinutes = minutes, context = context)
                snoozeReminderTarget = null
            },
            onSnoozeTomorrow = {
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                val tomStr = String.format("%02d/%02d/%04d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
                FarmRepository.snoozeReminder(rem.id, newDateStr = tomStr, context = context)
                snoozeReminderTarget = null
            },
            onSnoozeCustom = { newDate, newTime ->
                FarmRepository.snoozeReminder(rem.id, newDateStr = newDate, newTimeStr = newTime, context = context)
                snoozeReminderTarget = null
            }
        )
    }

    // Modal: Dettaglio Promemoria & Entità Collegata (Requirement 49)
    detailTarget?.let { rem ->
        ReminderDetailDialog(
            reminder = rem,
            fields = fields,
            tasks = tasks,
            notes = notes,
            movements = movements,
            onDismiss = { detailTarget = null },
            onToggleComplete = {
                FarmRepository.toggleReminderCompleted(rem.id, context)
                detailTarget = null
            },
            onSnooze = {
                snoozeReminderTarget = rem
                detailTarget = null
            },
            onEdit = {
                editingReminder = rem
                detailTarget = null
            },
            onNavigateToZone = { field ->
                detailTarget = null
                onNavigateToZone?.invoke(field)
            }
        )
    }

    // Modal: Conferma eliminazione
    deleteConfirmTarget?.let { rem ->
        AlertDialog(
            onDismissRequest = { deleteConfirmTarget = null },
            title = { Text("Eliminare promemoria?", fontWeight = FontWeight.Bold) },
            text = { Text("Vuoi davvero rimuovere '${rem.title}'? Questa azione non può essere annullata.") },
            confirmButton = {
                Button(
                    onClick = {
                        FarmRepository.deleteReminder(rem.id, context)
                        deleteConfirmTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmRedCategory)
                ) {
                    Text("Elimina", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmTarget = null }) {
                    Text("Annulla")
                }
            }
        )
    }
}

@Composable
fun ReminderCardItem(
    reminder: ReminderItem,
    onToggleComplete: () -> Unit,
    onSnooze: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onDetail: () -> Unit,
    onNavigateToLinked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompleted = reminder.status == ReminderStatus.COMPLETATO
    val isUrgent = reminder.priority == ReminderPriority.URGENTE
    val isHigh = reminder.priority == ReminderPriority.ALTA

    val priorityColor = when (reminder.priority) {
        ReminderPriority.URGENTE -> FarmRedCategory
        ReminderPriority.ALTA -> FarmOrangeCategory
        ReminderPriority.NORMALE -> FarmGreenDark
        ReminderPriority.BASSA -> FarmTextSecondary
    }

    Surface(
        onClick = onDetail,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = if (isUrgent) 2.dp else 1.2.dp,
            color = if (isUrgent) FarmRedCategory else if (isCompleted) FarmCardBorderSubtle else FarmCardBorder
        ),
        color = if (isCompleted) Color(0xFFF7F6F2) else FarmSurface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Priority Badge, Category, Time, Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Priority chip
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = priorityColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, priorityColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = reminder.priority.name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = priorityColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = reminder.category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FarmTextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = FarmTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${reminder.date} ${reminder.time}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = FarmTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title & Checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleComplete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Completa",
                        tint = if (isCompleted) FarmGreenDark else FarmTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminder.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) FarmTextSecondary else FarmTextPrimary,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )

                    if (reminder.description.isNotBlank()) {
                        Text(
                            text = reminder.description,
                            fontSize = 13.sp,
                            color = FarmTextSecondary,
                            maxLines = 2
                        )
                    }
                }
            }

            // Tags row: Zone, Recurrence, Linked Entity
            if (!reminder.zoneName.isNullOrBlank() || reminder.recurrence != RecurrenceType.NESSUNA || reminder.linkedEntityType != LinkedEntityType.NESSUNA) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!reminder.zoneName.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = FarmBlueCategory.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Terrain, contentDescription = null, tint = FarmBlueCategory, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(reminder.zoneName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = FarmBlueCategory)
                            }
                        }
                    }

                    if (reminder.recurrence != RecurrenceType.NESSUNA) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = FarmGreenDark.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Repeat, contentDescription = null, tint = FarmGreenDark, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    when (reminder.recurrence) {
                                        RecurrenceType.OGNI_GIORNO -> "Ogni giorno"
                                        RecurrenceType.OGNI_SETTIMANA -> "Ogni settimana"
                                        RecurrenceType.OGNI_MESE -> "Ogni mese"
                                        RecurrenceType.OGNI_ANNO -> "Ogni anno"
                                        RecurrenceType.PERSONALIZZATA -> "Ogni ${reminder.customRecurrenceInterval} ${reminder.customRecurrenceUnit.lowercase()}"
                                        RecurrenceType.NESSUNA -> ""
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = FarmGreenDark
                                )
                            }
                        }
                    }

                    if (reminder.linkedEntityType != LinkedEntityType.NESSUNA) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = FarmPurpleCategory.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null, tint = FarmPurpleCategory, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Collegato: ${reminder.linkedEntityType.name}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = FarmPurpleCategory)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action buttons row: Posticipa, Modifica, Elimina
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isCompleted) {
                    TextButton(
                        onClick = onSnooze,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Posticipa", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                TextButton(
                    onClick = onEdit,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Modifica", fontSize = 12.sp)
                }

                IconButton(
                    onClick = onArchive,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Archive, contentDescription = "Archivia", tint = FarmTextSecondary, modifier = Modifier.size(18.dp))
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Elimina", tint = FarmRedCategory, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun CreateOrEditReminderDialog(
    initialReminder: ReminderItem?,
    fields: List<Field>,
    onDismiss: () -> Unit,
    onSave: (ReminderItem) -> Unit
) {
    var title by remember { mutableStateOf(initialReminder?.title ?: "") }
    var description by remember { mutableStateOf(initialReminder?.description ?: "") }
    var date by remember {
        mutableStateOf(
            initialReminder?.date ?: run {
                val cal = Calendar.getInstance()
                String.format("%02d/%02d/%04d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
            }
        )
    }
    var time by remember { mutableStateOf(initialReminder?.time ?: "08:00") }
    var priority by remember { mutableStateOf(initialReminder?.priority ?: ReminderPriority.NORMALE) }
    var category by remember { mutableStateOf(initialReminder?.category ?: "Generale") }
    var selectedZoneId by remember { mutableStateOf(initialReminder?.zoneId) }
    var plantId by remember { mutableStateOf(initialReminder?.plantId ?: "") }
    var recurrence by remember { mutableStateOf(initialReminder?.recurrence ?: RecurrenceType.NESSUNA) }
    var customInterval by remember { mutableIntStateOf(initialReminder?.customRecurrenceInterval ?: 1) }
    var customUnit by remember { mutableStateOf(initialReminder?.customRecurrenceUnit ?: "GIORNI") }
    var recurrenceEndDate by remember { mutableStateOf(initialReminder?.recurrenceEndDate ?: "") }
    var notificationEnabled by remember { mutableStateOf(initialReminder?.notificationEnabled ?: true) }

    val categories = listOf("Generale", "Trattamento", "Irrigazione", "Raccolta", "Manutenzione", "Scadenze", "Magazzino")
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialReminder != null) "Modifica Promemoria" else "Nuovo Promemoria",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Titolo
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titolo *") },
                    placeholder = { Text("es. Irrigazione settore Ulivi Nord") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Descrizione / Memo
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Memo / Descrizione (opzionale)") },
                    placeholder = { Text("Dettagli o note operative...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                // Data (Componente Calendario)
                FarmDateSelector(
                    selectedDateStr = date,
                    onDateSelected = { date = it },
                    label = "Data di scadenza"
                )

                // Ora
                FarmTimeSelector(
                    selectedTimeStr = time,
                    onTimeSelected = { time = it },
                    label = "Orario promemoria"
                )

                // Priorità
                Text("Priorità", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ReminderPriority.values().forEach { prio ->
                        val isSelected = priority == prio
                        FilterChip(
                            selected = isSelected,
                            onClick = { priority = prio },
                            label = { Text(prio.name, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (prio) {
                                    ReminderPriority.URGENTE -> FarmRedCategory
                                    ReminderPriority.ALTA -> FarmOrangeCategory
                                    ReminderPriority.NORMALE -> FarmGreenDark
                                    ReminderPriority.BASSA -> FarmTextSecondary
                                },
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Categoria
                Text("Categoria", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }

                // Zona
                Text("Zona / Terreno (opzionale)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedZoneId == null,
                        onClick = { selectedZoneId = null },
                        label = { Text("Nessuna") }
                    )
                    fields.forEach { field ->
                        FilterChip(
                            selected = selectedZoneId == field.id,
                            onClick = { selectedZoneId = field.id },
                            label = { Text(field.name) }
                        )
                    }
                }

                // Pianta / Dettaglio specifico
                OutlinedTextField(
                    value = plantId,
                    onValueChange = { plantId = it },
                    label = { Text("Pianta / Riferimento specifico (opzionale)") },
                    placeholder = { Text("es. Filare 3 o Albero #42") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Ricorrenza
                Text("Ricorrenza", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        RecurrenceType.NESSUNA to "Nessuna",
                        RecurrenceType.OGNI_GIORNO to "Ogni giorno",
                        RecurrenceType.OGNI_SETTIMANA to "Ogni settimana",
                        RecurrenceType.OGNI_MESE to "Ogni mese",
                        RecurrenceType.OGNI_ANNO to "Ogni anno",
                        RecurrenceType.PERSONALIZZATA to "Personalizzata"
                    ).forEach { (rec, label) ->
                        FilterChip(
                            selected = recurrence == rec,
                            onClick = { recurrence = rec },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                if (recurrence == RecurrenceType.PERSONALIZZATA) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customInterval.toString(),
                            onValueChange = { customInterval = it.toIntOrNull() ?: 1 },
                            label = { Text("Ogni") },
                            modifier = Modifier.weight(1f)
                        )
                        Row(modifier = Modifier.weight(2f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("GIORNI", "SETTIMANE", "MESI").forEach { unit ->
                                FilterChip(
                                    selected = customUnit == unit,
                                    onClick = { customUnit = unit },
                                    label = { Text(unit.lowercase(), fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                // Notifica Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Notifica sul dispositivo", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Avviso sonoro/visivo locale", fontSize = 12.sp, color = FarmTextSecondary)
                    }
                    Switch(
                        checked = notificationEnabled,
                        onCheckedChange = { notificationEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = FarmGreenDark)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val zoneName = fields.find { it.id == selectedZoneId }?.name
                        val linkedType = if (selectedZoneId != null) LinkedEntityType.ZONA else initialReminder?.linkedEntityType ?: LinkedEntityType.NESSUNA
                        val linkedId = selectedZoneId ?: initialReminder?.linkedEntityId
                        val linkedTitle = zoneName ?: initialReminder?.linkedEntityTitle

                        val item = (initialReminder ?: ReminderItem(title = title, date = date)).copy(
                            title = title.trim(),
                            description = description.trim(),
                            date = date,
                            time = time,
                            priority = priority,
                            category = category,
                            zoneId = selectedZoneId,
                            zoneName = zoneName,
                            plantId = plantId.trim().ifBlank { null },
                            recurrence = recurrence,
                            customRecurrenceInterval = customInterval,
                            customRecurrenceUnit = customUnit,
                            recurrenceEndDate = recurrenceEndDate.ifBlank { null },
                            notificationEnabled = notificationEnabled,
                            linkedEntityType = linkedType,
                            linkedEntityId = linkedId,
                            linkedEntityTitle = linkedTitle
                        )
                        onSave(item)
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark)
            ) {
                Text("Salva", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

@Composable
fun SnoozeDialog(
    reminder: ReminderItem,
    onDismiss: () -> Unit,
    onSnoozeMinutes: (Int) -> Unit,
    onSnoozeTomorrow: () -> Unit,
    onSnoozeCustom: (String, String) -> Unit
) {
    var isCustomMode by remember { mutableStateOf(false) }
    var customDate by remember { mutableStateOf(reminder.date) }
    var customTime by remember { mutableStateOf(reminder.time) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Posticipa promemoria", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Scegli di quanto posticipare '${reminder.title}':",
                    fontSize = 14.sp,
                    color = FarmTextSecondary
                )

                if (!isCustomMode) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onSnoozeMinutes(10) },
                            colors = ButtonDefaults.buttonColors(containerColor = FarmSurface),
                            border = BorderStroke(1.dp, FarmCardBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("10 min", color = FarmTextPrimary, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onSnoozeMinutes(30) },
                            colors = ButtonDefaults.buttonColors(containerColor = FarmSurface),
                            border = BorderStroke(1.dp, FarmCardBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("30 min", color = FarmTextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onSnoozeMinutes(60) },
                            colors = ButtonDefaults.buttonColors(containerColor = FarmSurface),
                            border = BorderStroke(1.dp, FarmCardBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("1 ora", color = FarmTextPrimary, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onSnoozeTomorrow,
                            colors = ButtonDefaults.buttonColors(containerColor = FarmSurface),
                            border = BorderStroke(1.dp, FarmCardBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Domani", color = FarmTextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = { isCustomMode = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scegli data e ora precisa")
                    }
                } else {
                    FarmDateSelector(
                        selectedDateStr = customDate,
                        onDateSelected = { customDate = it },
                        label = "Nuova data"
                    )

                    FarmTimeSelector(
                        selectedTimeStr = customTime,
                        onTimeSelected = { customTime = it },
                        label = "Nuova ora"
                    )
                }
            }
        },
        confirmButton = {
            if (isCustomMode) {
                Button(
                    onClick = { onSnoozeCustom(customDate, customTime) },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark)
                ) {
                    Text("Conferma", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

@Composable
fun ReminderDetailDialog(
    reminder: ReminderItem,
    fields: List<Field>,
    tasks: List<TaskItem>,
    notes: List<NoteItem>,
    movements: List<EconomicMovement>,
    onDismiss: () -> Unit,
    onToggleComplete: () -> Unit,
    onSnooze: () -> Unit,
    onEdit: () -> Unit,
    onNavigateToZone: (Field) -> Unit
) {
    val isCompleted = reminder.status == ReminderStatus.COMPLETATO
    val linkedField = fields.find { it.id == reminder.linkedEntityId }
    val linkedTask = tasks.find { it.id == reminder.linkedEntityId }
    val linkedNote = notes.find { it.id == reminder.linkedEntityId }
    val linkedMovement = movements.find { it.id == reminder.linkedEntityId }

    val isEntityMissing = reminder.linkedEntityType != LinkedEntityType.NESSUNA &&
            linkedField == null && linkedTask == null && linkedNote == null && linkedMovement == null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = FarmGreenDark,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(reminder.title, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (reminder.description.isNotBlank()) {
                    Text(reminder.description, fontSize = 14.sp, color = FarmTextPrimary)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = FarmBgCream,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("📅 Data: ${reminder.date} ore ${reminder.time}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("⚡ Priorità: ${reminder.priority.name}", fontSize = 13.sp)
                        Text("🏷 Categoria: ${reminder.category}", fontSize = 13.sp)
                        if (!reminder.zoneName.isNullOrBlank()) {
                            Text("🌳 Zona: ${reminder.zoneName}", fontSize = 13.sp)
                        }
                    }
                }

                // Collegamento entità (Requirement 49)
                if (reminder.linkedEntityType != LinkedEntityType.NESSUNA) {
                    if (isEntityMissing) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFEBEE),
                            border = BorderStroke(1.dp, FarmRedCategory)
                        ) {
                            Text(
                                text = "⚠️ Elemento non più disponibile (è stato eliminato)",
                                fontSize = 12.sp,
                                color = FarmRedCategory,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    } else if (linkedField != null) {
                        Button(
                            onClick = { onNavigateToZone(linkedField) },
                            colors = ButtonDefaults.buttonColors(containerColor = FarmBlueCategory),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Terrain, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Apri scheda zona: ${linkedField.name}")
                        }
                    } else {
                        Text(
                            text = "🔗 Collegato a: ${reminder.linkedEntityTitle ?: reminder.linkedEntityType.name}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FarmPurpleCategory
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onToggleComplete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCompleted) FarmTextSecondary else FarmGreenDark
                    )
                ) {
                    Text(if (isCompleted) "Riapri" else "Fatto", fontWeight = FontWeight.Bold)
                }

                if (!isCompleted) {
                    OutlinedButton(onClick = onSnooze) {
                        Text("Posticipa")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onEdit) {
                Text("Modifica")
            }
        }
    )
}
