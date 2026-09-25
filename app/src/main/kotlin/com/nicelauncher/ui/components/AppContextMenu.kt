package com.nicelauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nicelauncher.data.AppWithIcon
import com.nicelauncher.data.BackgroundMode

@Composable
fun AppContextMenu(
    app: AppWithIcon,
    onChangeImage: () -> Unit,
    onSetBackgroundMode: (BackgroundMode) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .width(280.dp)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column {
                Text(
                    text = app.app.label,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White,
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)

                // ── Background Style Section ─────────────────────────────
                Text(
                    text = "BACKGROUND",
                    modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 6.dp),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp,
                    color = Color.White.copy(alpha = 0.4f),
                )

                // Background mode options as a horizontal row of circles
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    BackgroundOption(
                        label = "Blur",
                        color = Color(0xFF6366F1),
                        isSelected = app.backgroundMode == BackgroundMode.BLURRED,
                        onClick = { onSetBackgroundMode(BackgroundMode.BLURRED) },
                        modifier = Modifier.weight(1f),
                    )
                    BackgroundOption(
                        label = "Black",
                        color = Color.Black,
                        isSelected = app.backgroundMode == BackgroundMode.BLACK,
                        onClick = { onSetBackgroundMode(BackgroundMode.BLACK) },
                        modifier = Modifier.weight(1f),
                    )
                    BackgroundOption(
                        label = "White",
                        color = Color.White,
                        isSelected = app.backgroundMode == BackgroundMode.WHITE,
                        onClick = { onSetBackgroundMode(BackgroundMode.WHITE) },
                        modifier = Modifier.weight(1f),
                    )
                    BackgroundOption(
                        label = "Photo",
                        color = Color(0xFFFF9500),
                        isSelected = app.backgroundMode == BackgroundMode.PHOTO,
                        onClick = onChangeImage,
                        modifier = Modifier.weight(1f),
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)

                MenuRow(
                    icon = Icons.Outlined.Delete,
                    label = "Remove from Home",
                    tint = Color(0xFFFF453A),
                    onClick = onRemove,
                )
            }
        }
    }
}

@Composable
private fun BackgroundOption(
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color)
                .then(
                    if (isSelected) Modifier.background(Color.Transparent) else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Selected",
                    tint = if (color == Color.White || color == Color(0xFFFF9500)) Color.Black else Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.size(4.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(text = label, color = tint, fontSize = 15.sp, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.weight(1f))
    }
}
