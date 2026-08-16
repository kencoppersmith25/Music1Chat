package com.coppersmith.music1chat.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coppersmith.music1chat.R

/**
 * FINAL PLAY STORE FEATURE GRAPHIC (1024 x 500)
 * Proportioned for both phone-hacks and previewers.
 */
@Composable
fun FinalFeatureGraphic() {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // LEFT SIDE: Branding
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_icon),
                contentDescription = null,
                modifier = Modifier.size(150.dp) // Clean, centered size
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Hands Radio",
                color = Color.White,
                fontSize = 38.sp, // Safe size for one line
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.width(20.dp))

        // RIGHT SIDE: Feature Column
        Column(
            modifier = Modifier.weight(1f).padding(start = 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            FeatureRow(Icons.AutoMirrored.Filled.DirectionsBike, "Engineered for the Road")
            Spacer(modifier = Modifier.height(20.dp))
            FeatureRow(Icons.Default.Language, "World Radio")
            Spacer(modifier = Modifier.height(20.dp))
            FeatureRow(Icons.Default.Tv, "Stream to your TV")
        }
    }
}

@Composable
private fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(45.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(widthDp = 1024, heightDp = 500)
@Composable
fun FeatureGraphicPreview() {
    FinalFeatureGraphic()
}
