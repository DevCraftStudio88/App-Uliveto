package com.example.ui.components

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

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
    showDetailedForecast: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFFF9E6),
        border = BorderStroke(1.dp, Color(0xFFFFE082)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFFFEE58), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = "Meteo soleggiato",
                    tint = Color(0xFFF57F17),
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Oggi 22° e sereno",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = FarmTextPrimary
                )
                Text(
                    text = "Sabato piove: meglio fare i trattamenti domani",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = FarmTextSecondary
                )
            }
        }
    }
}
