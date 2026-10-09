package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onNavigateToOggi: () -> Unit,
    onNavigateToTerreni: () -> Unit,
    onNavigateToAppunti: () -> Unit,
    onNavigateToSpese: () -> Unit,
    onNavigateToDiario: () -> Unit,
    onNavigateToCentroOperativo: () -> Unit,
    onNavigateToPromemoria: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tasks by FarmRepository.tasks.collectAsState()
    val fields by FarmRepository.fields.collectAsState()
    val notes by FarmRepository.notes.collectAsState()
    val movements by FarmRepository.movements.collectAsState()
    val settings by FarmRepository.settings.collectAsState()
    val reminders by FarmRepository.reminders.collectAsState()

    var showQuickMemoDialog by remember { mutableStateOf(false) }
    var memoToReminderItem by remember { mutableStateOf<ReminderItem?>(null) }

    // Real dynamic date in Italian
    val currentDateStr = remember {
        val cal = Calendar.getInstance()
        val dayName = SimpleDateFormat("EEEE", Locale.ITALIAN).format(cal.time)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ITALIAN) else it.toString() }
        val dayNumber = cal.get(Calendar.DAY_OF_MONTH)
        val monthName = SimpleDateFormat("MMMM", Locale.ITALIAN).format(cal.time)
        val year = cal.get(Calendar.YEAR)
        "$dayName $dayNumber $monthName $year"
    }

    val todayFormatted = remember {
        val cal = Calendar.getInstance()
        String.format("%02d/%02d/%04d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
    }

    val pendingToday = tasks.count {
        (it.dueDateStr == "Oggi" || it.dueDateStr == todayFormatted || it.dueCategory == DueCategory.OGGI) && !it.isCompleted
    }

    // Dynamic greeting based on user settings
    val greetingText = remember(settings.assistant) {
        val phrase = settings.assistant.greetingPhrase.ifBlank { "Ciao" }
        val name = settings.assistant.userName.trim()
        if (name.isNotBlank()) "$phrase $name!" else "$phrase!"
    }

    // Active reminders for today (Requirement 52)
    val todayReminders = remember(reminders, todayFormatted) {
        reminders.filter { it.status == ReminderStatus.ATTIVO && it.date == todayFormatted }
            .sortedBy { it.time }
    }
    val nextReminder = todayReminders.firstOrNull() ?: reminders.filter { it.status == ReminderStatus.ATTIVO }.sortedWith(compareBy({ it.date }, { it.time })).firstOrNull()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FarmBgCream)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Top Bar: Dynamic greeting, Real Date, and Settings icon button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = greetingText,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FarmTextPrimary
                )
                Text(
                    text = currentDateStr,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = FarmTextSecondary
                )
            }

            Surface(
                onClick = onNavigateToCentroOperativo,
                shape = CircleShape,
                color = FarmSurface,
                shadowElevation = 1.dp,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("centro_operativo_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Centro operativo",
                        tint = FarmTextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Badge: Saved Status
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = FarmGreenLight,
            modifier = Modifier.wrapContentSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = FarmGreenDark,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Dati sincronizzati",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FarmGreenDark
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Promemoria & Notifiche Banner (Requirement 52)
        Surface(
            onClick = onNavigateToPromemoria,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.5.dp, if (todayReminders.isNotEmpty()) Color(0xFFF9A825) else FarmCardBorder),
            color = if (todayReminders.isNotEmpty()) Color(0xFFFFFDE7) else FarmSurface,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_promemoria_card")
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (todayReminders.isNotEmpty()) Color(0xFFFBC02D) else FarmGreenDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (todayReminders.isNotEmpty()) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                            contentDescription = null,
                            tint = if (todayReminders.isNotEmpty()) Color.Black else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = if (todayReminders.isNotEmpty()) "${todayReminders.size} promemoria oggi" else "Nessun promemoria oggi",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = FarmTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (nextReminder != null) "Prossimo: ore ${nextReminder.time} · ${nextReminder.title}" else "Tocca per gestire avvisi e scadenze",
                            fontSize = 12.sp,
                            color = FarmTextSecondary,
                            maxLines = 1
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Apri promemoria",
                    tint = FarmTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4 Main Colored Cards (2x2 Grid)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Card 1: Cosa fare (Green)
            DashboardTile(
                title = "Cosa fare",
                subtitle = if (pendingToday > 0) "$pendingToday ${if (pendingToday == 1) "lavoro oggi" else "lavori oggi"}" else "Nessun lavoro oggi",
                icon = Icons.Default.Check,
                backgroundColor = FarmGreenDark,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToOggi,
                testTag = "tile_cosa_fare"
            )

            // Card 2: Appunti (Terracotta Orange)
            DashboardTile(
                title = "Appunti",
                subtitle = if (notes.isNotEmpty()) "${notes.size} ${if (notes.size == 1) "appunto" else "appunti"}" else "Nessun appunto",
                icon = Icons.Default.EditNote,
                backgroundColor = FarmOrangeCategory,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToAppunti,
                testTag = "tile_appunti"
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Card 3: Terreni (Blue)
            DashboardTile(
                title = "Terreni",
                subtitle = if (fields.isNotEmpty()) "${fields.size} ${if (fields.size == 1) "zona" else "zone"}" else "Nessuna zona",
                icon = Icons.Default.Forest,
                backgroundColor = FarmBlueCategory,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToTerreni,
                testTag = "tile_terreni"
            )

            // Card 4: SPESE (Purple) - Neutral reference, no fake numbers!
            DashboardTile(
                title = "Spese",
                subtitle = if (movements.isNotEmpty()) "${movements.size} ${if (movements.size == 1) "movimento" else "movimenti"}" else "0 movimenti",
                icon = Icons.Default.ReceiptLong,
                backgroundColor = FarmPurpleCategory,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToSpese,
                testTag = "tile_spese"
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Action Buttons: Voice Diary & Memo Rapido (Requirement 47)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Big Yellow Voice Action Button: "Racconta cosa hai fatto"
            Surface(
                onClick = onNavigateToDiario,
                shape = RoundedCornerShape(24.dp),
                color = FarmYellowGold,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .weight(1.3f)
                    .height(60.dp)
                    .testTag("cta_racconta_fatto")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Microfono",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Racconta",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }

            // Quick Action: "Memo Rapido" (Requirement 47)
            Surface(
                onClick = { showQuickMemoDialog = true },
                shape = RoundedCornerShape(24.dp),
                color = FarmSurface,
                border = BorderStroke(1.5.dp, FarmCardBorder),
                modifier = Modifier
                    .weight(1.0f)
                    .height(60.dp)
                    .testTag("cta_memo_rapido")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NoteAdd,
                        contentDescription = "Memo Rapido",
                        tint = FarmTextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Memo",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = FarmTextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Upcoming Real Deadlines (Prossime scadenze)
        Text(
            text = "Prossime scadenze",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = FarmTextPrimary
        )
        Spacer(modifier = Modifier.height(10.dp))

        val upcomingTasks = tasks.filter { !it.isCompleted && it.dueDateStr.isNotBlank() }

        if (upcomingTasks.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, FarmCardBorderSubtle),
                color = FarmSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Nessuna scadenza in programma.",
                    fontSize = 14.sp,
                    color = FarmTextSecondary,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            upcomingTasks.take(3).forEach { task ->
                Surface(
                    onClick = onNavigateToOggi,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, FarmCardBorder),
                    color = FarmSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1EFEA),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                text = "📅 ${task.dueDateStr}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = FarmTextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = FarmTextPrimary
                            )
                            if (task.fieldName.isNotBlank()) {
                                Text(
                                    text = "Zona: ${task.fieldName}",
                                    fontSize = 12.sp,
                                    color = FarmTextSecondary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Modal: Memo Rapido (Requirement 47 & 48)
    if (showQuickMemoDialog) {
        var quickText by remember { mutableStateOf("") }
        var selectedZoneId by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showQuickMemoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EditNote, contentDescription = null, tint = FarmGreenDark, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Memo Rapido", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Scrivi rapidamente una nota o appuntati qualcosa al volo.",
                        fontSize = 13.sp,
                        color = FarmTextSecondary
                    )

                    OutlinedTextField(
                        value = quickText,
                        onValueChange = { quickText = it },
                        placeholder = { Text("es. Controllare erogatori goccia a goccia...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    if (fields.isNotEmpty()) {
                        Text("Zona facoltativa:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = FarmTextSecondary)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedZoneId == null,
                                onClick = { selectedZoneId = null },
                                label = { Text("Generale") }
                            )
                            fields.forEach { field ->
                                FilterChip(
                                    selected = selectedZoneId == field.id,
                                    onClick = { selectedZoneId = field.id },
                                    label = { Text(field.name) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Trasforma in promemoria (Requirement 48)
                    OutlinedButton(
                        onClick = {
                            val zoneName = fields.find { it.id == selectedZoneId }?.name
                            val initial = ReminderItem(
                                title = if (quickText.length > 35) quickText.take(35) + "..." else quickText.ifBlank { "Promemoria rapido" },
                                description = quickText,
                                date = todayFormatted,
                                zoneId = selectedZoneId,
                                zoneName = zoneName
                            )
                            memoToReminderItem = initial
                            showQuickMemoDialog = false
                        },
                        enabled = quickText.isNotBlank()
                    ) {
                        Text("Trasforma in promemoria", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Salva come semplice appunto (Requirement 47)
                    Button(
                        onClick = {
                            if (quickText.isNotBlank()) {
                                val zoneName = fields.find { it.id == selectedZoneId }?.name
                                FarmRepository.addNote(quickText.trim(), selectedZoneId, zoneName)
                                showQuickMemoDialog = false
                            }
                        },
                        enabled = quickText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark)
                    ) {
                        Text("Salva appunto", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickMemoDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }

    // Modal: Conversione da Memo Rapido a Promemoria (Requirement 48)
    memoToReminderItem?.let { item ->
        CreateOrEditReminderDialog(
            initialReminder = item,
            fields = fields,
            onDismiss = { memoToReminderItem = null },
            onSave = { reminder ->
                FarmRepository.addReminder(reminder, context)
                memoToReminderItem = null
            }
        )
    }
}

@Composable
fun DashboardTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = backgroundColor,
        modifier = modifier
            .height(160.dp)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}
