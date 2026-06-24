package com.ptniger.hris

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ptniger.hris.ui.navigation.AppNavigation
import com.ptniger.hris.ui.theme.HRISTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ptniger.hris.utils.NetworkMonitor
import com.ptniger.hris.ui.components.OfflineScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HRISTheme {
                val context = LocalContext.current
                val networkMonitor = remember { NetworkMonitor(context) }
                val isOnline by networkMonitor.isOnline.collectAsState(initial = true)

                if (isOnline) {
                    AppNavigation()
                } else {
                    OfflineScreen()
                }
            }
        }
    }
}