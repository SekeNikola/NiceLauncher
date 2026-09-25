package com.nicelauncher.ui.components

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nicelauncher.data.AppWithIcon
import com.nicelauncher.data.BackgroundMode
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val CIRCLE_SIZE = 118.dp

// Percentages passed to RoundedCornerShape(percent=...)
// 50 = perfect circle | 26 = iOS-style squircle (superellipse feel)
private const val PCT_CIRCLE   = 50f
private const val PCT_SQUIRCLE = 34f

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppCircle(
    appWithIcon: AppWithIcon,
    unlockKey: Int,
    index: Int,
    circleSize: androidx.compose.ui.unit.Dp,
    isDarkMode: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val targetPct = PCT_SQUIRCLE
    val cornerPct = remember { Animatable(PCT_CIRCLE) }
    val scale = remember { Animatable(0.78f) }

    LaunchedEffect(unlockKey) {
        scale.snapTo(0.78f)
        cornerPct.snapTo(PCT_CIRCLE)          // instant reset to circle
        delay(index * 150L)                   // staggered reveal
        launch {
            scale.animateTo(
                targetValue = 1.14f,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                    stiffness = 70f,
                )
            )
            scale.animateTo(
                targetValue = 1f,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = 85f,
                )
            )
        }
        launch {
            cornerPct.animateTo(
                targetValue = targetPct,
                animationSpec = tween(
                    durationMillis = 2100,
                    easing         = FastOutSlowInEasing
                ),
            )
        }
    }

    val bitmap = remember(appWithIcon.app.icon) {
        appWithIcon.app.icon.toBitmapFull()
    }

    // Determine container background color based on mode
    val containerBg = when (appWithIcon.backgroundMode) {
        BackgroundMode.BLACK -> Color.Black
        BackgroundMode.WHITE -> Color.White
        BackgroundMode.BLURRED -> Color.Transparent // blurred icon drawn as image layer
        BackgroundMode.PHOTO -> Color.Transparent   // custom photo drawn as image layer
    }

    Box(
        modifier = modifier
            .size(circleSize * 1.18f),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(circleSize)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    clip = false
                }
                .clip(RoundedCornerShape(percent = cornerPct.value.toInt()))
                .background(containerBg)
                .combinedClickable(
                    onClick    = onTap,
                    onLongClick = onLongPress,
                ),
        ) {
            when (appWithIcon.backgroundMode) {
                BackgroundMode.PHOTO -> {
                    if (appWithIcon.customImageUri != null) {
                        AsyncImage(
                            model               = appWithIcon.customImageUri,
                            contentDescription  = appWithIcon.app.label,
                            contentScale        = ContentScale.Crop,
                            modifier            = Modifier.fillMaxSize(),
                        )
                    }
                }

                BackgroundMode.BLURRED -> {
                    Image(
                        painter            = BitmapPainter(bitmap.asImageBitmap()),
                        contentDescription = null,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier
                            .fillMaxSize()
                            .blur(24.dp),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = if (isDarkMode) 0.18f else 0.08f)),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(circleSize * 0.18f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter            = BitmapPainter(bitmap.asImageBitmap()),
                            contentDescription = appWithIcon.app.label,
                            contentScale       = ContentScale.Fit,
                            modifier           = Modifier.fillMaxSize(),
                        )
                    }
                }

                BackgroundMode.BLACK -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(circleSize * 0.18f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter            = BitmapPainter(bitmap.asImageBitmap()),
                            contentDescription = appWithIcon.app.label,
                            contentScale       = ContentScale.Fit,
                            modifier           = Modifier.fillMaxSize(),
                        )
                    }
                }

                BackgroundMode.WHITE -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(circleSize * 0.18f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter            = BitmapPainter(bitmap.asImageBitmap()),
                            contentDescription = appWithIcon.app.label,
                            contentScale       = ContentScale.Fit,
                            modifier           = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

private fun Drawable.toBitmapFull(size: Int = 192): Bitmap {
    val bmp    = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    if (this is BitmapDrawable && bitmap != null) {
        canvas.drawBitmap(bitmap, null, android.graphics.Rect(0, 0, size, size), null)
    } else {
        setBounds(0, 0, size, size)
        draw(canvas)
    }
    return bmp
}
