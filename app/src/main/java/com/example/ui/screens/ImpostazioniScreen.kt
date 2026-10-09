package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.notifications.ReminderNotificationManager
import com.example.ui.components.FarmBackButton
import com.example.ui.theme.*

@Composable
fun ImpostazioniScreen(
    onBack: () -> Unit,
    onNavigateToTerreni: () -> Unit,
    onNavigateToPromemoria: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val settings by FarmRepository.settings.collectAsState()
    val fields by FarmRepository.fields.collectAsState()
    val property by FarmRepository.property.collectAsState()
    val reminders by FarmRepository.reminders.collectAsState()

    var showAssistantDialog by remember { mutableStateOf(false) }
    var showNotifSettingsDialog by remember { mutableStateOf(false) }
    var showExportConfirmation by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FarmBgCream)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        FarmBackButton(onBack = onBack)

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Impostazioni",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = FarmTextPrimary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Section: "Il mio assistente" (Requirement 12)
        Surface(
            onClick = { showAssistantDialog = true },
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.5.dp, FarmCardBorder),
            color = FarmSurface,
            modifier = Modifier.fillMaxWidth().testTag("setting_mio_assistente")
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(FarmGreenDark, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Il mio assistente",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = FarmTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${settings.assistant.assistantName} · ${if (settings.assistant.userName.isNotBlank()) "Utente: " + settings.assistant.userName else "Nome non impostato"} (${settings.assistant.tone})",
                            fontSize = 13.sp,
                            color = FarmTextSecondary
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = FarmTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Section: "Grandezza del testo"
        Text(
            text = "Grandezza del testo",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = FarmTextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TextSizeChip(
                label = "Normale",
                isSelected = settings.textSize == TextSizeOption.NORMALE,
                onClick = { FarmRepository.setTextSize(TextSizeOption.NORMALE) },
                modifier = Modifier.weight(1f)
            )
            TextSizeChip(
                label = "Grande",
                isSelected = settings.textSize == TextSizeOption.GRANDE,
                onClick = { FarmRepository.setTextSize(TextSizeOption.GRANDE) },
                modifier = Modifier.weight(1f)
            )
            TextSizeChip(
                label = "Enorme",
                isSelected = settings.textSize == TextSizeOption.ENORME,
                onClick = { FarmRepository.setTextSize(TextSizeOption.ENORME) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Zone e colture
        SettingsOptionCard(
            title = "Zone e colture",
            subtitle = if (fields.isNotEmpty()) "${fields.size} zone create" else "Nessuna zona creata",
            icon = Icons.Default.Terrain,
            iconBg = FarmBlueCategory,
            onClick = onNavigateToTerreni,
            testTag = "setting_zone_colture"
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Promemoria & Notifiche (Requirement 56 & 57)
        SettingsOptionCard(
            title = "Notifiche e promemoria",
            subtitle = if (settings.remindersEnabled) "Attive · ${settings.notificationSettings.privacyMode.lowercase()} · ${reminders.count { it.status == ReminderStatus.ATTIVO }} attivi" else "Disattivate",
            icon = Icons.Default.Notifications,
            iconBg = Color(0xFFF9A825),
            onClick = { showNotifSettingsDialog = true },
            testTag = "setting_promemoria"
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Esporta
        SettingsOptionCard(
            title = "Esporta resoconto",
            subtitle = "Esporta dati per il commercialista o archivio",
            icon = Icons.Default.FileDownload,
            iconBg = FarmOrangeCategory,
            onClick = {
                val propText = property?.let { "${it.name} (${String.format("%.2f", it.areaHectares)} ha)" } ?: "Nessuna proprietà salvata"
                val activeRems = reminders.count { it.status == ReminderStatus.ATTIVO }
                val completedRems = reminders.count { it.status == ReminderStatus.COMPLETATO }
                val report = """
                    RESOCONTO AZIENDA AGRICOLA
                    Proprietà: $propText
                    Zone registrate: ${fields.size}
                    Movimenti economici totali: ${FarmRepository.movements.value.size}
                    
                    PROMEMORIA E SCADENZE:
                    Totale promemoria: ${reminders.size}
                    Promemoria attivi: $activeRems
                    Promemoria completati: $completedRems
                    Notifiche attive: ${if (settings.remindersEnabled) "Sì" else "No"}
                    Modalità privacy: ${settings.notificationSettings.privacyMode}
                """.trimIndent()
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, report)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Esporta resoconto aziendale"))
                showExportConfirmation = true
            },
            testTag = "setting_esporta"
        )

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Dialog: "Il mio assistente" configuration
    if (showAssistantDialog) {
        var userName by remember { mutableStateOf(settings.assistant.userName) }
        var assistantName by remember { mutableStateOf(settings.assistant.assistantName) }
        var greetingPhrase by remember { mutableStateOf(settings.assistant.greetingPhrase) }
        var tone by remember { mutableStateOf(settings.assistant.tone) }

        AlertDialog(
            onDismissRequest = {
                keyboard?.hide()
                showAssistantDialog = false
            },
            title = { Text("Configura Il Mio Assistente", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = userName,
                        onValueChange = { userName = it },
                        label = { Text("Nome utente") },
                        placeholder = { Text("es. Davide") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = assistantName,
                        onValueChange = { assistantName = it },
                        label = { Text("Nome assistente") },
                        placeholder = { Text("es. Assistente Uliveto") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = greetingPhrase,
                        onValueChange = { greetingPhrase = it },
                        label = { Text("Formula di saluto") },
                        placeholder = { Text("es. Ciao, Buongiorno, Benvenuto") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Tono preferito:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Amichevole", "Sintetico", "Professionale").forEach { t ->
                            FilterChip(
                                selected = tone == t,
                                onClick = { tone = t },
                                label = { Text(t, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        keyboard?.hide()
                        FarmRepository.updateAssistant(
                            userName = userName.trim(),
                            assistantName = assistantName.trim().ifBlank { "Il mio assistente" },
                            greetingPhrase = greetingPhrase.trim().ifBlank { "Ciao" },
                            tone = tone
                        )
                        showAssistantDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark)
                ) { Text("Salva impostazioni", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = {
                    keyboard?.hide()
                    showAssistantDialog = false
                }) { Text("Annulla") }
            }
        )
    }

    // Dialog: "Notifiche e promemoria" (Requirement 56 & 57)
    if (showNotifSettingsDialog) {
        var notifEnabled by remember { mutableStateOf(settings.remindersEnabled) }
        var soundEnabled by remember { mutableStateOf(settings.notificationSettings.soundEnabled) }
        var vibrationEnabled by remember { mutableStateOf(settings.notificationSettings.vibrationEnabled) }
        var privacyMode by remember { mutableStateOf(settings.notificationSettings.privacyMode) }
        var recurringEnabled by remember { mutableStateOf(settings.notificationSettings.recurringRemindersEnabled) }

        AlertDialog(
            onDismissRequest = { showNotifSettingsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = FarmGreenDark, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Notifiche e Promemoria", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Notifiche attive
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Notifiche attive", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Ricevi avvisi per scadenze e promemoria", fontSize = 12.sp, color = FarmTextSecondary)
                        }
                        Switch(
                            checked = notifEnabled,
                            onCheckedChange = { notifEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = FarmGreenDark)
                        )
                    }

                    HorizontalDivider()

                    // Suono
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Suono notifica", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = { soundEnabled = it },
                            enabled = notifEnabled,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = FarmGreenDark)
                        )
                    }

                    // Vibrazione
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Vibrazione", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = vibrationEnabled,
                            onCheckedChange = { vibrationEnabled = it },
                            enabled = notifEnabled,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = FarmGreenDark)
                        )
                    }

                    // Promemoria ricorrenti
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Promemoria ricorrenti", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = recurringEnabled,
                            onCheckedChange = { recurringEnabled = it },
                            enabled = notifEnabled,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = FarmGreenDark)
                        )
                    }

                    HorizontalDivider()

                    // Mostra dettagli / Privacy (Requirement 56)
                    Text("Privacy su blocco schermo:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            "DETTAGLI" to "Mostra dettagli completi",
                            "SOLO_TITOLO" to "Mostra solo titolo",
                            "NASCONDI" to "Nascondi contenuto"
                        ).forEach { (mode, desc) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = privacyMode == mode,
                                    onClick = { privacyMode = mode },
                                    enabled = notifEnabled
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(desc, fontSize = 13.sp)
                            }
                        }
                    }

                    HorizontalDivider()

                    // Anteprima notifica di prova
                    OutlinedButton(
                        onClick = {
                            ReminderNotificationManager.sendTestNotification(context)
                        },
                        enabled = notifEnabled,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Invia notifica di prova")
                    }

                    // Pulsante Gestisci tutti i promemoria
                    Button(
                        onClick = {
                            showNotifSettingsDialog = false
                            onNavigateToPromemoria()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apri centro promemoria")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newSettings = NotificationSettings(
                            soundEnabled = soundEnabled,
                            vibrationEnabled = vibrationEnabled,
                            privacyMode = privacyMode,
                            recurringRemindersEnabled = recurringEnabled
                        )
                        FarmRepository.updateNotificationSettings(newSettings)
                        if (settings.remindersEnabled != notifEnabled) {
                            FarmRepository.toggleReminders(context)
                        }
                        showNotifSettingsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark)
                ) {
                    Text("Salva", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotifSettingsDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }
}

@Composable
fun TextSizeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, if (isSelected) FarmGreenDark else FarmCardBorder),
        color = if (isSelected) FarmGreenLight else FarmSurface,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) FarmGreenDark else FarmTextPrimary
            )
        }
    }
}

@Composable
fun SettingsOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, FarmCardBorder),
        color = FarmSurface,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(iconBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = FarmTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = FarmTextSecondary
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = FarmTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

