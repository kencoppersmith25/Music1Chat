package com.coppersmith.music1chat.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay


@Composable
fun NavigationIndicator(
    included: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabledColor = Color(0xFF21A53A)
    val disabledColor = MaterialTheme.colorScheme.onSurface
    val routeColor by animateColorAsState(
        targetValue = if (included) enabledColor else disabledColor,
        animationSpec = tween(durationMillis = 180),
        label = "navigationRouteColor"
    )
    val redX = Color(0xFFE3262E)

    Canvas(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        val strokeWidth = 2.6.dp.toPx()
        val centerY = size.height * 0.62f

        val leftBaseX = size.width * 0.05f
        val leftTipX = size.width * 0.20f
        val leftHalfHeight = size.height * 0.12f

        val routeStartX = leftTipX + size.width * 0.03f
        val rightTipX = size.width * 0.94f
        val rightBaseX = size.width * 0.72f
        val rightHalfHeight = size.height * 0.19f

        val markCenterX = size.width * 0.52f
        val gapHalfWidth = size.width * 0.15f

        // Small starting arrow, pointing to the right.
        drawLine(
            color = routeColor,
            start = Offset(leftBaseX, centerY - leftHalfHeight),
            end = Offset(leftTipX, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = routeColor,
            start = Offset(leftBaseX, centerY + leftHalfHeight),
            end = Offset(leftTipX, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        if (included) {
            drawLine(
                color = routeColor,
                start = Offset(routeStartX, centerY),
                end = Offset(rightBaseX, centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        } else {
            drawLine(
                color = routeColor,
                start = Offset(routeStartX, centerY),
                end = Offset(markCenterX - gapHalfWidth, centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = routeColor,
                start = Offset(markCenterX + gapHalfWidth, centerY),
                end = Offset(rightBaseX, centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }

        // Larger destination arrow, also pointing to the right.
        drawLine(
            color = routeColor,
            start = Offset(rightBaseX, centerY),
            end = Offset(rightTipX, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = routeColor,
            start = Offset(rightTipX - size.width * 0.16f, centerY - rightHalfHeight),
            end = Offset(rightTipX, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = routeColor,
            start = Offset(rightTipX - size.width * 0.16f, centerY + rightHalfHeight),
            end = Offset(rightTipX, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        if (included) {
            val checkCenterX = markCenterX
            val checkCenterY = size.height * 0.24f
            drawLine(
                color = enabledColor,
                start = Offset(checkCenterX - size.width * 0.09f, checkCenterY),
                end = Offset(checkCenterX - size.width * 0.02f, checkCenterY + size.height * 0.08f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = enabledColor,
                start = Offset(checkCenterX - size.width * 0.02f, checkCenterY + size.height * 0.08f),
                end = Offset(checkCenterX + size.width * 0.11f, checkCenterY - size.height * 0.08f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        } else {
            val xHalfWidth = size.width * 0.11f
            val xHalfHeight = size.height * 0.14f
            drawLine(
                color = redX,
                start = Offset(markCenterX - xHalfWidth, centerY - xHalfHeight),
                end = Offset(markCenterX + xHalfWidth, centerY + xHalfHeight),
                strokeWidth = 3.2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = redX,
                start = Offset(markCenterX - xHalfWidth, centerY + xHalfHeight),
                end = Offset(markCenterX + xHalfWidth, centerY - xHalfHeight),
                strokeWidth = 3.2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}