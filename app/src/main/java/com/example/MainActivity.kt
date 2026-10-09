package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FarmRepository
import com.example.data.Field
import com.example.data.TextSizeOption
import com.example.ui.components.FarmBottomNavBar
import com.example.ui.components.MainNavTab
import com.example.ui.screens.*
import com.example.ui.theme.*

sealed class ScreenState {
    object MainTab : ScreenState()
    object Oggi : ScreenState()
    object Terreni : ScreenState()
    data class SchedaTerreno(val field: Field) : ScreenState()
    object Appunti : ScreenState()
    object Spese : ScreenState()
    object Prodotti : ScreenState()
    object RegistraUsoProdotto : ScreenState()
    object Diario : ScreenState()
    object CentroOperativo : ScreenState()
    object Impostazioni : ScreenState()
    object Promemoria : ScreenState()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FarmRepository.init(this)
        enableEdgeToEdge()
        setContent {
            val settings by FarmRepository.settings.collectAsState()

            val fontScale = when (settings.textSize) {
                TextSizeOption.NORMALE -> 1.0f
                TextSizeOption.GRANDE -> 1.15f
                TextSizeOption.ENORME -> 1.30f
            }

            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = LocalDensity.current.density,
                    fontScale = fontScale
                )
            ) {
                MyApplicationTheme {
                    MainAppContent()
                }
            }
        }
    }
}

@Composable
fun MainAppContent() {
    val activity = androidx.compose.ui.platform.LocalContext.current as? androidx.activity.ComponentActivity
    var currentTab by remember { mutableStateOf(MainNavTab.HOME) }
    var screenStack by remember { mutableStateOf<List<ScreenState>>(listOf(ScreenState.MainTab)) }

    val currentScreen = screenStack.lastOrNull() ?: ScreenState.MainTab

    fun pushScreen(screen: ScreenState) {
        screenStack = screenStack + screen
    }

    fun popScreen() {
        if (screenStack.size > 1) {
            screenStack = screenStack.dropLast(1)
        }
    }

    // Check intent from notifications or external trigger
    LaunchedEffect(Unit) {
        if (activity?.intent?.getBooleanExtra("open_reminders", false) == true) {
            pushScreen(ScreenState.Promemoria)
        }
        FarmRepository.checkForDueReminders()
    }

    // Handle Android system back gesture
    if (screenStack.size > 1) {
        BackHandler {
            popScreen()
        }
    }

    val showBottomBar = currentScreen is ScreenState.MainTab

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .background(FarmBgCream),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (showBottomBar) {
                FarmBottomNavBar(
                    selectedTab = currentTab,
                    onTabSelected = { tab ->
                        currentTab = tab
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                is ScreenState.MainTab -> {
                    when (currentTab) {
                        MainNavTab.HOME -> {
                            HomeScreen(
                                onNavigateToOggi = { pushScreen(ScreenState.Oggi) },
                                onNavigateToTerreni = { pushScreen(ScreenState.Terreni) },
                                onNavigateToAppunti = { pushScreen(ScreenState.Appunti) },
                                onNavigateToSpese = { pushScreen(ScreenState.Spese) },
                                onNavigateToDiario = { pushScreen(ScreenState.Diario) },
                                onNavigateToCentroOperativo = { pushScreen(ScreenState.CentroOperativo) },
                                onNavigateToPromemoria = { pushScreen(ScreenState.Promemoria) }
                            )
                        }
                        MainNavTab.MAPPA -> {
                            MappaScreen(
                                onBack = { currentTab = MainNavTab.HOME },
                                onOpenFieldDetail = { field -> pushScreen(ScreenState.SchedaTerreno(field)) },
                                onNavigateToDiario = { pushScreen(ScreenState.Diario) }
                            )
                        }
                        MainNavTab.DIARIO -> {
                            DiarioScreen(
                                onBack = { currentTab = MainNavTab.HOME }
                            )
                        }
                        MainNavTab.PRODOTTI -> {
                            ProdottiScreen(
                                onBack = { currentTab = MainNavTab.HOME },
                                onNavigateToUsoProdotto = { pushScreen(ScreenState.RegistraUsoProdotto) }
                            )
                        }
                        MainNavTab.ALTRO -> {
                            AltroMenuScreen(
                                onNavigateToOggi = { pushScreen(ScreenState.Oggi) },
                                onNavigateToTerreni = { pushScreen(ScreenState.Terreni) },
                                onNavigateToAppunti = { pushScreen(ScreenState.Appunti) },
                                onNavigateToSpese = { pushScreen(ScreenState.Spese) },
                                onNavigateToCentroOperativo = { pushScreen(ScreenState.CentroOperativo) },
                                onNavigateToImpostazioni = { pushScreen(ScreenState.Impostazioni) },
                                onNavigateToPromemoria = { pushScreen(ScreenState.Promemoria) }
                            )
                        }
                    }
                }
                is ScreenState.Oggi -> {
                    OggiScreen(
                        onBack = { popScreen() }
                    )
                }
                is ScreenState.Terreni -> {
                    TerreniScreen(
                        onBack = { popScreen() },
                        onSelectField = { field -> pushScreen(ScreenState.SchedaTerreno(field)) },
                        onNavigateToMappa = {
                            popScreen()
                            currentTab = MainNavTab.MAPPA
                        }
                    )
                }
                is ScreenState.SchedaTerreno -> {
                    SchedaTerrenoScreen(
                        field = screen.field,
                        onBack = { popScreen() },
                        onNavigateToDiario = { pushScreen(ScreenState.Diario) }
                    )
                }
                is ScreenState.Appunti -> {
                    AppuntiScreen(
                        onBack = { popScreen() }
                    )
                }
                is ScreenState.Spese -> {
                    SpeseScreen(
                        onBack = { popScreen() }
                    )
                }
                is ScreenState.Prodotti -> {
                    ProdottiScreen(
                        onBack = { popScreen() },
                        onNavigateToUsoProdotto = { pushScreen(ScreenState.RegistraUsoProdotto) }
                    )
                }
                is ScreenState.RegistraUsoProdotto -> {
                    RegistraUsoProdottoScreen(
                        onBack = { popScreen() }
                    )
                }
                is ScreenState.Diario -> {
                    DiarioScreen(
                        onBack = { popScreen() }
                    )
                }
                is ScreenState.CentroOperativo -> {
                    CentroOperativoScreen(
                        onBack = { popScreen() }
                    )
                }
                is ScreenState.Impostazioni -> {
                    ImpostazioniScreen(
                        onBack = { popScreen() },
                        onNavigateToTerreni = { pushScreen(ScreenState.Terreni) },
                        onNavigateToPromemoria = { pushScreen(ScreenState.Promemoria) }
                    )
                }
                is ScreenState.Promemoria -> {
                    PromemoriaScreen(
                        onBack = { popScreen() },
                        onNavigateToZone = { field -> pushScreen(ScreenState.SchedaTerreno(field)) }
                    )
                }
            }
        }
    }

    // Modal In-App Popup per scadenze mentre l'app è aperta (Requirement 44)
    val dueReminder by FarmRepository.dueInAppReminder.collectAsState()
    var showInAppSnooze by remember { mutableStateOf(false) }

    dueReminder?.let { rem ->
        if (!showInAppSnooze) {
            AlertDialog(
                onDismissRequest = { FarmRepository.dismissInAppReminder() },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = FarmGreenDark,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Promemoria", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            rem.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = FarmTextPrimary
                        )
                        if (rem.description.isNotBlank()) {
                            Text(
                                rem.description,
                                fontSize = 14.sp,
                                color = FarmTextSecondary
                            )
                        }
                        if (!rem.zoneName.isNullOrBlank()) {
                            Text(
                                "Zona: ${rem.zoneName}",
                                fontSize = 12.sp,
                                color = FarmBlueCategory,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                FarmRepository.toggleReminderCompleted(rem.id, activity)
                                FarmRepository.dismissInAppReminder()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark)
                        ) {
                            Text("Fatto", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(onClick = { showInAppSnooze = true }) {
                            Text("Posticipa")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            FarmRepository.dismissInAppReminder()
                            pushScreen(ScreenState.Promemoria)
                        }
                    ) {
                        Text("Apri dettaglio")
                    }
                }
            )
        } else {
            SnoozeDialog(
                reminder = rem,
                onDismiss = {
                    showInAppSnooze = false
                    FarmRepository.dismissInAppReminder()
                },
                onSnoozeMinutes = { mins ->
                    FarmRepository.snoozeReminder(rem.id, offsetMinutes = mins, context = activity)
                    showInAppSnooze = false
                },
                onSnoozeTomorrow = {
                    val cal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, 1) }
                    val tom = String.format("%02d/%02d/%04d", cal.get(java.util.Calendar.DAY_OF_MONTH), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.YEAR))
                    FarmRepository.snoozeReminder(rem.id, newDateStr = tom, context = activity)
                    showInAppSnooze = false
                },
                onSnoozeCustom = { d, t ->
                    FarmRepository.snoozeReminder(rem.id, newDateStr = d, newTimeStr = t, context = activity)
                    showInAppSnooze = false
                }
            )
        }
    }
}
