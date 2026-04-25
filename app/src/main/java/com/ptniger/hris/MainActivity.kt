package com.ptniger.hris

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ptniger.hris.ui.navigation.AppNavigation
import com.ptniger.hris.ui.theme.HRISTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HRISTheme {
                AppNavigation()
            }
        }
    }
}