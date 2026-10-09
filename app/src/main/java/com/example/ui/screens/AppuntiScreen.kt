package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.components.FarmBackButton
import com.example.ui.components.FarmPrimaryButton
import com.example.ui.theme.*

@Composable
fun AppuntiScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val notes by FarmRepository.notes.collectAsState()
    val fields by FarmRepository.fields.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<NoteItem?>(null) }
    var noteToReminder by remember { mutableStateOf<NoteItem?>(null) }
    val context = LocalContext.current

    // Double confirmation states for delete
    var deleteConfirm1 by remember { mutableStateOf<NoteItem?>(null) }
    var deleteConfirm2 by remember { mutableStateOf<NoteItem?>(null) }

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
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(42.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nuovo appunto", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Appunti",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = FarmTextPrimary
        )

        Text(
            text = "${notes.size} appunti registrati",
            fontSize = 15.sp,
            color = FarmTextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        tint = FarmTextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Non hai ancora creato appunti.",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FarmTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tocca 'Nuovo appunto' per annotare osservazioni agronomiche o promemoria.",
                        fontSize = 13.sp,
                        color = FarmTextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(notes, key = { _, n -> n.id }) { index, note ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.5.dp, FarmCardBorder),
                        color = FarmSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    if (!note.fieldName.isNullOrBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = FarmGreenLight,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        ) {
                                            Text(
                                                text = "Zona: ${note.fieldName}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = FarmGreenDark,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = note.text,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = FarmTextPrimary,
                                        lineHeight = 22.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Data: ${note.createdAt}",
                                        fontSize = 12.sp,
                                        color = FarmTextSecondary
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { noteToReminder = note },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.AddAlert, contentDescription = "Trasforma in promemoria", tint = FarmGreenDark, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { noteToEdit = note },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Modifica", tint = FarmTextPrimary, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { deleteConfirm1 = note },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Elimina", tint = FarmRedAlert, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = FarmCardBorderSubtle)

                            // Move Up / Move Down order buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Ordine: ", fontSize = 11.sp, color = FarmTextSecondary)
                                IconButton(
                                    onClick = { FarmRepository.moveNoteUp(note.id) },
                                    enabled = index > 0,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Sposta su", modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { FarmRepository.moveNoteDown(note.id) },
                                    enabled = index < notes.size - 1,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Sposta giù", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Create Note
    if (showCreateDialog) {
        val keyboard = LocalSoftwareKeyboardController.current
        var noteText by remember { mutableStateOf("") }
        var selectedFieldId by remember { mutableStateOf<String?>(null) }
        val selectedFieldName = fields.find { it.id == selectedFieldId }?.name

        AlertDialog(
            onDismissRequest = {
                keyboard?.hide()
                showCreateDialog = false
            },
            title = { Text("Nuovo Appunto", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Testo dell'appunto") },
                        placeholder = { Text("Scrivi note di campo, osservazioni...") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        maxLines = 5
                    )

                    if (fields.isNotEmpty()) {
                        Text("Associa a una zona (opzionale):", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = selectedFieldId == null,
                                onClick = { selectedFieldId = null },
                                label = { Text("Nessuna") }
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        keyboard?.hide()
                        if (noteText.isNotBlank()) {
                            FarmRepository.addNote(noteText.trim(), selectedFieldId, selectedFieldName)
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark),
                    enabled = noteText.isNotBlank()
                ) { Text("Salva appunto", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = {
                    keyboard?.hide()
                    showCreateDialog = false
                }) { Text("Annulla") }
            }
        )
    }

    // Dialog: Edit Note
    noteToEdit?.let { currentNote ->
        val keyboard = LocalSoftwareKeyboardController.current
        var editText by remember { mutableStateOf(currentNote.text) }
        var editFieldId by remember { mutableStateOf(currentNote.fieldId) }
        val editFieldName = fields.find { it.id == editFieldId }?.name

        AlertDialog(
            onDismissRequest = {
                keyboard?.hide()
                noteToEdit = null
            },
            title = { Text("Modifica Appunto", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        label = { Text("Testo appunto") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        maxLines = 5
                    )

                    if (fields.isNotEmpty()) {
                        Text("Zona associata:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = editFieldId == null,
                                onClick = { editFieldId = null },
                                label = { Text("Nessuna") }
                            )
                            fields.take(3).forEach { f ->
                                FilterChip(
                                    selected = editFieldId == f.id,
                                    onClick = { editFieldId = f.id },
                                    label = { Text(f.name) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        keyboard?.hide()
                        if (editText.isNotBlank()) {
                            FarmRepository.updateNote(currentNote.id, editText.trim(), editFieldId, editFieldName)
                            noteToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark)
                ) { Text("Aggiorna", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = {
                    keyboard?.hide()
                    noteToEdit = null
                }) { Text("Annulla") }
            }
        )
    }

    // Double Confirmation: Delete Note
    deleteConfirm1?.let { n ->
        AlertDialog(
            onDismissRequest = { deleteConfirm1 = null },
            title = { Text("Eliminare questo appunto?", fontWeight = FontWeight.Bold) },
            text = { Text("Vuoi davvero rimuovere questo appunto?") },
            confirmButton = {
                Button(
                    onClick = {
                        deleteConfirm2 = n
                        deleteConfirm1 = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmRedAlert)
                ) { Text("Procedi", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirm1 = null }) { Text("Annulla") }
            }
        )
    }

    deleteConfirm2?.let { n ->
        AlertDialog(
            onDismissRequest = { deleteConfirm2 = null },
            title = { Text("Conferma definitiva", fontWeight = FontWeight.Bold, color = FarmRedAlert) },
            text = { Text("Questa operazione eliminerà definitivamente l'appunto. Confermi?") },
            confirmButton = {
                Button(
                    onClick = {
                        FarmRepository.deleteNote(n.id)
                        deleteConfirm2 = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmRedAlert)
                ) { Text("Sì, elimina definitivamente", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirm2 = null }) { Text("Annulla") }
            }
        )
    }

    // Modal: Trasforma in promemoria (Requirement 48)
    noteToReminder?.let { n ->
        val todayStr = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.ITALIAN).format(java.util.Date())
        val initialReminder = ReminderItem(
            title = if (n.text.length > 35) n.text.take(35) + "..." else n.text,
            description = n.text,
            date = todayStr,
            zoneId = n.fieldId,
            zoneName = n.fieldName,
            category = "Generale",
            linkedEntityType = LinkedEntityType.APPUNTO,
            linkedEntityId = n.id,
            linkedEntityTitle = if (n.text.length > 25) n.text.take(25) + "..." else n.text
        )

        CreateOrEditReminderDialog(
            initialReminder = initialReminder,
            fields = fields,
            onDismiss = { noteToReminder = null },
            onSave = { rem ->
                FarmRepository.addReminder(rem, context)
                noteToReminder = null
            }
        )
    }
}
