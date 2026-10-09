package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

enum class MainNavTab {
    HOME, MAPPA, DIARIO, PRODOTTI, ALTRO
}

@Composable
fun FarmBottomNavBar(
    selectedTab: MainNavTab,
    onTabSelected: (MainNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        border = BorderStroke(1.dp, Color(0xFFE2DFD6)),
        color = FarmSurface,
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                label = "Home",
                icon = Icons.Default.Home,
                isSelected = selectedTab == MainNavTab.HOME,
                onClick = { onTabSelected(MainNavTab.HOME) },
                testTag = "nav_home"
            )

            NavItem(
                label = "Mappa",
                icon = Icons.Default.Map,
                isSelected = selectedTab == MainNavTab.MAPPA,
                onClick = { onTabSelected(MainNavTab.MAPPA) },
                testTag = "nav_mappa"
            )

            NavItem(
                label = "Diario",
                icon = Icons.Default.Mic,
                isSelected = selectedTab == MainNavTab.DIARIO,
                onClick = { onTabSelected(MainNavTab.DIARIO) },
                testTag = "nav_diario"
            )

            NavItem(
                label = "Prodotti",
                icon = Icons.Default.Inventory2,
                isSelected = selectedTab == MainNavTab.PRODOTTI,
                onClick = { onTabSelected(MainNavTab.PRODOTTI) },
                testTag = "nav_prodotti"
            )

            NavItem(
                label = "Altro",
                icon = Icons.Default.MoreHoriz,
                isSelected = selectedTab == MainNavTab.ALTRO,
                onClick = { onTabSelected(MainNavTab.ALTRO) },
                testTag = "nav_altro"
            )
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) FarmGreenDark else FarmTextSecondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) FarmGreenDark else FarmTextSecondary
        )
    }
}
