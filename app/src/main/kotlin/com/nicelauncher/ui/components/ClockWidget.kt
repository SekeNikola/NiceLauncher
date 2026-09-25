package com.nicelauncher.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun ClockWidget(
    isDarkMode: Boolean,
    modifier: Modifier = Modifier,
) {
    var hour by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    var amPm by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance()
            val h = now.get(Calendar.HOUR).let { if (it == 0) 12 else it }
            hour = h.toString()
            minutes = "%02d".format(now.get(Calendar.MINUTE))
            amPm = if (now.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
            delay(1_000L)
        }
    }

    val primary = if (isDarkMode) Color.White else Color.Black
    val secondary = primary.copy(alpha = 0.5f)

    Column(modifier = modifier) {
        // Hour:Minutes or Hour:AM_PM (matching 9:PM style from mockup when minutes are 00)
        androidx.compose.material3.Text(
            text = if (minutes == "00" || minutes.isEmpty()) "$hour:$amPm" else "$hour:$minutes",
            fontSize = 20.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            maxLines = 1,
            softWrap = false,
            color = primary,
        )
        if (minutes != "00" && minutes.isNotEmpty()) {
            androidx.compose.material3.Text(
                text = amPm,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                color = secondary,
            )
        }
    }
}
