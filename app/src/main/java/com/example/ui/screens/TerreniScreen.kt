package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.example.ui.theme.*

@Composable
fun TerreniScreen(
    onBack: () -> Unit,
    onSelectField: (Field) -> Unit,
    onNavigateToMappa: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fields by FarmRepository.fields.collectAsState()
    val property by FarmRepository.property.collectAsState()

    val totalHectares = fields.sumOf { it.areaHectares }
    val totalM2 = fields.sumOf { it.areaSquareMeters }

    var fieldToEdit by remember { mutableStateOf<Field?>(null) }
    var deleteZoneConfirm1 by remember { mutableStateOf<Field?>(null) }
    var deleteZoneConfirm2 by remember { mutableStateOf<Field?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FarmBgCream)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Back Button & Map button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FarmBackButton(onBack = onBack)

            Button(
                onClick = onNavigateToMappa,
                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(42.dp)
            ) {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Apri Mappa", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Terreni & Zone",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = FarmTextPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (fields.isNotEmpty()) "${String.format("%.2f", totalHectares)} ettari totali (${String.format("%.0f", totalM2)} m²) · ${fields.size} ${if (fields.size == 1) "zona" else "zone"}" else "Nessuna zona creata",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = FarmTextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (fields.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Forest,
                        contentDescription = null,
                        tint = FarmTextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Non hai ancora creato nessuna zona.",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FarmTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Apri la mappa e tocca 'Disegna nuova zona' per tracciare il poligono.",
                        fontSize = 13.sp,
                        color = FarmTextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onNavigateToMappa,
                        colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Disegna zona su Mappa", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(fields, key = { _, f -> f.id }) { index, field ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.5.dp, FarmCardBorder),
                        color = FarmSurface,
                        modifier = Modifier.fillMaxWidth().testTag("field_item_${field.id}")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f).clickable { onSelectField(field) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(Color(field.colorHex).copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Eco,
                                            contentDescription = null,
                                            tint = Color(field.colorHex),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column {
                                        Text(
                                            text = field.name,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = FarmTextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${if (field.crop.isNotBlank()) field.crop + " · " else ""}${String.format("%.2f", field.areaHectares)} ha (${String.format("%.0f", field.areaSquareMeters)} m²)",
                                            fontSize = 13.sp,
                                            color = FarmTextSecondary
                                        )
                                    }
                                }

                                Row {
                                    IconButton(onClick = { fieldToEdit = field }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Modifica", tint = FarmTextPrimary, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { deleteZoneConfirm1 = field }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Elimina", tint = FarmRedAlert, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = FarmCardBorderSubtle)

                            // Order management: move up / move down with persistent index
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { onSelectField(field) }) {
                                    Text("Apri scheda →", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FarmGreenDark)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Ordine: ", fontSize = 11.sp, color = FarmTextSecondary)
                                    IconButton(
                                        onClick = { FarmRepository.moveFieldUp(field.id) },
                                        enabled = index > 0,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Sposta su", modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { FarmRepository.moveFieldDown(field.id) },
                                        enabled = index < fields.size - 1,
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
    }

    // Modal Dialog: Edit Zone
    fieldToEdit?.let { f ->
        var editName by remember { mutableStateOf(f.name) }
        var editCrop by remember { mutableStateOf(f.crop) }
        var editNotes by remember { mutableStateOf(f.notes) }

        AlertDialog(
            onDismissRequest = { fieldToEdit = null },
            title = { Text("Modifica Zona", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Nome zona *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCrop,
                        onValueChange = { editCrop = it },
                        label = { Text("Coltura") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("Note / Appunti") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            FarmRepository.updateField(f.id, editName.trim(), editCrop.trim(), editNotes.trim())
                            fieldToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark),
                    enabled = editName.isNotBlank()
                ) { Text("Salva modifiche", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { fieldToEdit = null }) { Text("Annulla") }
            }
        )
    }

    // Double Confirmation: Delete Zone
    deleteZoneConfirm1?.let { z ->
        AlertDialog(
            onDismissRequest = { deleteZoneConfirm1 = null },
            title = { Text("Eliminare la zona '${z.name}'?", fontWeight = FontWeight.Bold) },
            text = { Text("Vuoi davvero eliminare questa zona?") },
            confirmButton = {
                Button(
                    onClick = {
                        deleteZoneConfirm2 = z
                        deleteZoneConfirm1 = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmRedAlert)
                ) { Text("Procedi", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { deleteZoneConfirm1 = null }) { Text("Annulla") }
            }
        )
    }

    deleteZoneConfirm2?.let { z ->
        AlertDialog(
            onDismissRequest = { deleteZoneConfirm2 = null },
            title = { Text("Conferma definitiva eliminazione", fontWeight = FontWeight.Bold, color = FarmRedAlert) },
            text = { Text("Questa operazione eliminerà definitivamente la zona '${z.name}', i suoi confini e i dati associati. Confermi?") },
            confirmButton = {
                Button(
                    onClick = {
                        FarmRepository.deleteField(z.id)
                        deleteZoneConfirm2 = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmRedAlert)
                ) { Text("Sì, elimina definitivamente", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { deleteZoneConfirm2 = null }) { Text("Annulla") }
            }
        )
    }
}
