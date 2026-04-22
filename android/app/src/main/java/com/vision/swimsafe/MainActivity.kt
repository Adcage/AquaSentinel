package com.vision.swimsafe

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.amap.api.location.AMapLocationClient
import com.amap.api.maps.MapsInitializer
import com.vision.swimsafe.config.AppConfig
import com.vision.swimsafe.data.remote.AuthSession
import com.vision.swimsafe.data.alert.RealtimeAlertNotifier
import com.vision.swimsafe.ui.navigation.SwimSafeApp
import com.vision.swimsafe.ui.theme.AndroidTheme
import com.vision.swimsafe.ui.theme.PageBackground

class MainActivity : ComponentActivity() {
    private companion object {
        const val REQUEST_CODE_POST_NOTIFICATIONS = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapsInitializer.updatePrivacyShow(this, true, true)
        MapsInitializer.updatePrivacyAgree(this, true)
        AMapLocationClient.updatePrivacyShow(this, true, true)
        AMapLocationClient.updatePrivacyAgree(this, true)
        AppConfig.init(this)
        // 调试输出 Key 和包名
        android.util.Log.d("MainActivity", "Package: ${packageName}")
        android.util.Log.d("MainActivity", "AMap Key: ${AppConfig.getAMapKey()}")
        AuthSession.init(this)
        RealtimeAlertNotifier.initialize(this)
        if (AuthSession.isLoggedIn()) {
            RealtimeAlertNotifier.connectIfNeeded()
        }
        requestNotificationPermissionIfNeeded()
        enableEdgeToEdge()
        setContent {
            AndroidTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = PageBackground) {
                    SwimSafeApp()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (AuthSession.isLoggedIn()) {
            RealtimeAlertNotifier.connectIfNeeded()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            return
        }
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_CODE_POST_NOTIFICATIONS,
            )
        }
    }
}
