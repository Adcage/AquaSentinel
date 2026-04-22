package com.vision.swimsafe.ui.screens.location

import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.vision.swimsafe.config.AppConfig
import com.vision.swimsafe.data.remote.RemoteLocationRepository
import com.vision.swimsafe.data.remote.RemoteMapper
import com.vision.swimsafe.ui.components.common.SecondaryActionButton
import com.vision.swimsafe.ui.model.LocationUiState
import com.vision.swimsafe.ui.components.common.AlertBanner
import com.vision.swimsafe.ui.components.common.AppTopBar
import com.vision.swimsafe.ui.components.common.StatusChip
import com.vision.swimsafe.ui.components.map.AMapView
import com.vision.swimsafe.ui.theme.AndroidTheme
import com.vision.swimsafe.ui.theme.AppDimens
import com.vision.swimsafe.ui.theme.CardBackground
import com.vision.swimsafe.ui.theme.DividerColor
import com.vision.swimsafe.ui.theme.PageBackground
import com.vision.swimsafe.ui.theme.SafetyBlue
import com.vision.swimsafe.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun LocationScreen(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var refreshVersion by remember { mutableStateOf(0) }
    var reporting by remember { mutableStateOf(false) }
    var reportResult by remember { mutableStateOf<String?>(null) }
    var realtimeLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var hasLocationPermission by remember { mutableStateOf(hasLocationPermission(context)) }
    var locationRefreshing by remember { mutableStateOf(false) }
    var locationRefreshVersion by remember { mutableStateOf(0) }
    var locationHint by remember { mutableStateOf<String?>(null) }
    var locationErrorDetail by remember { mutableStateOf<String?>(null) }
    var showRefreshHint by remember { mutableStateOf(false) }

    val state by produceState(
        initialValue = LocationUiState(
            reportStatus = "加载中",
            lastReportTime = "--",
            signalStrength = "--",
            coordinateText = "--",
            outOfFence = false,
            records = emptyList(),
        ),
        key1 = refreshVersion,
    ) {
        value = RemoteLocationRepository.getLocationUiState()
    }

    // 解析后端坐标：优先使用 state.coordinateText，如果解析失败则从 records 列表中获取
    val backendLocation = remember(state.coordinateText, state.records) {
        var parsed = RemoteMapper.parseCoordinateText(state.coordinateText)
        android.util.Log.d("LocationScreen", "Backend coordinateText='${state.coordinateText}', parsed=$parsed")
        // 如果 coordinateText 解析失败，尝试从 records 列表中获取第一个有效坐标
        if (parsed == null && state.records.isNotEmpty()) {
            val firstRecord = state.records.first()
            val recordText = firstRecord.coordinateText.trim()
            android.util.Log.d("LocationScreen", "Trying to parse from first record: '$recordText'")
            parsed = RemoteMapper.parseCoordinateText(recordText)
            android.util.Log.d("LocationScreen", "Parsed from record: $parsed")
        }
        parsed
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasLocationPermission = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            hasLocationPermission(context)
        if (hasLocationPermission) {
            locationRefreshVersion += 1
        } else {
            locationHint = "未授予定位权限，当前使用后台上报坐标"
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
        }
    }

    LaunchedEffect(locationHint) {
        if (!locationHint.isNullOrBlank()) {
            Toast.makeText(context, locationHint, Toast.LENGTH_SHORT).show()
            locationHint = null
        }
    }

    // 使用高德原生定位获取实时位置（优先于后端最近上报）
    DisposableEffect(hasLocationPermission, locationRefreshVersion) {
        if (!hasLocationPermission) {
            android.util.Log.w("LocationScreen", "Location permission not granted, fallback to backend location")
            onDispose { }
        } else {
            locationRefreshing = true
            locationErrorDetail = null
            var attemptCount = 0
            var active = true
            val client = AMapLocationClient(context)
            val option = AMapLocationClientOption().apply {
                locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                isOnceLocation = false
                interval = 2000
                isLocationCacheEnable = false
                isNeedAddress = false
                httpTimeOut = 10000
            }
            client.setLocationOption(option)
            client.setLocationListener { location ->
                if (!active) {
                    return@setLocationListener
                }
                attemptCount += 1
                if (location != null && location.errorCode == 0) {
                    val lat = location.latitude
                    val lng = location.longitude
                    // 检查坐标是否在中国范围内（模拟器可能返回 WGS84 坐标如 Google 总部 37.42, -122.08）
                    if (!isLocationInChina(lat, lng)) {
                        locationRefreshing = false
                        locationErrorDetail = "检测到模拟器环境，定位坐标不在中国境内($lat, $lng)"
                        locationHint = "模拟器无法获取真实位置，使用后台坐标"
                        android.util.Log.w(
                            "LocationScreen",
                            "Emulator location detected: ($lat, $lng), fallback to backend"
                        )
                        active = false
                        client.stopLocation()
                        return@setLocationListener
                    }
                    locationRefreshing = false
                    realtimeLocation = lat to lng
                    if (showRefreshHint) {
                        locationHint = "实时定位已刷新"
                    }
                    showRefreshHint = false
                    locationErrorDetail = null
                    android.util.Log.d("LocationScreen", "Realtime location: $realtimeLocation")
                    active = false
                    client.stopLocation()
                } else {
                    val code = location?.errorCode ?: -1
                    val info = location?.errorInfo ?: "unknown"
                    val conciseInfo = info.take(36)
                    locationErrorDetail = "定位失败(code=$code): $conciseInfo"
                    android.util.Log.e(
                        "LocationScreen",
                        "Realtime location failed attempt=$attemptCount, code=$code, info=$info"
                    )
                    if (attemptCount >= 3) {
                        locationRefreshing = false
                        locationHint = "实时定位失败，使用后台坐标"
                        active = false
                        client.stopLocation()
                    } else {
                        locationHint = "定位重试中($attemptCount/3)..."
                    }
                }
            }
            client.startLocation()

            onDispose {
                active = false
                locationRefreshing = false
                client.stopLocation()
                client.onDestroy()
            }
        }
    }

    val currentLocation = realtimeLocation ?: backendLocation
    val locationText = currentLocation?.let { "${it.first}, ${it.second}" } ?: state.coordinateText
    val locationSourceText = if (realtimeLocation != null) "实时定位" else "后台坐标"

    Column(modifier = modifier.fillMaxSize().background(PageBackground)) {
        AppTopBar(title = "定位", containerColor = SafetyBlue, titleColor = CardBackground)
        if (state.outOfFence) {
            AlertBanner(text = "已超出在岗区域，请立即返回")
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = AppDimens.spacingMd),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spacingMd),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = AppDimens.spacingMd),
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(AppDimens.cardRadius)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppDimens.spacingLg),
                        verticalArrangement = Arrangement.spacedBy(AppDimens.spacingSm)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(text = "定位状态", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text(text = state.reportStatus, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "最近上报：${state.lastReportTime}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                                StatusChip(text = state.signalStrength)
                                Text(
                                    text = locationText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = locationSourceText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        SecondaryActionButton(
                            text = if (reporting) "上报中..." else "立即上报定位",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (reporting) {
                                    return@SecondaryActionButton
                                }
                                val fallback = 31.230416 to 121.473701
                                val (latitude, longitude) = currentLocation ?: fallback
                                reporting = true
                                reportResult = null
                                scope.launch {
                                    val result = RemoteLocationRepository.reportCurrentLocation(
                                        latitude = latitude,
                                        longitude = longitude,
                                    )
                                    reporting = false
                                    if (result.isSuccess) {
                                        reportResult = "定位上报成功"
                                        refreshVersion += 1
                                    } else {
                                        reportResult = result.exceptionOrNull()?.message ?: "定位上报失败"
                                    }
                                }
                            },
                        )
                        SecondaryActionButton(
                            text = if (locationRefreshing) "定位刷新中..." else "刷新实时定位",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (!locationRefreshing) {
                                    if (hasLocationPermission) {
                                        showRefreshHint = true
                                        locationRefreshVersion += 1
                                    } else {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                            )
                                        )
                                    }
                                }
                            },
                        )
                        if (reportResult != null) {
                            Text(
                                text = reportResult.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (reportResult == "定位上报成功") SafetyBlue else MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = AppDimens.spacingSm),
                            )
                        }
                        if (locationErrorDetail != null) {
                            Text(
                                text = locationErrorDetail.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(AppDimens.cardRadius))
                        .background(CardBackground)
                        .border(1.dp, DividerColor, RoundedCornerShape(AppDimens.cardRadius)),
                ) {
                    AMapView(
                        modifier = Modifier.fillMaxSize(),
                        currentLocation = currentLocation,
                        fences = emptyList(), // TODO: 后续可从服务端加载电子围栏数据
                        amapKey = AppConfig.getAMapKey(),
                    )
                }
            }
            item {
                Card(shape = RoundedCornerShape(AppDimens.cardRadius)) {
                    Column(modifier = Modifier.padding(AppDimens.spacingLg)) {
                        Text(text = "最近定位上报", style = MaterialTheme.typography.titleMedium)
                        state.records.forEach { record ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = AppDimens.spacingSm),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text(text = record.time, style = MaterialTheme.typography.bodyLarge)
                                    Text(text = record.coordinateText, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                StatusChip(text = record.status)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationScreenPreview() {
    AndroidTheme {
        LocationScreen()
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    val hasFine = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PermissionChecker.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PermissionChecker.PERMISSION_GRANTED
    return hasFine || hasCoarse
}

/**
 * 检查坐标是否在中国范围内
 * 用于检测模拟器环境（模拟器通常返回 WGS84 坐标，如 Google 总部 37.42, -122.08）
 * 中国大致范围：纬度 18-54°N，经度 73-135°E
 */
private fun isLocationInChina(latitude: Double, longitude: Double): Boolean {
    return latitude in 18.0..54.0 && longitude in 73.0..135.0
}
