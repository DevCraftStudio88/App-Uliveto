package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WeatherService
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun FarmBackButton(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Indietro"
) {
    Surface(
        onClick = onBack,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, FarmCardBorder),
        color = FarmSurface,
        modifier = modifier
            .height(42.dp)
            .testTag("back_button")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = label,
                tint = FarmTextPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = FarmTextPrimary
            )
        }
    }
}

@Composable
fun FarmPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    backgroundColor: Color = FarmGreenDark,
    contentColor: Color = Color.White,
    testTag: String = "primary_button"
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun FarmOutlinedCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = FarmSurface,
    borderColor: Color = FarmCardBorder,
    borderWidth: Float = 1.5f,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier.clickable { onClick() }
    } else {
        modifier
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(borderWidth.dp, borderColor),
        color = backgroundColor,
        modifier = cardModifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun StatusBadge(
    text: String,
    color: Color,
    textColor: Color = FarmTextPrimary,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color,
        modifier = modifier
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun WeatherBanner(
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false
) {
    val weather by WeatherService.weatherState.collectAsState()
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        WeatherService.refreshWeather()
    }

    val primaryAdvice = weather.adviceList.firstOrNull { it.isAlert } ?: weather.adviceList.firstOrNull()

    val weatherIcon = when (weather.weatherCode) {
        0 -> Icons.Default.WbSunny
        1, 2 -> Icons.Default.WbCloudy
        3 -> Icons.Default.Cloud
        51, 53, 55, 61, 63, 65, 80, 81 -> Icons.Default.WaterDrop
        95, 96, 99 -> Icons.Default.Thunderstorm
        else -> Icons.Default.WbSunny
    }

    val iconBgColor = when {
        weather.weatherCode in listOf(51, 53, 55, 61, 63, 65, 80, 81, 95, 96, 99) -> Color(0xFFE1F5FE)
        weather.weatherCode in listOf(1, 2, 3) -> Color(0xFFECEFF1)
        else -> Color(0xFFFFEE58)
    }

    val iconTint = when {
        weather.weatherCode in listOf(51, 53, 55, 61, 63, 65, 80, 81, 95, 96, 99) -> Color(0xFF0288D1)
        weather.weatherCode in listOf(1, 2, 3) -> Color(0xFF546E7A)
        else -> Color(0xFFF57F17)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (primaryAdvice?.isAlert == true) Color(0xFFFFF8E1) else Color(0xFFF1F8E9),
        border = BorderStroke(1.5.dp, if (primaryAdvice?.isAlert == true) Color(0xFFFFD54F) else Color(0xFFC5E1A5)),
        modifier = modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(iconBgColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = weatherIcon,
                            contentDescription = weather.conditionTitle,
                            tint = iconTint,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${weather.temperature.toInt()}°C · ${weather.conditionTitle}",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = FarmTextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = primaryAdvice?.title ?: "Condizioni favorevoli per l'oliveto",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (primaryAdvice?.isAlert == true) Color(0xFFD84315) else FarmGreenDark,
                            maxLines = 1
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                WeatherService.refreshWeather(force = true)
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Aggiorna meteo",
                            tint = FarmTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Dettagli meteo",
                        tint = FarmTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Divider(color = FarmCardBorderSubtle, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Meteorological Telemetry Pill Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Opacity, contentDescription = null, tint = Color(0xFF0288D1), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Umidità: ${weather.humidity}%", fontSize = 12.sp, color = FarmTextPrimary)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Air, contentDescription = null, tint = Color(0xFF00897B), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Vento: ${weather.windSpeedKmH.toInt()} km/h", fontSize = 12.sp, color = FarmTextPrimary)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFF3949AB), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pioggia: ${weather.precipitationMm} mm", fontSize = 12.sp, color = FarmTextPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Agronomic Advice Box
                    weather.adviceList.forEach { adv ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (adv.isAlert) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                            border = BorderStroke(1.dp, if (adv.isAlert) Color(0xFFFFCDD2) else Color(0xFFC8E6C9)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = adv.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (adv.isAlert) Color(0xFFC62828) else FarmGreenDark
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = adv.description,
                                    fontSize = 12.sp,
                                    color = FarmTextPrimary,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Multi-day forecast preview
                    if (weather.dailyForecasts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Previsioni prossimi giorni:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = FarmTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            weather.dailyForecasts.take(4).forEach { day ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = FarmSurface,
                                    border = BorderStroke(1.dp, FarmCardBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(text = day.dayOfWeek, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${day.tempMax.toInt()}° / ${day.tempMin.toInt()}°",
                                            fontSize = 10.sp,
                                            color = FarmTextSecondary
                                        )
                                        if (day.precipitationProbability > 20) {
                                            Text(
                                                text = "${day.precipitationProbability}% 🌧️",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0288D1)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
