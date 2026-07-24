package com.coppersmith.music1chat

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.coppersmith.music1chat.ui.screens.MainScreen
import com.coppersmith.music1chat.ui.theme.Music1ChatTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Music1ChatTheme {
                MainScreen()
            }
        }
    }
}
