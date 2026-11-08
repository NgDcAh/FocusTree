package com.focustree

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.focustree.FocusTreeApplication
import com.focustree.app.FocusTreeApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val app = application as FocusTreeApplication
            FocusTreeApp(app.sessions, app.preferences, app::setSyncEnabled)
        }
    }
}
