package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DueCategory
import com.example.data.FarmRepository
import com.example.data.TaskItem
import com.example.ui.components.FarmBackButton
import com.example.ui.components.FarmDateSelector
import com.example.ui.components.FarmPrimaryButton
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun OggiScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tasks by FarmRepository.tasks.collectAsState()
    val fields by FarmRepository.fields.collectAsState()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToDeleteConfirm by remember { mutableStateOf<TaskItem?>(null) }

    val todayStr = remember {
        val cal = Calendar.getInstance()
        String.format("%02d/%02d/%04d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
    }

    val tomorrowStr = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 1)
        String.format("%02d/%02d/%04d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
    }

    val todayTasks = tasks.filter { it.dueDateStr == "Oggi" || it.dueDateStr == todayStr || it.dueCategory == DueCategory.OGGI }
    val tomorrowTasks = tasks.filter { it.dueDateStr == "Domani" || it.dueDateStr == tomorrowStr || it.dueCategory == DueCategory.DOMANI }
    val otherTasks = tasks.filterNot { todayTasks.contains(it) || tomorrowTasks.contains(it) }

    val completedToday = todayTasks.count { it.isCompleted }
    val totalToday = todayTasks.size

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FarmBgCream)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FarmBackButton(onBack = onBack)

            Button(
                onClick = { showAddTaskDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(42.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nuovo lavoro", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Da fare oggi",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = FarmTextPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (totalToday > 0) "$completedToday ${if (completedToday == 1) "lavoro fatto" else "lavori fatti"} su $totalToday" else "Nessun lavoro programmato per oggi",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = FarmTextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Checklist,
                        contentDescription = null,
                        tint = FarmTextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Non hai ancora aggiunto lavori.",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FarmTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tocca 'Nuovo lavoro' per programmare attività agricole e scadenze.",
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
                if (todayTasks.isNotEmpty()) {
                    item {
                        Text("OGGI", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = FarmGreenDark, letterSpacing = 1.sp)
                    }
                    items(todayTasks, key = { it.id }) { task ->
                        TaskItemRow(
                            task = task,
                            onToggle = { FarmRepository.toggleTask(task.id) },
                            onDelete = { taskToDeleteConfirm = task }
                        )
                    }
                }

                if (tomorrowTasks.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("DOMANI", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = FarmTextSecondary, letterSpacing = 1.sp)
                    }
                    items(tomorrowTasks, key = { it.id }) { task ->
                        TaskItemRow(
                            task = task,
                            onToggle = { FarmRepository.toggleTask(task.id) },
                            onDelete = { taskToDeleteConfirm = task }
                        )
                    }
                }

                if (otherTasks.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("ALTRE DATE", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = FarmTextSecondary, letterSpacing = 1.sp)
                    }
                    items(otherTasks, key = { it.id }) { task ->
                        TaskItemRow(
                            task = task,
                            onToggle = { FarmRepository.toggleTask(task.id) },
                            onDelete = { taskToDeleteConfirm = task }
                        )
                    }
                }
            }
        }
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            fields = fields,
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, fieldName, fieldId, category, dueStr ->
                FarmRepository.addTask(title, fieldName, fieldId, category, dueStr)
                showAddTaskDialog = false
            }
        )
    }

    taskToDeleteConfirm?.let { t ->
        AlertDialog(
            onDismissRequest = { taskToDeleteConfirm = null },
            title = { Text("Eliminare lavoro?", fontWeight = FontWeight.Bold) },
            text = { Text("Vuoi davvero rimuovere '${t.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        FarmRepository.deleteTask(t.id)
                        taskToDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmRedAlert)
                ) { Text("Elimina", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { taskToDeleteConfirm = null }) { Text("Annulla") }
            }
        )
    }
}

@Composable
fun TaskItemRow(
    task: TaskItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.2.dp, FarmCardBorder),
        color = FarmSurface,
        modifier = Modifier.fillMaxWidth().testTag("task_item_${task.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                border = if (!task.isCompleted) BorderStroke(2.dp, FarmCardBorder) else null,
                color = if (task.isCompleted) FarmGreenDark else Color.Transparent,
                modifier = Modifier.size(32.dp).clickable { onToggle() }
            ) {
                if (task.isCompleted) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (task.isCompleted) FarmTextMuted else FarmTextPrimary,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                if (task.fieldName.isNotBlank() || task.dueDateStr.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = listOf(task.dueDateStr, task.fieldName).filter { it.isNotBlank() }.joinToString(" · "),
                        fontSize = 13.sp,
                        color = FarmTextSecondary
                    )
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Elimina", tint = FarmTextSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun AddTaskDialog(
    fields: List<com.example.data.Field>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, fieldName: String, fieldId: String?, category: DueCategory, dueStr: String) -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current
    var title by remember { mutableStateOf("") }
    var selectedFieldId by remember { mutableStateOf<String?>(null) }
    val selectedFieldName = fields.find { it.id == selectedFieldId }?.name ?: ""

    val defaultToday = remember {
        val cal = Calendar.getInstance()
        String.format("%02d/%02d/%04d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
    }
    var chosenDate by remember { mutableStateOf(defaultToday) }

    AlertDialog(
        onDismissRequest = {
            keyboard?.hide()
            onDismiss()
        },
        title = { Text("Nuovo lavoro", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Attività da svolgere *") },
                    placeholder = { Text("es. Trattamento rameico, Potatura chioma") },
                    modifier = Modifier.fillMaxWidth().testTag("input_task_title")
                )

                if (fields.isNotEmpty()) {
                    Text("Terreno associato:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = selectedFieldId == null,
                            onClick = { selectedFieldId = null },
                            label = { Text("Nessuno") }
                        )
                        fields.take(3).forEach { f ->
                            FilterChip(
                                selected = selectedFieldId == f.id,
                                onClick = { selectedFieldId = f.id },
                                label = { Text(f.name) }
                            )
                        }
                    }
                }

                // Date selector with real Calendar DatePickerDialog
                FarmDateSelector(
                    selectedDateStr = chosenDate,
                    onDateSelected = { chosenDate = it },
                    label = "Data scadenza o esecuzione *"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    keyboard?.hide()
                    if (title.isNotBlank()) {
                        val cal = Calendar.getInstance()
                        val todayStr = String.format("%02d/%02d/%04d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
                        cal.add(Calendar.DAY_OF_MONTH, 1)
                        val tomorrowStr = String.format("%02d/%02d/%04d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))

                        val cat = when (chosenDate) {
                            todayStr -> DueCategory.OGGI
                            tomorrowStr -> DueCategory.DOMANI
                            else -> DueCategory.SELEZIONA_DATA
                        }
                        onConfirm(title.trim(), selectedFieldName, selectedFieldId, cat, chosenDate)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark),
                enabled = title.isNotBlank()
            ) {
                Text("Salva", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                keyboard?.hide()
                onDismiss()
            }) {
                Text("Annulla")
            }
        }
    )
}
