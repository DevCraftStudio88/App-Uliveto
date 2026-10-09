package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FarmRepository
import com.example.ui.components.FarmBackButton
import com.example.ui.components.FarmPrimaryButton
import com.example.ui.theme.*

@Composable
fun RegistraUsoProdottoScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fields by FarmRepository.fields.collectAsState()
    val products by FarmRepository.products.collectAsState()

    var selectedField by remember { mutableStateOf("Agrumeto") }
    var selectedProduct by remember { mutableStateOf("Poltiglia bordolese") }
    var amount by remember { mutableStateOf("2 kg") }
    var dateStr by remember { mutableStateOf("Oggi, 8 ottobre") }
    var reminderEnabled by remember { mutableStateOf(true) }

    var showFieldPicker by remember { mutableStateOf(false) }
    var showProductPicker by remember { mutableStateOf(false) }
    var showAmountDialog by remember { mutableStateOf(false) }

    // Dynamic withdrawal period calculation
    val currentProductObj = products.find { it.name == selectedProduct }
    val withdrawalDays = currentProductObj?.withdrawalPeriodDays ?: 20
    val safeDateStr = if (withdrawalDays > 0) "28 ottobre" else "Subito"

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

        Spacer(modifier = Modifier.height(18.dp))

        // Title
        Text(
            text = "Ho usato un prodotto",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = FarmTextPrimary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Selector 1: Dove?
        FormSelectionCard(
            label = "Dove?",
            value = selectedField,
            onClick = { showFieldPicker = true },
            testTag = "select_field_usage"
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Selector 2: Che prodotto?
        FormSelectionCard(
            label = "Che prodotto?",
            value = selectedProduct,
            onClick = { showProductPicker = true },
            testTag = "select_product_usage"
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Selector 3: Quanto?
        FormSelectionCard(
            label = "Quanto?",
            value = amount,
            onClick = { showAmountDialog = true },
            testTag = "select_amount_usage"
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Selector 4: Quando?
        FormSelectionCard(
            label = "Quando?",
            value = dateStr,
            onClick = { /* default is today */ },
            testTag = "select_date_usage"
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Yellow Advisory Box: CARENZA (Page 10)
        if (withdrawalDays > 0) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = FarmYellowGold,
                border = BorderStroke(1.5.dp, FarmCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "CARENZA: $withdrawalDays GIORNI",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Non raccogliere prima\ndel $safeDateStr",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            lineHeight = 24.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Controlla sempre la carenza scritta sull'etichetta.",
                            fontSize = 13.sp,
                            color = Color.Black.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Switch: "Ricordami il 28 ottobre"
            Surface(
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, FarmCardBorder),
                color = FarmSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ricordami il $safeDateStr",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = FarmTextPrimary
                    )
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { reminderEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = FarmGreenDark
                        ),
                        modifier = Modifier.testTag("switch_reminder")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Big Save Button
        FarmPrimaryButton(
            text = "✓ Salva",
            onClick = {
                FarmRepository.recordProductUsage(
                    fieldName = selectedField,
                    productName = selectedProduct,
                    amount = amount,
                    dateStr = dateStr,
                    withdrawalDays = withdrawalDays,
                    safeHarvestDate = safeDateStr,
                    reminder = reminderEnabled
                )
                onBack()
            },
            testTag = "btn_save_usage"
        )

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showFieldPicker) {
        AlertDialog(
            onDismissRequest = { showFieldPicker = false },
            title = { Text("Seleziona terreno", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    fields.forEach { f ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedField = f.name
                                    showFieldPicker = false
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = f.name,
                                fontSize = 16.sp,
                                fontWeight = if (selectedField == f.name) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showProductPicker) {
        AlertDialog(
            onDismissRequest = { showProductPicker = false },
            title = { Text("Seleziona prodotto", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    products.forEach { p ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedProduct = p.name
                                    showProductPicker = false
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = "${p.name} (${p.category})",
                                fontSize = 16.sp,
                                fontWeight = if (selectedProduct == p.name) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showAmountDialog) {
        val keyboardController = LocalSoftwareKeyboardController.current
        var tempAmount by remember { mutableStateOf(amount) }
        AlertDialog(
            onDismissRequest = {
                keyboardController?.hide()
                showAmountDialog = false
            },
            title = { Text("Quantità usata", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempAmount,
                    onValueChange = { tempAmount = it },
                    label = { Text("Dose / Quantità") },
                    placeholder = { Text("es. 2 kg, 1 litro, 3 sacchi") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        keyboardController?.hide()
                        amount = tempAmount
                        showAmountDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark)
                ) {
                    Text("OK", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    keyboardController?.hide()
                    showAmountDialog = false
                }) {
                    Text("Annulla")
                }
            }
        )
    }
}

@Composable
fun FormSelectionCard(
    label: String,
    value: String,
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = FarmTextSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = FarmTextPrimary
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = FarmTextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
