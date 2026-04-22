package com.vision.swimsafe.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

object AppRoutes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val ALARM_CENTER = "alarm_center"
    const val LOCATION = "location"
    const val RECORD = "record"
    const val PROFILE = "profile"
    const val ALARM_DETAIL = "alarm_detail"
    const val VIDEO_LIST = "video_list"
}

enum class MainTab(
    val route: String,
    val title: String,
    val icon: ImageVector,
) {
    HOME(AppRoutes.HOME, "首页", Icons.Filled.Home),
    ALARM(AppRoutes.ALARM_CENTER, "报警", Icons.Filled.NotificationsActive),
    LOCATION(AppRoutes.LOCATION, "定位", Icons.Filled.LocationOn),
    RECORD(AppRoutes.RECORD, "记录", Icons.AutoMirrored.Filled.ViewList),
    PROFILE(AppRoutes.PROFILE, "我的", Icons.Filled.Person),
}

fun mainTabs(): List<MainTab> = MainTab.entries.toList()
