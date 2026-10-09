package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FarmCardBorder
import com.example.ui.theme.FarmGreenDark
import com.example.ui.theme.FarmSurface
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun FarmDateSelector(
    selectedDateStr: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Data"
) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }

    val todayStr = remember {
        val cal = Calendar.getInstance()
        String.format("%02d/%02d/%04d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
    }

    val tomorrowStr = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 1)
        String.format("%02d/%02d/%04d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
    }

    val isToday = (selectedDateStr == todayStr)
    val isTomorrow = (selectedDateStr == tomorrowStr)
    val isCustom = (!isToday && !isTomorrow && selectedDateStr.isNotBlank())

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Oggi chip
            FilterChip(
                selected = isToday,
                onClick = { onDateSelected(todayStr) },
                label = { Text("Oggi", fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FarmGreenDark,
                    selectedLabelColor = Color.White
                )
            )

            // Domani chip
            FilterChip(
                selected = isTomorrow,
                onClick = { onDateSelected(tomorrowStr) },
                label = { Text("Domani", fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FarmGreenDark,
                    selectedLabelColor = Color.White
                )
            )

            // Seleziona data chip / button
            Surface(
                onClick = {
                    val dialog = DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val chosen = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                            onDateSelected(chosen)
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    )
                    dialog.show()
                },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isCustom) FarmGreenDark else FarmCardBorder),
                color = if (isCustom) FarmGreenDark else FarmSurface,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Calendario",
                        tint = if (isCustom) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCustom) selectedDateStr else "Seleziona data",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCustom) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun FarmTimeSelector(
    selectedTimeStr: String,
    onTimeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Ora"
) {
    val context = LocalContext.current
    val parts = selectedTimeStr.split(":")
    val initialHour = if (parts.isNotEmpty()) parts[0].toIntOrNull() ?: 8 else 8
    val initialMinute = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("07:00", "08:00", "12:00", "18:00").forEach { preset ->
                val isSelected = (selectedTimeStr == preset)
                FilterChip(
                    selected = isSelected,
                    onClick = { onTimeSelected(preset) },
                    label = { Text(preset, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FarmGreenDark,
                        selectedLabelColor = Color.White
                    )
                )
            }

            // Custom Time Picker
            val isCustom = selectedTimeStr !in listOf("07:00", "08:00", "12:00", "18:00") && selectedTimeStr.isNotBlank()
            Surface(
                onClick = {
                    val dialog = TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            val chosen = String.format("%02d:%02d", hourOfDay, minute)
                            onTimeSelected(chosen)
                        },
                        initialHour,
                        initialMinute,
                        true
                    )
                    dialog.show()
                },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isCustom) FarmGreenDark else FarmCardBorder),
                color = if (isCustom) FarmGreenDark else FarmSurface,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Ora",
                        tint = if (isCustom) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCustom) selectedTimeStr else "Altro",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCustom) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

