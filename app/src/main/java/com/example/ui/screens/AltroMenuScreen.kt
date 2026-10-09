package com.example.ui.screens

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun AltroMenuScreen(
    onNavigateToOggi: () -> Unit,
    onNavigateToTerreni: () -> Unit,
    onNavigateToAppunti: () -> Unit,
    onNavigateToSpese: () -> Unit,
    onNavigateToCentroOperativo: () -> Unit,
    onNavigateToImpostazioni: () -> Unit,
    onNavigateToPromemoria: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FarmBgCream)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Text(
            text = "Altre sezioni",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = FarmTextPrimary
        )

        Text(
            text = "Accedi rapidamente a tutti gli strumenti dell'azienda",
            fontSize = 15.sp,
            color = FarmTextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        AltroMenuCard(
            title = "Promemoria & Notifiche",
            subtitle = "Avvisi, scadenze, promemoria ricorrenti locali",
            icon = Icons.Default.Notifications,
            iconBg = Color(0xFFF9A825),
            onClick = onNavigateToPromemoria,
            testTag = "menu_promemoria"
        )

        Spacer(modifier = Modifier.height(12.dp))

        AltroMenuCard(
            title = "Oggi",
            subtitle = "Attività giornaliere e scadenze con calendario",
            icon = Icons.Default.Today,
            iconBg = FarmGreenDark,
            onClick = onNavigateToOggi,
            testTag = "menu_oggi"
        )

        Spacer(modifier = Modifier.height(12.dp))

        AltroMenuCard(
            title = "Terreni & Zone",
            subtitle = "Gestione zone, superfici, riordino e dettagli",
            icon = Icons.Default.Forest,
            iconBg = FarmBlueCategory,
            onClick = onNavigateToTerreni,
            testTag = "menu_terreni"
        )

        Spacer(modifier = Modifier.height(12.dp))

        AltroMenuCard(
            title = "Appunti",
            subtitle = "Note agronomiche, osservazioni con riordino e collegamenti",
            icon = Icons.Default.EditNote,
            iconBg = FarmOrangeCategory,
            onClick = onNavigateToAppunti,
            testTag = "menu_appunti"
        )

        Spacer(modifier = Modifier.height(12.dp))

        AltroMenuCard(
            title = "Spese & Movimenti",
            subtitle = "Entrate, spese operative e investimenti con filtri",
            icon = Icons.Default.ReceiptLong,
            iconBg = FarmPurpleCategory,
            onClick = onNavigateToSpese,
            testTag = "menu_spese"
        )

        Spacer(modifier = Modifier.height(12.dp))

        AltroMenuCard(
            title = "Centro operativo",
            subtitle = "Analisi strategica, raccomandazioni e AI",
            icon = Icons.Default.AutoAwesome,
            iconBg = FarmGreenMedium,
            onClick = onNavigateToCentroOperativo,
            testTag = "menu_centro_operativo"
        )

        Spacer(modifier = Modifier.height(12.dp))

        AltroMenuCard(
            title = "Impostazioni",
            subtitle = "Il mio assistente, grandezza testo, salvataggio",
            icon = Icons.Default.Settings,
            iconBg = Color(0xFF5D4037),
            onClick = onNavigateToImpostazioni,
            testTag = "menu_impostazioni"
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun AltroMenuCard(
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
                        .size(44.dp)
                        .background(iconBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = title,
                        fontSize = 18.sp,
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
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = FarmTextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
