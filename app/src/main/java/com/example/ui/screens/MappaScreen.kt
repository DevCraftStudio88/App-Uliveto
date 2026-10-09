package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.FarmProperty
import com.example.data.FarmRepository
import com.example.data.Field
import com.example.data.GeoPoint
import com.example.ui.components.FarmBackButton
import com.example.ui.theme.*
import org.json.JSONArray
import org.json.JSONObject

enum class MapInteractionMode {
    VIEW,
    DRAW_PROPERTY,
    DRAW_ZONE
}

class MapBridge(
    private val onPolygonPointsChanged: (List<GeoPoint>, Double, Double) -> Unit,
    private val onZoneSelected: (String) -> Unit
) {
    @JavascriptInterface
    fun updateDrawingPoints(jsonStr: String, areaM2: Double, areaHa: Double) {
        try {
            val arr = JSONArray(jsonStr)
            val pts = mutableListOf<GeoPoint>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                pts.add(GeoPoint(obj.getDouble("lat"), obj.getDouble("lng")))
            }
            onPolygonPointsChanged(pts, areaM2, areaHa)
        } catch (_: Exception) {}
    }

    @JavascriptInterface
    fun selectZone(zoneId: String) {
        onZoneSelected(zoneId)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MappaScreen(
    onBack: () -> Unit,
    onOpenFieldDetail: (Field) -> Unit,
    onNavigateToDiario: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val property by FarmRepository.property.collectAsState()
    val fields by FarmRepository.fields.collectAsState()

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var interactionMode by remember { mutableStateOf(MapInteractionMode.VIEW) }
    var isSatelliteLayer by remember { mutableStateOf(true) }

    // Real-time drawing state
    var currentDrawnPoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var currentDrawnM2 by remember { mutableDoubleStateOf(0.0) }
    var currentDrawnHa by remember { mutableDoubleStateOf(0.0) }

    // Dialog state for new Zone details
    var showZoneSaveDialog by remember { mutableStateOf(false) }
    var pendingZonePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }

    // Selected Zone for detail card
    var selectedZoneId by remember { mutableStateOf<String?>(null) }
    val selectedField = fields.find { it.id == selectedZoneId }

    // Double Confirmation Dialog states
    var showDeletePropConfirm1 by remember { mutableStateOf(false) }
    var showDeletePropConfirm2 by remember { mutableStateOf(false) }

    var showDeleteZoneConfirm1 by remember { mutableStateOf<Field?>(null) }
    var showDeleteZoneConfirm2 by remember { mutableStateOf<Field?>(null) }

    // Edit zone dialog
    var fieldToEdit by remember { mutableStateOf<Field?>(null) }

    // GPS tracking
    var gpsStatusMessage by remember { mutableStateOf("Centra GPS") }
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            locateUser(context) { loc ->
                if (loc != null) {
                    gpsStatusMessage = "GPS (±${loc.accuracy.toInt()}m)"
                    webViewRef?.evaluateJavascript(
                        "centerOnCoords(${loc.latitude}, ${loc.longitude}, ${loc.accuracy});",
                        null
                    )
                } else {
                    gpsStatusMessage = "Segnale debole"
                }
            }
        } else {
            gpsStatusMessage = "Permesso negato"
        }
    }

    // Leaflet HTML initialization string
    val leafletHtml = remember { generateLeafletMapHtml() }

    fun refreshMapPolygons() {
        val wv = webViewRef ?: return
        // Pass property polygon
        val propJson = JSONArray().apply {
            property?.polygon?.forEach { pt ->
                put(JSONObject().apply {
                    put("lat", pt.latitude)
                    put("lng", pt.longitude)
                })
            }
        }.toString()

        // Pass zones polygons
        val zonesJson = JSONArray().apply {
            fields.forEach { f ->
                put(JSONObject().apply {
                    put("id", f.id)
                    put("name", f.name)
                    put("crop", f.crop)
                    put("colorHex", String.format("#%06X", (0xFFFFFF and f.colorHex.toInt())))
                    val pts = JSONArray()
                    f.polygon.forEach { pt ->
                        pts.put(JSONObject().apply {
                            put("lat", pt.latitude)
                            put("lng", pt.longitude)
                        })
                    }
                    put("points", pts)
                })
            }
        }.toString()

        val propB64 = android.util.Base64.encodeToString(propJson.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        val zonesB64 = android.util.Base64.encodeToString(zonesJson.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        wv.evaluateJavascript("loadExistingGeometryBase64('$propB64', '$zonesB64');", null)
    }

    LaunchedEffect(property, fields) {
        refreshMapPolygons()
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF1E281F))) {
        // Real Geographic Map in WebView
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                    webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                            android.util.Log.d("LeafletMap", "${consoleMessage?.message()} -- line ${consoleMessage?.lineNumber()}")
                            return true
                        }
                    }

                    addJavascriptInterface(
                        MapBridge(
                            onPolygonPointsChanged = { pts, m2, ha ->
                                currentDrawnPoints = pts
                                currentDrawnM2 = m2
                                currentDrawnHa = ha
                            },
                            onZoneSelected = { id ->
                                selectedZoneId = id
                            }
                        ),
                        "AndroidBridge"
                    )

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            view?.postDelayed({
                                view.evaluateJavascript("invalidateMapSize();", null)
                                refreshMapPolygons()
                            }, 150)
                        }
                    }

                    loadDataWithBaseURL("file:///android_asset/", leafletHtml, "text/html", "UTF-8", null)
                    webViewRef = this
                }
            },
            update = { wv ->
                webViewRef = wv
            }
        )

        // Top Header and Controls
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FarmBackButton(onBack = onBack, label = "Indietro")

                // GPS Centering Pill
                Surface(
                    onClick = {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            locateUser(context) { loc ->
                                if (loc != null) {
                                    gpsStatusMessage = "GPS (±${loc.accuracy.toInt()}m)"
                                    webViewRef?.evaluateJavascript(
                                        "centerOnCoords(${loc.latitude}, ${loc.longitude}, ${loc.accuracy});",
                                        null
                                    )
                                } else {
                                    gpsStatusMessage = "Posizione non disponibile"
                                }
                            }
                        } else {
                            locationLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = FarmSurface,
                    border = BorderStroke(1.2.dp, FarmCardBorder),
                    modifier = Modifier.testTag("btn_center_gps")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "GPS",
                            tint = Color(0xFF1E88E5),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = gpsStatusMessage,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = FarmTextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mode Indicator Bar
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = when (interactionMode) {
                    MapInteractionMode.VIEW -> FarmSurface.copy(alpha = 0.95f)
                    MapInteractionMode.DRAW_PROPERTY -> Color(0xFFFFF3CD)
                    MapInteractionMode.DRAW_ZONE -> Color(0xFFE8F5E9)
                },
                border = BorderStroke(1.5.dp, FarmCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    when (interactionMode) {
                        MapInteractionMode.VIEW -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (property != null) "Proprietà: ${property?.name}" else "Nessuna proprietà tracciata",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FarmTextPrimary
                                    )
                                    Text(
                                        text = if (property != null) "${String.format("%.1f", property?.areaHectares)} ha (${String.format("%.0f", property?.areaSquareMeters)} m²) · ${fields.size} zone" else "Tocca 'Disegna proprietà' per iniziare",
                                        fontSize = 12.sp,
                                        color = FarmTextSecondary
                                    )
                                }

                                if (property != null) {
                                    IconButton(
                                        onClick = { showDeletePropConfirm1 = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Elimina proprietà",
                                            tint = FarmRedAlert,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                        MapInteractionMode.DRAW_PROPERTY -> {
                            Text(
                                text = "✏️ Disegno confine proprietà",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF856404)
                            )
                            Text(
                                text = "Tocca la mappa per aggiungere punti (${currentDrawnPoints.size} vertici) · ${String.format("%.0f", currentDrawnM2)} m² (${String.format("%.2f", currentDrawnHa)} ha)",
                                fontSize = 12.sp,
                                color = FarmTextPrimary
                            )
                        }
                        MapInteractionMode.DRAW_ZONE -> {
                            Text(
                                text = "🌱 Disegno nuova zona",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = FarmGreenDark
                            )
                            Text(
                                text = "Tocca per tracciare il poligono (${currentDrawnPoints.size} vertici) · ${String.format("%.0f", currentDrawnM2)} m² (${String.format("%.2f", currentDrawnHa)} ha)",
                                fontSize = 12.sp,
                                color = FarmTextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Right side floating controls: Layer switch (Satellite/Stradale) & Zoom
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                onClick = {
                    isSatelliteLayer = !isSatelliteLayer
                    val layerName = if (isSatelliteLayer) "satellite" else "streets"
                    webViewRef?.evaluateJavascript("switchBaseLayer('$layerName');", null)
                },
                shape = CircleShape,
                border = BorderStroke(1.5.dp, FarmCardBorder),
                color = FarmSurface,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isSatelliteLayer) Icons.Default.Satellite else Icons.Default.Map,
                        contentDescription = "Cambia vista",
                        tint = FarmTextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Surface(
                onClick = { webViewRef?.evaluateJavascript("map.zoomIn();", null) },
                shape = CircleShape,
                border = BorderStroke(1.5.dp, FarmCardBorder),
                color = FarmSurface,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom avanti",
                        tint = FarmTextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Surface(
                onClick = { webViewRef?.evaluateJavascript("map.zoomOut();", null) },
                shape = CircleShape,
                border = BorderStroke(1.5.dp, FarmCardBorder),
                color = FarmSurface,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom indietro",
                        tint = FarmTextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Bottom Action Bar based on Mode
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            when (interactionMode) {
                MapInteractionMode.VIEW -> {
                    // Action buttons to start drawing Property or Zone
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                interactionMode = MapInteractionMode.DRAW_PROPERTY
                                currentDrawnPoints = emptyList()
                                currentDrawnM2 = 0.0
                                currentDrawnHa = 0.0
                                webViewRef?.evaluateJavascript("startDrawing('property');", null)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FarmYellowGold),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text(
                                text = if (property == null) "Disegna proprietà" else "Ridisegna confini",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Button(
                            onClick = {
                                interactionMode = MapInteractionMode.DRAW_ZONE
                                currentDrawnPoints = emptyList()
                                currentDrawnM2 = 0.0
                                currentDrawnHa = 0.0
                                webViewRef?.evaluateJavascript("startDrawing('zone');", null)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text(
                                text = "Disegna nuova zona",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                MapInteractionMode.DRAW_PROPERTY, MapInteractionMode.DRAW_ZONE -> {
                    // Drawing toolbar: Annulla ultimo punto, Annulla tutto, Conferma
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.5.dp, FarmCardBorder),
                        color = FarmSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        webViewRef?.evaluateJavascript("undoLastPoint();", null)
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, FarmCardBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Annulla punto", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        interactionMode = MapInteractionMode.VIEW
                                        webViewRef?.evaluateJavascript("cancelDrawing();", null)
                                        refreshMapPolygons()
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, FarmCardBorder),
                                    modifier = Modifier.weight(0.8f)
                                ) {
                                    Text("Annulla tutto", fontSize = 12.sp, color = FarmRedAlert)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    if (currentDrawnPoints.size >= 3) {
                                        if (interactionMode == MapInteractionMode.DRAW_PROPERTY) {
                                            FarmRepository.saveProperty(currentDrawnPoints)
                                            interactionMode = MapInteractionMode.VIEW
                                            webViewRef?.evaluateJavascript("finishDrawing();", null)
                                            refreshMapPolygons()
                                        } else {
                                            pendingZonePoints = currentDrawnPoints
                                            showZoneSaveDialog = true
                                        }
                                    }
                                },
                                enabled = currentDrawnPoints.size >= 3,
                                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text(
                                    text = if (currentDrawnPoints.size < 3) "Aggiungi almeno 3 punti (${currentDrawnPoints.size}/3)" else "✓ Conferma e Salva (${String.format("%.2f", currentDrawnHa)} ha)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Selected Zone detail card
            selectedField?.let { field ->
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.5.dp, FarmCardBorder),
                    color = FarmSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = field.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = FarmTextPrimary
                                )
                                Text(
                                    text = "${if (field.crop.isNotBlank()) field.crop + " · " else ""}${String.format("%.2f", field.areaHectares)} ha (${String.format("%.0f", field.areaSquareMeters)} m²)",
                                    fontSize = 13.sp,
                                    color = FarmTextSecondary
                                )
                            }

                            Row {
                                IconButton(onClick = { fieldToEdit = field }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Modifica", tint = FarmTextPrimary)
                                }
                                IconButton(onClick = { showDeleteZoneConfirm1 = field }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Elimina", tint = FarmRedAlert)
                                }
                            }
                        }

                        if (field.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = field.notes, fontSize = 12.sp, color = FarmTextSecondary)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onOpenFieldDetail(field) },
                                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Apri scheda zona", color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { selectedZoneId = null },
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, FarmCardBorder)
                            ) {
                                Text("Chiudi", color = FarmTextPrimary)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog to save new Zone
    if (showZoneSaveDialog) {
        var zoneName by remember { mutableStateOf("") }
        var zoneCrop by remember { mutableStateOf("") }
        var zoneNotes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showZoneSaveDialog = false },
            title = { Text("Nuova Zona", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Superficie rilevata: ${String.format("%.0f", currentDrawnM2)} m² (${String.format("%.2f", currentDrawnHa)} ha)",
                        fontWeight = FontWeight.Bold,
                        color = FarmGreenDark
                    )
                    OutlinedTextField(
                        value = zoneName,
                        onValueChange = { zoneName = it },
                        label = { Text("Nome zona *") },
                        placeholder = { Text("es. Appezzamento Nord") },
                        modifier = Modifier.fillMaxWidth().testTag("input_zone_name")
                    )
                    OutlinedTextField(
                        value = zoneCrop,
                        onValueChange = { zoneCrop = it },
                        label = { Text("Coltura (opzionale)") },
                        placeholder = { Text("es. Ulivi Leccino") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = zoneNotes,
                        onValueChange = { zoneNotes = it },
                        label = { Text("Appunti / Note") },
                        placeholder = { Text("es. Impianto 2020, sesto 6x6") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (zoneName.isNotBlank()) {
                            FarmRepository.addField(
                                name = zoneName.trim(),
                                crop = zoneCrop.trim(),
                                notes = zoneNotes.trim(),
                                polygon = pendingZonePoints
                            )
                            showZoneSaveDialog = false
                            interactionMode = MapInteractionMode.VIEW
                            webViewRef?.evaluateJavascript("finishDrawing();", null)
                            refreshMapPolygons()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark),
                    enabled = zoneName.isNotBlank()
                ) {
                    Text("Salva Zona", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showZoneSaveDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }

    // Edit Zone Dialog
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
                        label = { Text("Nome zona") },
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
                        label = { Text("Note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        FarmRepository.updateField(f.id, editName, editCrop, editNotes)
                        fieldToEdit = null
                        refreshMapPolygons()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark)
                ) { Text("Salva modifiche", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { fieldToEdit = null }) { Text("Annulla") }
            }
        )
    }

    // Double Confirmation: Delete Property
    if (showDeletePropConfirm1) {
        AlertDialog(
            onDismissRequest = { showDeletePropConfirm1 = false },
            title = { Text("Eliminare la proprietà?", fontWeight = FontWeight.Bold) },
            text = { Text("Vuoi davvero rimuovere i confini della proprietà dalla mappa?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeletePropConfirm1 = false
                        showDeletePropConfirm2 = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmRedAlert)
                ) { Text("Procedi", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePropConfirm1 = false }) { Text("Annulla") }
            }
        )
    }

    if (showDeletePropConfirm2) {
        AlertDialog(
            onDismissRequest = { showDeletePropConfirm2 = false },
            title = { Text("Conferma definitiva", fontWeight = FontWeight.Bold, color = FarmRedAlert) },
            text = { Text("Questa operazione eliminerà definitivamente il perimetro della proprietà. Confermi l'eliminazione?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeletePropConfirm2 = false
                        FarmRepository.deleteProperty()
                        refreshMapPolygons()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmRedAlert)
                ) { Text("Sì, elimina definitivamente", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePropConfirm2 = false }) { Text("Annulla") }
            }
        )
    }

    // Double Confirmation: Delete Zone
    showDeleteZoneConfirm1?.let { z ->
        AlertDialog(
            onDismissRequest = { showDeleteZoneConfirm1 = null },
            title = { Text("Eliminare la zona '${z.name}'?", fontWeight = FontWeight.Bold) },
            text = { Text("Vuoi davvero eliminare questa zona?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteZoneConfirm2 = z
                        showDeleteZoneConfirm1 = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmRedAlert)
                ) { Text("Procedi", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteZoneConfirm1 = null }) { Text("Annulla") }
            }
        )
    }

    showDeleteZoneConfirm2?.let { z ->
        AlertDialog(
            onDismissRequest = { showDeleteZoneConfirm2 = null },
            title = { Text("Conferma definitiva eliminazione", fontWeight = FontWeight.Bold, color = FarmRedAlert) },
            text = { Text("Questa operazione eliminerà definitivamente la zona '${z.name}', i suoi confini e i dati associati. Confermi?") },
            confirmButton = {
                Button(
                    onClick = {
                        FarmRepository.deleteField(z.id)
                        showDeleteZoneConfirm2 = null
                        selectedZoneId = null
                        refreshMapPolygons()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmRedAlert)
                ) { Text("Sì, elimina definitivamente", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteZoneConfirm2 = null }) { Text("Annulla") }
            }
        )
    }
}

@SuppressLint("MissingPermission")
private fun locateUser(context: Context, onResult: (Location?) -> Unit) {
    try {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return onResult(null)
        val provider = if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
        val last = lm.getLastKnownLocation(provider)
        if (last != null) {
            onResult(last)
            return
        }
        lm.requestSingleUpdate(provider, object : LocationListener {
            override fun onLocationChanged(l: Location) { onResult(l) }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
            override fun onProviderEnabled(p: String) {}
            override fun onProviderDisabled(p: String) {}
        }, null)
    } catch (_: Exception) {
        onResult(null)
    }
}

private fun generateLeafletMapHtml(): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="leaflet/leaflet.css" onerror="this.onerror=null;this.href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css';" />
            <script src="leaflet/leaflet.js"></script>
            <script>
                if (typeof L === 'undefined') {
                    document.write('<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"><\/script>');
                }
            </script>
            <style>
                html, body {
                    margin: 0;
                    padding: 0;
                    width: 100%;
                    height: 100%;
                    overflow: hidden;
                    background: #1b261e;
                }
                #map {
                    position: absolute;
                    top: 0;
                    bottom: 0;
                    left: 0;
                    right: 0;
                    width: 100%;
                    height: 100%;
                    background: #1b261e;
                }
                .vertex-marker {
                    background: #ffffff;
                    border: 2px solid #1b683b;
                    border-radius: 50%;
                    width: 14px;
                    height: 14px;
                    margin-left: -7px;
                    margin-top: -7px;
                }
                .zone-label {
                    background: rgba(255, 255, 255, 0.92);
                    border: 1px solid #1b683b;
                    border-radius: 8px;
                    color: #1b261e;
                    font-weight: bold;
                    font-size: 11px;
                    padding: 2px 6px;
                    box-shadow: 0 1px 4px rgba(0,0,0,0.25);
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map', {
                    zoomControl: false,
                    attributionControl: true
                }).setView([41.9028, 12.4964], 6);

                // OpenStreetMap Tile Layer (Complies with OSM Tile Policy and attribution)
                var streetsLayer = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    maxZoom: 19,
                    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                });

                // Satellite Tile Layer (Free hybrid imagery)
                var satelliteLayer = L.tileLayer('https://mt1.google.com/vt/lyrs=y&x={x}&y={y}&z={z}', {
                    maxZoom: 20,
                    attribution: '&copy; Google'
                }).addTo(map);

                var currentBaseLayer = satelliteLayer;

                function switchBaseLayer(name) {
                    if (name === 'streets') {
                        if (map.hasLayer(satelliteLayer)) map.removeLayer(satelliteLayer);
                        if (!map.hasLayer(streetsLayer)) streetsLayer.addTo(map);
                        currentBaseLayer = streetsLayer;
                    } else {
                        if (map.hasLayer(streetsLayer)) map.removeLayer(streetsLayer);
                        if (!map.hasLayer(satelliteLayer)) satelliteLayer.addTo(map);
                        currentBaseLayer = satelliteLayer;
                    }
                }

                function invalidateMapSize() {
                    if (map) {
                        map.invalidateSize();
                    }
                }
                window.addEventListener('resize', invalidateMapSize);
                setTimeout(invalidateMapSize, 100);
                setTimeout(invalidateMapSize, 300);
                setTimeout(invalidateMapSize, 800);
                setTimeout(invalidateMapSize, 1500);

                var propertyLayer = L.layerGroup().addTo(map);
                var zonesLayer = L.layerGroup().addTo(map);
                var userMarkerLayer = L.layerGroup().addTo(map);

                // Drawing engine
                var isDrawing = false;
                var drawMode = 'property';
                var drawnPoints = [];
                var drawnMarkers = [];
                var drawnPolyline = null;
                var drawnPolygon = null;

                function calculateGeodesicArea(coords) {
                    if (coords.length < 3) return 0;
                    var rad = 6378137.0;
                    var total = 0;
                    for (var i = 0; i < coords.length; i++) {
                        var p1 = coords[i];
                        var p2 = coords[(i + 1) % coords.length];
                        var lat1 = p1.lat * Math.PI / 180.0;
                        var lat2 = p2.lat * Math.PI / 180.0;
                        var lon1 = p1.lng * Math.PI / 180.0;
                        var lon2 = p2.lng * Math.PI / 180.0;
                        total += (lon2 - lon1) * (2.0 + Math.sin(lat1) + Math.sin(lat2));
                    }
                    return Math.abs(total * rad * rad / 2.0);
                }

                function notifyBridge() {
                    var areaM2 = calculateGeodesicArea(drawnPoints);
                    var areaHa = areaM2 / 10000.0;
                    if (window.AndroidBridge) {
                        window.AndroidBridge.updateDrawingPoints(JSON.stringify(drawnPoints), areaM2, areaHa);
                    }
                }

                function updateDrawingVisuals() {
                    if (drawnPolyline) map.removeLayer(drawnPolyline);
                    if (drawnPolygon) map.removeLayer(drawnPolygon);

                    if (drawnPoints.length >= 3) {
                        drawnPolygon = L.polygon(drawnPoints, {
                            color: drawMode === 'property' ? '#f5c71a' : '#2e7d32',
                            weight: 3,
                            fillOpacity: 0.35
                        }).addTo(map);
                    } else if (drawnPoints.length >= 2) {
                        drawnPolyline = L.polyline(drawnPoints, {
                            color: drawMode === 'property' ? '#f5c71a' : '#2e7d32',
                            weight: 3,
                            dashArray: '5, 5'
                        }).addTo(map);
                    }
                    notifyBridge();
                }

                function rebuildMarkers() {
                    drawnMarkers.forEach(function(m) { map.removeLayer(m); });
                    drawnMarkers = [];

                    drawnPoints.forEach(function(pt, idx) {
                        var marker = L.marker([pt.lat, pt.lng], {
                            draggable: true,
                            icon: L.divIcon({
                                className: 'vertex-marker',
                                iconSize: [14, 14],
                                iconAnchor: [7, 7]
                            })
                        }).addTo(map);

                        marker.on('drag', function(e) {
                            var pos = e.target.getLatLng();
                            drawnPoints[idx] = { lat: pos.lat, lng: pos.lng };
                            if (drawnPolyline && drawnPoints.length < 3) {
                                drawnPolyline.setLatLngs(drawnPoints);
                            }
                            if (drawnPolygon) {
                                drawnPolygon.setLatLngs(drawnPoints);
                            }
                            notifyBridge();
                        });

                        marker.on('dragend', function(e) {
                            var pos = e.target.getLatLng();
                            drawnPoints[idx] = { lat: pos.lat, lng: pos.lng };
                            updateDrawingVisuals();
                        });

                        marker.on('click', function(me) {
                            L.DomEvent.stopPropagation(me);
                            removePointAt(idx);
                        });

                        drawnMarkers.push(marker);
                    });
                }

                map.on('click', function(e) {
                    if (!isDrawing) return;
                    var pt = { lat: e.latlng.lat, lng: e.latlng.lng };
                    drawnPoints.push(pt);
                    rebuildMarkers();
                    updateDrawingVisuals();
                });

                function removePointAt(idx) {
                    if (idx >= 0 && idx < drawnPoints.length) {
                        drawnPoints.splice(idx, 1);
                        rebuildMarkers();
                        updateDrawingVisuals();
                    }
                }

                function undoLastPoint() {
                    if (drawnPoints.length > 0) {
                        drawnPoints.pop();
                        rebuildMarkers();
                        updateDrawingVisuals();
                    }
                }

                function startDrawing(mode) {
                    clearDrawing();
                    isDrawing = true;
                    drawMode = mode;
                }

                function cancelDrawing() {
                    clearDrawing();
                    isDrawing = false;
                }

                function finishDrawing() {
                    clearDrawing();
                    isDrawing = false;
                }

                function clearDrawing() {
                    drawnMarkers.forEach(function(m) { map.removeLayer(m); });
                    drawnMarkers = [];
                    drawnPoints = [];
                    if (drawnPolyline) map.removeLayer(drawnPolyline);
                    if (drawnPolygon) map.removeLayer(drawnPolygon);
                    drawnPolyline = null;
                    drawnPolygon = null;
                }

                function loadExistingGeometryBase64(propB64, zonesB64) {
                    try {
                        var propJson = decodeURIComponent(escape(atob(propB64)));
                        var zonesJson = decodeURIComponent(escape(atob(zonesB64)));
                        loadExistingGeometry(propJson, zonesJson);
                    } catch(e) {
                        console.error("Base64 geometry decoding error", e);
                    }
                }

                function loadExistingGeometry(propertyJson, zonesJson) {
                    try {
                        propertyLayer.clearLayers();
                        zonesLayer.clearLayers();

                        var prop = (typeof propertyJson === 'string') ? JSON.parse(propertyJson || '[]') : (propertyJson || []);
                        var hasProp = false;
                        if (prop && prop.length >= 3) {
                            var latlngs = prop.map(function(p) { return [p.lat, p.lng]; });
                            var pPoly = L.polygon(latlngs, {
                                color: '#e69500',
                                weight: 3,
                                dashArray: '6, 6',
                                fillOpacity: 0.12
                            }).addTo(propertyLayer);
                            hasProp = true;
                            if (!isDrawing) {
                                map.fitBounds(pPoly.getBounds(), { padding: [40, 40] });
                            }
                        }

                        var zones = (typeof zonesJson === 'string') ? JSON.parse(zonesJson || '[]') : (zonesJson || []);
                        var zoneBounds = [];
                        zones.forEach(function(z) {
                            if (z.points && z.points.length >= 3) {
                                var zPts = z.points.map(function(p) { return [p.lat, p.lng]; });
                                var zPoly = L.polygon(zPts, {
                                    color: z.colorHex || '#2e7d32',
                                    weight: 2.5,
                                    fillOpacity: 0.4
                                }).addTo(zonesLayer);

                                zPts.forEach(function(pt) { zoneBounds.push(pt); });

                                zPoly.bindTooltip(z.name, { permanent: true, direction: 'center', className: 'zone-label' });
                                zPoly.on('click', function(e) {
                                    L.DomEvent.stopPropagation(e);
                                    if (window.AndroidBridge) window.AndroidBridge.selectZone(z.id);
                                });
                            }
                        });

                        if (!hasProp && zoneBounds.length >= 3 && !isDrawing) {
                            map.fitBounds(L.latLngBounds(zoneBounds), { padding: [40, 40] });
                        }
                    } catch (err) {
                        console.error("loadExistingGeometry error", err);
                    }
                }

                function centerOnCoords(lat, lng, acc) {
                    userMarkerLayer.clearLayers();
                    L.circle([lat, lng], { radius: acc || 20, color: '#1E88E5', fillOpacity: 0.15 }).addTo(userMarkerLayer);
                    L.circleMarker([lat, lng], { radius: 8, color: '#ffffff', fillColor: '#1E88E5', fillOpacity: 1, weight: 2 }).addTo(userMarkerLayer);
                    map.setView([lat, lng], 17);
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}
