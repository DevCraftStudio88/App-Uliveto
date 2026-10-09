package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FarmRepository
import com.example.data.Field
import com.example.ui.components.FarmBackButton
import com.example.ui.components.FarmPrimaryButton
import com.example.ui.theme.*

@Composable
fun SchedaTerrenoScreen(
    field: Field,
    onBack: () -> Unit,
    onNavigateToDiario: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tasks by FarmRepository.tasks.collectAsState()
    val notes by FarmRepository.notes.collectAsState()

    var showAddTaskDialog by remember { mutableStateOf(false) }

    val fieldTasks = tasks.filter { it.fieldId == field.id || it.fieldName == field.name }
    val fieldNotes = notes.filter { it.fieldId == field.id || it.fieldName == field.name }

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
            text = field.name,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = FarmTextPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${if (field.crop.isNotBlank()) field.crop + " · " else ""}${String.format("%.2f", field.areaHectares)} ettari (${String.format("%.0f", field.areaSquareMeters)} m²)",
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = FarmTextSecondary
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Surface metrics box
        Surface(
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.2.dp, FarmCardBorder),
            color = FarmSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Superficie", fontSize = 12.sp, color = FarmTextSecondary)
                    Text("${String.format("%.2f", field.areaHectares)} ha", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = FarmGreenDark)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Metri quadri", fontSize = 12.sp, color = FarmTextSecondary)
                    Text("${String.format("%.0f", field.areaSquareMeters)} m²", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = FarmTextPrimary)
                }
            }
        }

        if (field.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, FarmCardBorderSubtle),
                color = FarmSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Note di campo:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FarmTextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(field.notes, fontSize = 14.sp, color = FarmTextPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Lavori associati
        Text(
            text = "Lavori associati a questa zona",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = FarmTextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (fieldTasks.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, FarmCardBorderSubtle),
                color = FarmSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Nessun lavoro registrato per questa zona.",
                    fontSize = 13.sp,
                    color = FarmTextSecondary,
                    modifier = Modifier.padding(14.dp)
                )
            }
        } else {
            fieldTasks.forEach { task ->
                TaskItemRow(
                    task = task,
                    onToggle = { FarmRepository.toggleTask(task.id) },
                    onDelete = { FarmRepository.deleteTask(task.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Appunti di questa zona
        Text(
            text = "Appunti di questa zona",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = FarmTextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (fieldNotes.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, FarmCardBorderSubtle),
                color = FarmSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Nessun appunto associato.",
                    fontSize = 13.sp,
                    color = FarmTextSecondary,
                    modifier = Modifier.padding(14.dp)
                )
            }
        } else {
            fieldNotes.forEach { note ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, FarmCardBorder),
                    color = FarmSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(note.text, fontSize = 14.sp, color = FarmTextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(note.createdAt, fontSize = 11.sp, color = FarmTextSecondary)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action button: Aggiungi lavoro
        FarmPrimaryButton(
            text = "+ Aggiungi un lavoro qui",
            onClick = { showAddTaskDialog = true },
            testTag = "btn_add_task_field"
        )

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            fields = listOf(field),
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, fieldName, fieldId, category, dueStr ->
                FarmRepository.addTask(title, fieldName, fieldId ?: field.id, category, dueStr)
                showAddTaskDialog = false
            }
        )
    }
}
