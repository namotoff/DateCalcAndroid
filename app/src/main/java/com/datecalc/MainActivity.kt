// Copyright (c) 2024-2026 Bios Tlt. Licensed under Apache License 2.0.
//

package com.datecalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.datecalc.ads.AdManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        AdManager.initialize(this)
        setContent {
            AppScreen()
        }
    }
}
