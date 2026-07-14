package com.coppersmith.music1chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.coppersmith.music1chat.ui.screens.MainScreen
import com.coppersmith.music1chat.ui.theme.Music1ChatTheme
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Music1ChatTheme {
                MainScreen()
            }
        }
    }
}