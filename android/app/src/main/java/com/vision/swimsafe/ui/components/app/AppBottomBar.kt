package com.vision.swimsafe.ui.components.app

import androidx.compose.foundation.layout.height
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vision.swimsafe.ui.navigation.MainTab
import com.vision.swimsafe.ui.theme.AppDimens
import com.vision.swimsafe.ui.theme.CardBackground
import com.vision.swimsafe.ui.theme.SafetyBlue
import com.vision.swimsafe.ui.theme.TextTertiary

@Composable
fun AppBottomBar(
    tabs: List<MainTab>,
    currentRoute: String,
    onTabSelected: (MainTab) -> Unit,
) {
    BottomAppBar(
        modifier = Modifier.height(AppDimens.bottomBarHeight + 16.dp),
        containerColor = CardBackground,
        tonalElevation = BottomAppBarDefaults.ContainerElevation,
    ) {
        tabs.forEach { tab ->
            val selected = tab.route == currentRoute
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab) },
                icon = {
                    BadgedBox(badge = {}) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = if (selected) SafetyBlue else TextTertiary,
                        )
                    }
                },
                label = {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) SafetyBlue else TextTertiary,
                    )
                },
            )
        }
    }
}
