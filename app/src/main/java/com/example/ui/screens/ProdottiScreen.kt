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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExpiryStatus
import com.example.data.FarmRepository
import com.example.data.ProductItem
import com.example.ui.components.FarmBackButton
import com.example.ui.components.FarmPrimaryButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

@Composable
fun ProdottiScreen(
    onBack: () -> Unit,
    onNavigateToUsoProdotto: () -> Unit,
    modifier: Modifier = Modifier
) {
    val products by FarmRepository.products.collectAsState()

    var selectedFilter by remember { mutableStateOf("Tutti") }
    var showAddDialog by remember { mutableStateOf(false) }

    val expiringCount = products.count { it.expiryStatus != ExpiryStatus.OK }

    val filteredProducts = when (selectedFilter) {
        "In scadenza" -> products.filter { it.expiryStatus != ExpiryStatus.OK }
        "Finiti" -> emptyList()
        else -> products
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FarmBgCream)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Back Button & "+ Nuovo"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FarmBackButton(onBack = onBack)

            Surface(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, FarmCardBorder),
                color = FarmSurface,
                modifier = Modifier
                    .height(42.dp)
                    .testTag("btn_new_product")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = FarmTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Nuovo",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = FarmTextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Prodotti",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = FarmTextPrimary
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Warning banner: 2 prodotti da usare o buttare (Page 9)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = FarmYellowGold,
            border = BorderStroke(1.5.dp, FarmCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "$expiringCount prodotti da usare o\nbuttare",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Advisory card with clock
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFFF9E6),
            border = BorderStroke(1.dp, Color(0xFFFFD54F)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = Color(0xFFF57F17),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Limoni: non raccogliere prima del 14 ottobre",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = FarmTextPrimary
                    )
                    Text(
                        text = "Dopo il rame del 24 settembre, carenza 20 giorni. Controlla sempre l'etichetta.",
                        fontSize = 12.sp,
                        color = FarmTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filter chips: Tutti 4, In scadenza 2, Finiti 0
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = (selectedFilter == "Tutti"),
                onClick = { selectedFilter = "Tutti" },
                label = { Text("Tutti ${products.size}") }
            )
            FilterChip(
                selected = (selectedFilter == "In scadenza"),
                onClick = { selectedFilter = "In scadenza" },
                label = { Text("In scadenza $expiringCount") }
            )
            FilterChip(
                selected = (selectedFilter == "Finiti"),
                onClick = { selectedFilter = "Finiti" },
                label = { Text("Finiti 0") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Product Cards
        filteredProducts.forEach { product ->
            ProductCard(
                product = product,
                onDelete = { FarmRepository.deleteProduct(product.id) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Big Bottom CTA: "Ho usato un prodotto"
        FarmPrimaryButton(
            text = "Ho usato un prodotto",
            onClick = onNavigateToUsoProdotto,
            testTag = "btn_used_product"
        )

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showAddDialog) {
        AddProductDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, category, qty, expiryLabel, withdrawal ->
                FarmRepository.addProduct(name, category, qty, expiryLabel, withdrawal)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ProductCard(
    product: ProductItem,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, FarmCardBorder),
        color = FarmSurface,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("prod_item_${product.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = FarmTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${product.category} · ${product.quantity}",
                        fontSize = 14.sp,
                        color = FarmTextSecondary
                    )
                }

                // Expiry status badge
                when (product.expiryStatus) {
                    ExpiryStatus.EXPIRED -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = FarmRedAlert
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Scaduto",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    ExpiryStatus.EXPIRING_SOON -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = FarmYellowGold
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = product.expiryLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                    ExpiryStatus.OK -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = FarmGreenLight
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = FarmGreenDark,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = product.expiryLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FarmGreenDark
                                )
                            }
                        }
                    }
                }
            }

            if (product.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = product.notes,
                    fontSize = 12.sp,
                    color = FarmTextSecondary
                )
            }
        }
    }
}

@Composable
fun AddProductDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, category: String, qty: String, expiry: String, withdrawal: Int) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Anticrittogamico") }
    var quantity by remember { mutableStateOf("") }
    var expiryLabel by remember { mutableStateOf("Va bene") }
    var withdrawalDays by remember { mutableIntStateOf(20) }

    AlertDialog(
        onDismissRequest = {
            keyboardController?.hide()
            onDismiss()
        },
        title = { Text("Aggiungi prodotto", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome prodotto") },
                    placeholder = { Text("es. Poltiglia bordolese") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Categoria") },
                    placeholder = { Text("es. Anticrittogamico, Concime") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantità in magazzino") },
                    placeholder = { Text("es. 5 kg, 2 litri, 4 sacchi") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = expiryLabel,
                    onValueChange = { expiryLabel = it },
                    label = { Text("Stato scadenza") },
                    placeholder = { Text("es. Scade 03/2028, Scaduto") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = withdrawalDays.toString(),
                    onValueChange = { withdrawalDays = it.toIntOrNull() ?: 0 },
                    label = { Text("Tempo di carenza (giorni)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    keyboardController?.hide()
                    if (name.isNotBlank()) {
                        onConfirm(name, category, quantity, expiryLabel, withdrawalDays)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark)
            ) {
                Text("Aggiungi", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                keyboardController?.hide()
                onDismiss()
            }) { Text("Annulla") }
        }
    )
}
