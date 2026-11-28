package com.example.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DigitalCounter(
    value: Int,
    modifier: Modifier = Modifier,
    testTag: String = "digital_counter"
) {
    val displayValue = when {
        value < -99 -> "-99"
        value < 0 -> String.format("-%02d", kotlin.math.abs(value))
        value > 999 -> "999"
        else -> String.format("%03d", value)
    }

    Box(
        modifier = modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF0F172A))
            .border(1.5.dp, Color(0xFF334155), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Dim background placeholder "888" for authentic 7-segment digital look
        Text(
            text = "888",
            color = Color(0x22EF4444),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
        Text(
            text = displayValue,
            color = Color(0xFFFF334B),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
    }
}
