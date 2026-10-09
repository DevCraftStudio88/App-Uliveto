package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FarmRepository
import com.example.data.GeminiService
import com.example.ui.components.FarmBackButton
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CentroOperativoScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val property by FarmRepository.property.collectAsState()
    val fields by FarmRepository.fields.collectAsState()
    val tasks by FarmRepository.tasks.collectAsState()
    val movements by FarmRepository.movements.collectAsState()
    val settings by FarmRepository.settings.collectAsState()
    val reminders by FarmRepository.reminders.collectAsState()

    var userQuestion by remember { mutableStateOf("") }
    var aiAnswer by remember { mutableStateOf<String?>(null) }
    var isThinking by remember { mutableStateOf(false) }

    val pendingTasksCount = tasks.count { !it.isCompleted }

    val todayFormatted = remember {
        val cal = java.util.Calendar.getInstance()
        String.format("%02d/%02d/%04d", cal.get(java.util.Calendar.DAY_OF_MONTH), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.YEAR))
    }
    val todayActiveReminders = reminders.filter { it.status == com.example.data.ReminderStatus.ATTIVO && it.date == todayFormatted }
        .sortedBy { it.time }

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
            text = "Centro operativo",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = FarmTextPrimary
        )

        Text(
            text = "Analisi strategica e raccomandazioni basate sui tuoi dati",
            fontSize = 15.sp,
            color = FarmTextSecondary
        )

        Spacer(modifier = Modifier.height(18.dp))

        // AI Advice Card based ONLY on real farm data
        Surface(
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, FarmGreenDark),
            color = FarmGreenLight,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = FarmGreenDark,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${settings.assistant.assistantName} consiglia",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = FarmGreenDark
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val realAdvice = when {
                    todayActiveReminders.isNotEmpty() -> {
                        val firstRem = todayActiveReminders.first()
                        val zoneInfo = if (!firstRem.zoneName.isNullOrBlank()) " nella zona ${firstRem.zoneName}" else ""
                        "Oggi hai ${todayActiveReminders.size} ${if (todayActiveReminders.size == 1) "promemoria" else "promemoria"}. Alle ${firstRem.time} hai '${firstRem.title}'$zoneInfo."
                    }
                    property == null && fields.isEmpty() ->
                        "Non hai ancora tracciato la tua proprietà o creato zone. Inizia dalla Mappa disegnando i confini del terreno."
                    fields.isNotEmpty() && tasks.isEmpty() ->
                        "Hai registrato ${fields.size} zone ma nessun lavoro in programma. Aggiungi i prossimi interventi da svolgere."
                    pendingTasksCount > 0 ->
                        "Ci sono $pendingTasksCount attività programmate. Concentrati prima sulle scadenze odierne e pianifica gli acquisti necessari."
                    else ->
                        "Nessuna attività pendente registrata per oggi. La proprietà è in ordine."
                }

                Text(
                    text = realAdvice,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = FarmTextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Interactive AI Agronomist Query
        Surface(
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, FarmCardBorder),
            color = FarmSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Chiedi all'Agronomo AI",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = FarmTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = userQuestion,
                    onValueChange = { userQuestion = it },
                    placeholder = { Text("es. Come impostare il piano di concimazione?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_ask_ai"),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        if (userQuestion.isNotBlank()) {
                            isThinking = true
                            coroutineScope.launch {
                                val propInfo = property?.let { "${it.name} (${String.format("%.2f", it.areaHectares)} ha)" } ?: "Nessuna proprietà"
                                val zonesInfo = fields.joinToString { "${it.name}: ${it.crop}" }
                                val contextStr = "Proprietà: $propInfo. Zone: $zonesInfo. Lavori in corso: ${tasks.size}. Tono: ${settings.assistant.tone}."
                                aiAnswer = GeminiService.askAdvisor(userQuestion, contextStr)
                                isThinking = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().testTag("btn_ask_ai")
                ) {
                    if (isThinking) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Invia domanda", color = Color.White, fontWeight = FontWeight.Bold)
                }

                aiAnswer?.let { answer ->
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFFF9E6),
                        border = BorderStroke(1.dp, Color(0xFFFFD54F)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Risposta:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF856404)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = answer,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = FarmTextPrimary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Real KPI summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiCard(title = "Proprietà", value = if (property != null) "${String.format("%.1f", property?.areaHectares)} ha" else "0 ha", subtitle = "superficie", modifier = Modifier.weight(1f))
            KpiCard(title = "Zone", value = "${fields.size}", subtitle = "create", modifier = Modifier.weight(1f))
            KpiCard(title = "Lavori", value = "$pendingTasksCount", subtitle = "da completare", modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.2.dp, FarmCardBorder),
        color = FarmSurface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = FarmTextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = FarmTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = FarmTextSecondary
            )
        }
    }
}

