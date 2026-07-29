package com.coppersmith.music1chat.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

@Composable
fun NavigationIndicator(
    included: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabledColor = Color(0xFF21A53A)
    val disabledColor = MaterialTheme.colorScheme.onSurface
    val redX = Color(0xFFE3262E)

    val routeColor by animateColorAsState(
        targetValue = if (included) enabledColor else disabledColor,
        animationSpec = tween(durationMillis = 180),
        label = "navigationRouteColor"
    )

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
            // Enabled navigation: one uninterrupted green route.
            drawLine(
                color = routeColor,
                start = Offset(routeStartX, centerY),
                end = Offset(rightBaseX, centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        } else {
            // Disabled navigation: leave a gap for the red X.
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
            start = Offset(
                rightTipX - size.width * 0.16f,
                centerY - rightHalfHeight
            ),
            end = Offset(rightTipX, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        drawLine(
            color = routeColor,
            start = Offset(
                rightTipX - size.width * 0.16f,
                centerY + rightHalfHeight
            ),
            end = Offset(rightTipX, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Only the disabled state needs an additional status mark.
        if (!included) {
            val xHalfWidth = size.width * 0.11f
            val xHalfHeight = size.height * 0.14f

            drawLine(
                color = redX,
                start = Offset(
                    markCenterX - xHalfWidth,
                    centerY - xHalfHeight
                ),
                end = Offset(
                    markCenterX + xHalfWidth,
                    centerY + xHalfHeight
                ),
                strokeWidth = 3.2.dp.toPx(),
                cap = StrokeCap.Round
            )

            drawLine(
                color = redX,
                start = Offset(
                    markCenterX - xHalfWidth,
                    centerY + xHalfHeight
                ),
                end = Offset(
                    markCenterX + xHalfWidth,
                    centerY - xHalfHeight
                ),
                strokeWidth = 3.2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}