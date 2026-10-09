package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DiaryEntry
import com.example.data.ExtractedDiary
import com.example.data.FarmRepository
import com.example.data.GeminiService
import com.example.ui.components.FarmBackButton
import com.example.ui.components.FarmPrimaryButton
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DiarioScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val diaryEntries by FarmRepository.diaryEntries.collectAsState()
    val settings by FarmRepository.settings.collectAsState()

    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var spokenText by remember { mutableStateOf("") }
    var extractedData by remember { mutableStateOf(ExtractedDiary()) }

    var isAiAnalyzing by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var saveSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Waveform animation
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FarmBgCream)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Back Button
        FarmBackButton(onBack = onBack)

        Spacer(modifier = Modifier.height(16.dp))

        // Title and description
        Text(
            text = "Diario",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = FarmTextPrimary
        )
        Text(
            text = "Parla o scrivi: il diario si compila da solo",
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = FarmTextSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Offline / Sync status banner (as seen in PDF Page 3)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFFF9E6),
            border = BorderStroke(1.5.dp, FarmCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = FarmTextPrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Niente rete: salvo sul telefono.",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = FarmTextPrimary
                    )
                    Text(
                        text = "Parte da solo quando torna il segnale.",
                        fontSize = 13.sp,
                        color = FarmTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Big Yellow Microphone Button (Page 3)
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                onClick = {
                    if (!isRecording) {
                        isRecording = true
                        recordingSeconds = 1
                        coroutineScope.launch {
                            for (i in 2..6) {
                                delay(600)
                                recordingSeconds = i
                            }
                            isRecording = false
                            isAiAnalyzing = true
                            // Run Gemini extraction on spoken text
                            extractedData = GeminiService.extractDiaryIntent(spokenText)
                            isAiAnalyzing = false
                        }
                    } else {
                        isRecording = false
                    }
                },
                shape = CircleShape,
                color = FarmYellowGold,
                border = BorderStroke(2.dp, FarmCardBorder),
                modifier = Modifier
                    .size(92.dp)
                    .scale(if (isRecording) pulseScale else 1f)
                    .testTag("btn_record_voice")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Registra vocale",
                        tint = Color.Black,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Audio waveform bars simulation & seconds label
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val barHeights = listOf(14, 24, 18, 30, 22, 14, 26, 18, 28, 16, 22, 14)
                barHeights.forEach { h ->
                    val barHeight = if (isRecording) (h * 1.2f).toInt() else h
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(barHeight.dp)
                            .background(if (isRecording) FarmGreenDark else FarmCardBorder, CircleShape)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isRecording) "Sto ascoltando... ($recordingSeconds s)" else "Registrazione di $recordingSeconds secondi",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = FarmTextSecondary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Preset example phrases for easy testing
        Text(
            text = "Oppure scegli una frase d'esempio:",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = FarmTextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "Oggi ho concimato il campo dei limoni con cinque sacchi",
                "Ho potato l'uliveto alto",
                "Seminato finocchi all'orto"
            ).forEach { phrase ->
                Surface(
                    onClick = {
                        spokenText = phrase
                        coroutineScope.launch {
                            isAiAnalyzing = true
                            extractedData = GeminiService.extractDiaryIntent(phrase)
                            isAiAnalyzing = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = FarmSurface,
                    border = BorderStroke(1.dp, FarmCardBorderSubtle),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = phrase.take(22) + "...",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = FarmTextPrimary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (spokenText.isNotBlank()) {
            // Card: "HAI DETTO"
            Surface(
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, FarmCardBorder),
                color = FarmSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "HAI DETTO",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = FarmTextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "«$spokenText»",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontStyle = FontStyle.Italic,
                        color = FarmTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Card: "HO CAPITO QUESTO"
            Surface(
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, FarmCardBorder),
                color = FarmSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HO CAPITO QUESTO",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = FarmTextSecondary,
                            letterSpacing = 1.sp
                        )
                        if (isAiAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = FarmGreenDark
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    ExtractedRow(label = "Zona", value = extractedData.zone.ifBlank { "—" })
                    ExtractedRow(label = "Lavoro", value = extractedData.task.ifBlank { "—" })
                    ExtractedRow(label = "Prodotto", value = extractedData.product.ifBlank { "—" })
                    ExtractedRow(label = "Quantità", value = extractedData.quantity.ifBlank { "—" })
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons: "✓ Va bene, salva" & "Correggi"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val todayStr = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.ITALIAN).format(java.util.Date())
                        FarmRepository.addDiaryEntry(
                            raw = spokenText,
                            zone = extractedData.zone,
                            task = extractedData.task,
                            product = extractedData.product,
                            qty = extractedData.quantity,
                            date = todayStr
                        )
                        saveSuccessMessage = "Voce salvata nel diario con successo!"
                        spokenText = ""
                        extractedData = ExtractedDiary()
                        coroutineScope.launch {
                            delay(2500)
                            saveSuccessMessage = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(54.dp)
                        .testTag("btn_save_diary")
                ) {
                    Text(
                        text = "✓ Va bene, salva",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                OutlinedButton(
                    onClick = { showEditDialog = true },
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.5.dp, FarmCardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("btn_correct_diary")
                ) {
                    Text(
                        text = "Correggi",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = FarmTextPrimary
                    )
                }
            }
        }


        AnimatedVisibility(visible = saveSuccessMessage != null) {
            saveSuccessMessage?.let { msg ->
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = FarmGreenLight,
                    border = BorderStroke(1.dp, FarmGreenDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = FarmGreenDark,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // History of Diary entries
        Text(
            text = "Ultime registrazioni",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = FarmTextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        diaryEntries.forEach { entry ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, FarmCardBorderSubtle),
                color = FarmSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = entry.zone,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = FarmTextPrimary
                        )
                        Text(
                            text = entry.dateStr,
                            fontSize = 13.sp,
                            color = FarmTextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${entry.task} · ${entry.quantity}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = FarmGreenDark
                    )
                    if (entry.rawText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "«${entry.rawText}»",
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            color = FarmTextSecondary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showEditDialog) {
        EditExtractedDialog(
            current = extractedData,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                extractedData = updated
                showEditDialog = false
            }
        )
    }
}

@Composable
fun ExtractedRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = FarmTextSecondary
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = FarmTextPrimary
        )
    }
}

@Composable
fun EditExtractedDialog(
    current: ExtractedDiary,
    onDismiss: () -> Unit,
    onSave: (ExtractedDiary) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var zone by remember { mutableStateOf(current.zone) }
    var task by remember { mutableStateOf(current.task) }
    var product by remember { mutableStateOf(current.product) }
    var quantity by remember { mutableStateOf(current.quantity) }

    AlertDialog(
        onDismissRequest = {
            keyboardController?.hide()
            onDismiss()
        },
        title = { Text("Modifica dati estratti", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = zone,
                    onValueChange = { zone = it },
                    label = { Text("Zona / Terreno") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = task,
                    onValueChange = { task = it },
                    label = { Text("Lavoro") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = product,
                    onValueChange = { product = it },
                    label = { Text("Prodotto") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantità") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    keyboardController?.hide()
                    onSave(current.copy(zone = zone, task = task, product = product, quantity = quantity))
                },
                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark)
            ) {
                Text("Applica correzioni", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                keyboardController?.hide()
                onDismiss()
            }) {
                Text("Annulla")
            }
        }
    )
}
