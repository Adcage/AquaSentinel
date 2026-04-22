package com.vision.swimsafe.ui.components.map

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.Polygon
import com.amap.api.maps.model.PolygonOptions
import kotlinx.coroutines.delay

data class FenceRegion(
    val id: String,
    val name: String,
    val points: List<Pair<Double, Double>>,
)

@Composable
fun AMapView(
    modifier: Modifier = Modifier,
    currentLocation: Pair<Double, Double>? = null,
    fences: List<FenceRegion> = emptyList(),
    amapKey: String = "",
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        MapView(context).apply {
            onCreate(Bundle())
        }
    }
    var map by remember { mutableStateOf<AMap?>(null) }
    var marker by remember { mutableStateOf<Marker?>(null) }
    var polygons by remember { mutableStateOf<List<Polygon>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var mapLoaded by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
        }
    }

    // 使用传入的坐标，如果没有则使用默认值（上海）
    val safeLocation = currentLocation ?: (31.230416 to 121.473701)
    val (lat, lng) = safeLocation
    val target = LatLng(lat, lng)

    LaunchedEffect(map, lat, lng, fences) {
        val aMap = map ?: return@LaunchedEffect

        if (marker == null) {
            marker = aMap.addMarker(
                MarkerOptions()
                    .position(target)
                    .title("当前位置")
            )
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(target, 16f))
        } else {
            marker?.position = target
            aMap.animateCamera(CameraUpdateFactory.newLatLng(target))
        }

        polygons.forEach { it.remove() }
        val nextPolygons = fences
            .filter { it.points.size >= 3 }
            .map { fence ->
                val latLngs = fence.points.map { (fenceLat, fenceLng) -> LatLng(fenceLat, fenceLng) }
                aMap.addPolygon(
                    PolygonOptions()
                        .addAll(latLngs)
                        .strokeWidth(3f)
                        .strokeColor(android.graphics.Color.parseColor("#1B4F9B"))
                        .fillColor(android.graphics.Color.parseColor("#261B4F9B"))
                )
            }
        polygons = nextPolygons
    }

    LaunchedEffect(map) {
        if (map == null) {
            return@LaunchedEffect
        }
        delay(6000)
        if (!mapLoaded && errorMessage == null) {
            errorMessage = "地图未加载成功，请检查高德Key绑定（包名/SHA1）"
        }
    }

    Box(modifier = modifier.background(Color(0xFFF5F7FA))) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                mapView
            },
            update = {
                if (map == null) {
                    map = it.map.apply {
                        uiSettings.isZoomControlsEnabled = false
                        uiSettings.isMyLocationButtonEnabled = false
                        setOnMapLoadedListener {
                            mapLoaded = true
                            errorMessage = null
                        }
                    }
                }
            }
        )

        if (amapKey.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCCF5F7FA)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "AMAP_KEY 未配置",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Red,
                )
            }
        }

        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCCF5F7FA)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = errorMessage ?: "地图加载失败",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Red,
                    )
                    Text(
                        text = "当前位置: ${lat.toFloat()}, ${lng.toFloat()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    if (amapKey.isNotBlank() && errorMessage?.contains("Key") == true) {
                        Text(
                            text = "提示: 请检查 local.properties 中的 AMAP_KEY 是否有效",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
