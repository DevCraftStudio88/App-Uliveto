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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EconomicMovement
import com.example.data.EconomicMovementType
import com.example.data.FarmRepository
import com.example.ui.components.FarmBackButton
import com.example.ui.components.FarmDateSelector
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SpeseScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val movements by FarmRepository.movements.collectAsState()
    val fields by FarmRepository.fields.collectAsState()

    var selectedTypeFilter by remember { mutableStateOf<EconomicMovementType?>(null) }
    var selectedFieldFilterId by remember { mutableStateOf<String?>(null) }

    var showNewMovementDialog by remember { mutableStateOf(false) }
    var movementToEdit by remember { mutableStateOf<EconomicMovement?>(null) }

    // Double confirmation for delete
    var deleteConfirm1 by remember { mutableStateOf<EconomicMovement?>(null) }
    var deleteConfirm2 by remember { mutableStateOf<EconomicMovement?>(null) }

    // Totals calculations
    val totalEntrate = movements.filter { it.type == EconomicMovementType.ENTRATA }.sumOf { it.amount }
    val totalSpeseOperative = movements.filter { it.type == EconomicMovementType.SPESA }.sumOf { it.amount }
    val totalInvestimenti = movements.filter { it.type == EconomicMovementType.INVESTIMENTO }.sumOf { it.amount }
    val saldoOperativo = totalEntrate - totalSpeseOperative

    val filteredMovements = movements.filter { m ->
        (selectedTypeFilter == null || m.type == selectedTypeFilter) &&
                (selectedFieldFilterId == null || m.fieldId == selectedFieldFilterId)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FarmBgCream)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Back Button & "+ Nuovo movimento"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FarmBackButton(onBack = onBack)

            Button(
                onClick = { showNewMovementDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(42.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nuovo movimento", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Spese & Movimenti",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = FarmTextPrimary
        )

        Text(
            text = "Gestione contabile ed economica dell'azienda",
            fontSize = 15.sp,
            color = FarmTextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Distinct KPI Cards: ENTRATE | SPESE OPERATIVE | INVESTIMENTI
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EconomicSummaryBox(
                title = "ENTRATE",
                amount = totalEntrate,
                color = FarmGreenDark,
                modifier = Modifier.weight(1f)
            )
            EconomicSummaryBox(
                title = "SPESE OPERATIVE",
                amount = totalSpeseOperative,
                color = FarmRedAlert,
                modifier = Modifier.weight(1f)
            )
            EconomicSummaryBox(
                title = "INVESTIMENTI",
                amount = totalInvestimenti,
                color = Color(0xFF1565C0),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Saldo Operativo bar
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (saldoOperativo >= 0) FarmGreenLight else Color(0xFFFFEBEE),
            border = BorderStroke(1.dp, if (saldoOperativo >= 0) FarmGreenDark else FarmRedAlert),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saldo Operativo (Entrate − Spese):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FarmTextPrimary
                )
                Text(
                    text = "${if (saldoOperativo >= 0) "+" else ""}${String.format(Locale.ITALY, "%,.2f", saldoOperativo)} €",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (saldoOperativo >= 0) FarmGreenDark else FarmRedAlert
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Filters Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedTypeFilter == null,
                onClick = { selectedTypeFilter = null },
                label = { Text("Tutti (${movements.size})", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedTypeFilter == EconomicMovementType.ENTRATA,
                onClick = { selectedTypeFilter = EconomicMovementType.ENTRATA },
                label = { Text("Entrate", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedTypeFilter == EconomicMovementType.SPESA,
                onClick = { selectedTypeFilter = EconomicMovementType.SPESA },
                label = { Text("Spese", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedTypeFilter == EconomicMovementType.INVESTIMENTO,
                onClick = { selectedTypeFilter = EconomicMovementType.INVESTIMENTO },
                label = { Text("Investimenti", fontSize = 11.sp) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Movements List
        if (filteredMovements.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = FarmTextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Non hai ancora inserito movimenti.",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FarmTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Registra entrate, spese operative o investimenti con '+ Nuovo movimento'.",
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
                items(filteredMovements, key = { it.id }) { mov ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.2.dp, FarmCardBorder),
                        color = FarmSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val typeBadgeColor = when (mov.type) {
                                        EconomicMovementType.ENTRATA -> FarmGreenLight
                                        EconomicMovementType.SPESA -> Color(0xFFFFEBEE)
                                        EconomicMovementType.INVESTIMENTO -> Color(0xFFE3F2FD)
                                    }
                                    val typeTextColor = when (mov.type) {
                                        EconomicMovementType.ENTRATA -> FarmGreenDark
                                        EconomicMovementType.SPESA -> FarmRedAlert
                                        EconomicMovementType.INVESTIMENTO -> Color(0xFF1565C0)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = typeBadgeColor
                                    ) {
                                        Text(
                                            text = mov.type.name,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = typeTextColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = mov.dateStr,
                                        fontSize = 12.sp,
                                        color = FarmTextSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = mov.description,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FarmTextPrimary
                                )

                                val subDetails = listOfNotNull(
                                    mov.category.ifBlank { null },
                                    mov.fieldName?.let { "Zona: $it" },
                                    mov.destination?.let { "Destinazione: $it" }
                                ).joinToString(" · ")

                                if (subDetails.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = subDetails,
                                        fontSize = 12.sp,
                                        color = FarmTextSecondary
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                val amountPrefix = when (mov.type) {
                                    EconomicMovementType.ENTRATA -> "+"
                                    EconomicMovementType.SPESA -> "−"
                                    EconomicMovementType.INVESTIMENTO -> "Inv. "
                                }
                                val amountColor = when (mov.type) {
                                    EconomicMovementType.ENTRATA -> FarmGreenDark
                                    EconomicMovementType.SPESA -> FarmRedAlert
                                    EconomicMovementType.INVESTIMENTO -> Color(0xFF1565C0)
                                }

                                Text(
                                    text = "$amountPrefix${String.format(Locale.ITALY, "%,.2f", mov.amount)} €",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = amountColor
                                )

                                Row {
                                    IconButton(
                                        onClick = { movementToEdit = mov },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Modifica", tint = FarmTextPrimary, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { deleteConfirm1 = mov },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Elimina", tint = FarmRedAlert, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: New Movement (Choice between Entrata, Spesa, Investimento)
    if (showNewMovementDialog) {
        MovementFormDialog(
            title = "Nuovo Movimento",
            initialMovement = null,
            fields = fields,
            onDismiss = { showNewMovementDialog = false },
            onSave = { type, desc, amt, dateStr, cat, fId, fName, dest, notes ->
                FarmRepository.addMovement(type, desc, amt, dateStr, cat, fId, fName, dest, notes)
                showNewMovementDialog = false
            }
        )
    }

    // Modal: Edit Movement (Same record updated, no duplication)
    movementToEdit?.let { mov ->
        MovementFormDialog(
            title = "Modifica Movimento",
            initialMovement = mov,
            fields = fields,
            onDismiss = { movementToEdit = null },
            onSave = { type, desc, amt, dateStr, cat, fId, fName, dest, notes ->
                FarmRepository.updateMovement(mov.id, type, desc, amt, dateStr, cat, fId, fName, dest, notes)
                movementToEdit = null
            }
        )
    }

    // Double Confirmation: Delete Movement
    deleteConfirm1?.let { mov ->
        AlertDialog(
            onDismissRequest = { deleteConfirm1 = null },
            title = { Text("Eliminare questo movimento?", fontWeight = FontWeight.Bold) },
            text = { Text("Vuoi davvero rimuovere '${mov.description}' da ${String.format(Locale.ITALY, "%.2f", mov.amount)} €?") },
            confirmButton = {
                Button(
                    onClick = {
                        deleteConfirm2 = mov
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

    deleteConfirm2?.let { mov ->
        AlertDialog(
            onDismissRequest = { deleteConfirm2 = null },
            title = { Text("Conferma definitiva eliminazione", fontWeight = FontWeight.Bold, color = FarmRedAlert) },
            text = { Text("Questa operazione eliminerà definitivamente il movimento e ricalcolerà i totali. Confermi?") },
            confirmButton = {
                Button(
                    onClick = {
                        FarmRepository.deleteMovement(mov.id)
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
}

@Composable
fun EconomicSummaryBox(
    title: String,
    amount: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.2.dp, FarmCardBorder),
        color = FarmSurface,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FarmTextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${String.format(Locale.ITALY, "%,.0f", amount)} €",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

@Composable
fun MovementFormDialog(
    title: String,
    initialMovement: EconomicMovement?,
    fields: List<com.example.data.Field>,
    onDismiss: () -> Unit,
    onSave: (
        type: EconomicMovementType,
        description: String,
        amount: Double,
        dateStr: String,
        category: String,
        fieldId: String?,
        fieldName: String?,
        destination: String?,
        notes: String
    ) -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current

    var selectedType by remember { mutableStateOf(initialMovement?.type ?: EconomicMovementType.SPESA) }
    var description by remember { mutableStateOf(initialMovement?.description ?: "") }
    var amountStr by remember { mutableStateOf(initialMovement?.amount?.toString() ?: "") }
    var dateStr by remember {
        mutableStateOf(
            initialMovement?.dateStr ?: SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN).format(Date())
        )
    }
    var category by remember { mutableStateOf(initialMovement?.category ?: "Generale") }
    var destination by remember { mutableStateOf(initialMovement?.destination ?: "") }
    var selectedFieldId by remember { mutableStateOf(initialMovement?.fieldId) }
    var notes by remember { mutableStateOf(initialMovement?.notes ?: "") }

    val selectedFieldName = fields.find { it.id == selectedFieldId }?.name

    AlertDialog(
        onDismissRequest = {
            keyboard?.hide()
            onDismiss()
        },
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Type selector: 3 clear choices
                Text("Tipologia movimento:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedType == EconomicMovementType.ENTRATA,
                        onClick = { selectedType = EconomicMovementType.ENTRATA },
                        label = { Text("Entrata") }
                    )
                    FilterChip(
                        selected = selectedType == EconomicMovementType.SPESA,
                        onClick = { selectedType = EconomicMovementType.SPESA },
                        label = { Text("Spesa") }
                    )
                    FilterChip(
                        selected = selectedType == EconomicMovementType.INVESTIMENTO,
                        onClick = { selectedType = EconomicMovementType.INVESTIMENTO },
                        label = { Text("Investimento") }
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrizione *") },
                    placeholder = { Text("es. Vendita olio, Ricambi, Nuovo impianto") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Importo (€) *") },
                    placeholder = { Text("es. 350.00") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Date Selector with real calendar
                FarmDateSelector(
                    selectedDateStr = dateStr,
                    onDateSelected = { dateStr = it },
                    label = "Data del movimento *"
                )

                if (selectedType == EconomicMovementType.INVESTIMENTO) {
                    OutlinedTextField(
                        value = destination,
                        onValueChange = { destination = it },
                        label = { Text("Destinazione investimento *") },
                        placeholder = { Text("es. Trattore, Terreno adiacente, Macchinario") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Categoria") },
                    placeholder = { Text("es. Carburante, Manodopera, Prodotti, Macchinari") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (fields.isNotEmpty()) {
                    Text("Terreno / Zona associata:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
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
                    val amt = amountStr.replace(',', '.').toDoubleOrNull() ?: 0.0
                    if (description.isNotBlank() && amt > 0.0) {
                        onSave(
                            selectedType,
                            description.trim(),
                            amt,
                            dateStr,
                            category.trim(),
                            selectedFieldId,
                            selectedFieldName,
                            destination.trim().ifBlank { null },
                            notes.trim()
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark),
                enabled = description.isNotBlank() && (amountStr.replace(',', '.').toDoubleOrNull() ?: 0.0) > 0.0
            ) {
                Text("Salva", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                keyboard?.hide()
                onDismiss()
            }) { Text("Annulla") }
        }
    )
}
