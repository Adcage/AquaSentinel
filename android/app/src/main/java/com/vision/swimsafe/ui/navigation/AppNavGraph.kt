package com.vision.swimsafe.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vision.swimsafe.data.remote.AuthSession
import com.vision.swimsafe.ui.components.app.AppBottomBar
import com.vision.swimsafe.ui.screens.alarm.AlarmCenterScreen
import com.vision.swimsafe.ui.screens.alarm.AlarmDetailScreen
import com.vision.swimsafe.ui.screens.home.HomeScreen
import com.vision.swimsafe.ui.screens.location.LocationScreen
import com.vision.swimsafe.ui.screens.login.LoginScreen
import com.vision.swimsafe.ui.screens.profile.ProfileScreen
import com.vision.swimsafe.ui.screens.record.RecordScreen
import com.vision.swimsafe.ui.screens.video.VideoListScreen

@Composable
fun SwimSafeApp() {
    val navController = rememberNavController()
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        startDestination = if (AuthSession.isLoggedIn()) AppRoutes.HOME else AppRoutes.LOGIN
    }

    if (startDestination == null) return

    NavHost(
        navController = navController,
        startDestination = startDestination!!,
    ) {
        composable(AppRoutes.LOGIN) {
            LoginScreen(onLoginClick = {
                navController.navigate(AppRoutes.HOME) {
                    popUpTo(AppRoutes.LOGIN) { inclusive = true }
                }
            })
        }

        mainTabs().forEach { tab ->
            composable(tab.route) {
                MainTabScaffold(
                    currentRoute = tab.route,
                    onTabSelected = { selectedTab ->
                        if (selectedTab.route != tab.route) {
                            navController.navigate(selectedTab.route) {
                                popUpTo(AppRoutes.HOME)
                                launchSingleTop = true
                            }
                        }
                    },
                ) { modifier ->
                    when (tab) {
                        MainTab.HOME -> HomeScreen(
                            modifier = modifier,
                            onOpenAlarmDetail = { alarmId ->
                                navController.navigate("${AppRoutes.ALARM_DETAIL}/$alarmId")
                            },
                            onNavigateToVideoList = {
                                navController.navigate(AppRoutes.VIDEO_LIST)
                            },
                            onNavigateToAlarmList = {
                                navController.navigate(AppRoutes.ALARM_CENTER) {
                                    popUpTo(AppRoutes.HOME)
                                    launchSingleTop = true
                                }
                            },
                        )

                        MainTab.ALARM -> AlarmCenterScreen(
                            modifier = modifier,
                            onOpenAlarmDetail = { alarmId ->
                                navController.navigate("${AppRoutes.ALARM_DETAIL}/$alarmId")
                            },
                        )

                        MainTab.LOCATION -> LocationScreen(modifier = modifier)
                        MainTab.RECORD -> RecordScreen(
                            modifier = modifier,
                            onOpenAlarmDetail = { alarmId ->
                                navController.navigate("${AppRoutes.ALARM_DETAIL}/$alarmId")
                            },
                        )

                        MainTab.PROFILE -> ProfileScreen(
                            modifier = modifier,
                            onLogout = {
                                navController.navigate(AppRoutes.LOGIN) {
                                    popUpTo(AppRoutes.LOGIN) { inclusive = true }
                                }
                            },
                        )
                    }
                }
            }
        }

        composable(
            route = "${AppRoutes.ALARM_DETAIL}/{alarmId}",
            arguments = listOf(navArgument("alarmId") { type = NavType.StringType }),
        ) { backStackEntry ->
            AlarmDetailScreen(
                alarmId = backStackEntry.arguments?.getString("alarmId").orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }

        composable(AppRoutes.VIDEO_LIST) {
            VideoListScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun MainTabScaffold(
    currentRoute: String,
    onTabSelected: (MainTab) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        bottomBar = {
            AppBottomBar(
                tabs = mainTabs(),
                currentRoute = currentRoute,
                onTabSelected = onTabSelected,
            )
        },
    ) { innerPadding ->
        content(Modifier.padding(innerPadding))
    }
}
