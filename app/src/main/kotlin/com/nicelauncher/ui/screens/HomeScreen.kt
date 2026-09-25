package com.nicelauncher.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.border
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.nicelauncher.data.AppItem
import com.nicelauncher.data.AppWithIcon
import com.nicelauncher.ui.components.AppCircle
import com.nicelauncher.ui.components.AppContextMenu
import com.nicelauncher.ui.components.AppDrawer
import com.nicelauncher.ui.components.ClockWidget
import com.nicelauncher.ui.components.PixelIcon
import com.nicelauncher.ui.theme.NiceLauncherTheme
import com.nicelauncher.viewmodel.LauncherViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(viewModel: LauncherViewModel) {
    val homeApps          by viewModel.homeApps.collectAsStateWithLifecycle()
    val allApps           by viewModel.allApps.collectAsStateWithLifecycle()
    val showDrawer        by viewModel.showDrawer.collectAsStateWithLifecycle()
    val isDarkMode        by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val noteText          by viewModel.noteText.collectAsStateWithLifecycle()
    val iconSizePercent   by viewModel.iconSizePercent.collectAsStateWithLifecycle()
    val userAvatarUri     by viewModel.userAvatarUri.collectAsStateWithLifecycle()
    val wallpaperUri      by viewModel.wallpaperUri.collectAsStateWithLifecycle()
    val userName          by viewModel.userName.collectAsStateWithLifecycle()
    val weather           by viewModel.weather.collectAsStateWithLifecycle()
    val clockAppPackage   by viewModel.clockAppPackage.collectAsStateWithLifecycle()
    val weatherAppPackage by viewModel.weatherAppPackage.collectAsStateWithLifecycle()

    // Dynamic circle size logic based on the explore slider percent (range: 80.dp to 140.dp)
    val circleSize = (80f + (iconSizePercent / 100f) * 60f).dp

    NiceLauncherTheme(darkTheme = isDarkMode) {
        val context = LocalContext.current
        var contextApp by remember { mutableStateOf<AppWithIcon?>(null) }
        var showNameDialog by remember { mutableStateOf(false) }
        var showWallpaperDialog by remember { mutableStateOf(false) }
        var showWeatherDialog by remember { mutableStateOf(false) }
        var showClockAppPicker by remember { mutableStateOf(false) }
        var showWeatherAppPicker by remember { mutableStateOf(false) }
        val listState = rememberLazyListState()
        val haptic = LocalHapticFeedback.current

        // Globe rotation speeds up while the list is scrolling. The animation itself
        // lives inside PixelIcon so it repaints only that canvas (see PixelIcon.kt).
        val isScrolling = listState.isScrollInProgress

        // ── Charging Flow Ring Animation ───────────────────────────────
        val infiniteTransition = rememberInfiniteTransition(label = "charging")
        val ringAngle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(2500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "ringAngle"
        )

        // ── Charging state ────────────────────────────────────────────────
        var isCharging by remember { mutableStateOf(false) }
        DisposableEffect(Unit) {
            fun readBattery(intent: Intent?) {
                val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
                          || status == BatteryManager.BATTERY_STATUS_FULL
            }
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            readBattery(context.registerReceiver(null, filter))
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) = readBattery(intent)
            }
            context.registerReceiver(receiver, filter)
            onDispose { context.unregisterReceiver(receiver) }
        }

        // ── Image picker for custom circle photo ──────────────────────────
        val imagePicker = rememberLauncherForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            uri?.let { picked ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        picked, Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                } catch (_: Exception) {}
                contextApp?.let { viewModel.updateCustomImage(it.app.packageName, picked) }
            }
            contextApp = null
        }

        // ── Image picker for user profile avatar ──────────────────────────
        val avatarPicker = rememberLauncherForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            uri?.let { picked ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        picked, Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                } catch (_: Exception) {}
                viewModel.updateUserAvatar(picked)
            }
        }

        // ── Image picker for wallpaper ─────────────────────────────────────
        val wallpaperPicker = rememberLauncherForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            uri?.let { picked ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        picked, Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                } catch (_: Exception) {}
                viewModel.updateWallpaper(picked)
            }
        }

        val primary = if (isDarkMode) Color.White else Color.Black

        Box(modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
        ) {
            // ── Wallpaper Image (if set) ──────────────────────────────────
            if (wallpaperUri != null) {
                AsyncImage(
                    model = wallpaperUri,
                    contentDescription = "Wallpaper",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    alpha = if (isDarkMode) 0.4f else 0.3f,
                )
            }

            // ── Background Canvas Dot Matrix ──────────────────────────────
            val dotColor = if (isDarkMode) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.07f)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val dotRadius = 1.2.dp.toPx()
                val spacing = 28.dp.toPx()
                val width = size.width
                val height = size.height
                var x = spacing / 2
                while (x < width) {
                    var y = spacing / 2
                    while (y < height) {
                        drawCircle(
                            color = dotColor,
                            radius = dotRadius,
                            center = androidx.compose.ui.geometry.Offset(x, y)
                        )
                        y += spacing
                    }
                    x += spacing
                }
            }

            LazyColumn(
                state                = listState,
                modifier             = Modifier.fillMaxSize(),
                horizontalAlignment  = Alignment.CenterHorizontally,
                contentPadding       = PaddingValues(bottom = 140.dp, top = 80.dp),
                verticalArrangement  = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(
                    homeApps,
                    key = { _, it -> it.app.packageName },
                ) { index, app ->
                    AppCircle(
                        appWithIcon = app,
                        index       = index,
                        circleSize  = circleSize,
                        isDarkMode  = isDarkMode,
                        onTap       = { viewModel.launchApp(app.app.packageName) },
                        onLongPress = { contextApp = app },
                        modifier    = Modifier.graphicsLayer {
                            val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { item -> item.key == app.app.packageName }
                            if (info != null) {
                                val viewportHeight = (listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset).toFloat()
                                if (viewportHeight > 0f) {
                                    val itemCenter = info.offset.toFloat() + info.size.toFloat() / 2f
                                    val listCenter = viewportHeight / 2f
                                    val distanceFromCenter = kotlin.math.abs(itemCenter - listCenter)
                                    val maxDistance = viewportHeight / 2f
                                    val fraction = (distanceFromCenter / maxDistance).coerceIn(0f, 1f)
                                    val computedScale = 1.15f - (fraction * 0.35f)
                                    scaleX = computedScale
                                    scaleY = computedScale
                                }
                            }
                        }
                    )
                }
            }

            // ── Date Header & Weather — top left ──────────────────────────
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 24.dp, top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DateHeader(isDarkMode = isDarkMode)
                PixelWeatherWidget(
                    isDarkMode = isDarkMode,
                    city = weather.city,
                    temperatureC = weather.temperatureC,
                    conditionCode = weather.conditionCode,
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            weatherAppPackage?.let { viewModel.launchApp(it) }
                                ?: run { showWeatherDialog = true }
                        },
                        onLongClick = { showWeatherDialog = true },
                    ),
                )
            }

            // ── Left Side Column (PixelIcon, Clock, Note) ─────────────────
            var showNoteDialog by remember { mutableStateOf(false) }
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 24.dp)
                    .width(96.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
                horizontalAlignment = Alignment.Start
            ) {
                PixelIcon(color = primary, isScrolling = isScrolling, modifier = Modifier.size(26.dp))
                ClockWidget(
                    isDarkMode = isDarkMode,
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            clockAppPackage?.let { viewModel.launchApp(it) }
                                ?: run { showClockAppPicker = true }
                        },
                        onLongClick = { showClockAppPicker = true },
                    )
                )
                
                // Clickable Note Widget
                Column(
                    modifier = Modifier.clickable { showNoteDialog = true }
                ) {
                    Text(
                        text = noteText,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp,
                        lineHeight = 12.sp,
                        color = primary.copy(alpha = 0.6f)
                    )
                }
            }

            // Note edit dialog
            if (showNoteDialog) {
                var tempText by remember { mutableStateOf(noteText) }
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showNoteDialog = false },
                    title = { Text("Edit Status Note", fontSize = 16.sp, fontFamily = FontFamily.Monospace) },
                    text = {
                        androidx.compose.material3.TextField(
                            value = tempText,
                            onValueChange = { tempText = it },
                            placeholder = { Text("PROJECT\nASSIGNM\nENT", fontFamily = FontFamily.Monospace) }
                        )
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                viewModel.updateNoteText(tempText)
                                showNoteDialog = false
                            }
                        ) {
                            Text("Save", fontFamily = FontFamily.Monospace)
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { showNoteDialog = false }) {
                            Text("Cancel", fontFamily = FontFamily.Monospace)
                        }
                    }
                )
            }

            // ── Avatar + charging + theme pill — top right ───────────────
            val textPillColor = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 24.dp, top = 20.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(if (isDarkMode) Color(0x991C1C1E) else Color(0xB3FFFFFF))
                    .then(
                        if (isDarkMode) Modifier.border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(26.dp))
                        else Modifier.border(0.5.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(26.dp))
                    )
                    .padding(vertical = 12.dp, horizontal = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Profile Avatar Container (with optional rotating charging ring)
                Box(
                    modifier = Modifier.size(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCharging) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { rotationZ = ringAngle }
                                .drawBehind {
                                    val strokeWidth = 2.dp.toPx()
                                    drawCircle(
                                        brush = Brush.sweepGradient(
                                            colors = listOf(
                                                Color(0xFFFFD60A).copy(alpha = 0.1f),
                                                Color(0xFFFFD60A),
                                                Color(0xFFFFD60A).copy(alpha = 0.1f)
                                            )
                                        ),
                                        style = Stroke(width = strokeWidth)
                                    )
                                }
                        )
                    }
                    // Profile Avatar Circle
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDarkMode) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.1f))
                            .clickable {
                                avatarPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (userAvatarUri != null) {
                            AsyncImage(
                                model = userAvatarUri,
                                contentDescription = "User Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector        = Icons.Outlined.Person,
                                contentDescription = "Profile",
                                tint               = if (isDarkMode) Color.White else Color.Black,
                                modifier           = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // HI USER
                Text(
                    text = "HI ${userName.uppercase()}",
                    fontSize = 7.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = textPillColor,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showNameDialog = true }
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                )

                // Theme Toggle
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isDarkMode) Color.White else Color.Black)
                        .clickable { viewModel.toggleDarkMode() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                        contentDescription = "Toggle theme",
                        tint = if (isDarkMode) Color.Black else Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }

                if (isCharging) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFD60A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.Bolt,
                            contentDescription = "Charging",
                            tint               = Color.Black,
                            modifier           = Modifier.size(16.dp),
                        )
                    }
                }
            }

            // ── Right-Edge Action Bar ─────────────────────────────────────
            val rightBarPrimary = if (isDarkMode) Color.White else Color.Black
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(if (isDarkMode) Color(0x991C1C1E) else Color(0xB3FFFFFF))
                    .then(
                        if (isDarkMode) Modifier.border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                        else Modifier.border(0.5.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    )
                    .padding(vertical = 16.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Camera
                IconButton(
                    onClick = {
                        try {
                            val intent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = "Camera",
                        tint = rightBarPrimary.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                // Video Recorder
                IconButton(
                    onClick = {
                        try {
                            val intent = Intent(android.provider.MediaStore.INTENT_ACTION_VIDEO_CAMERA)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Videocam,
                        contentDescription = "Video",
                        tint = rightBarPrimary.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                // Gallery
                IconButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("content://media/internal/images/media"))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.photos")
                                ?: context.packageManager.getLaunchIntentForPackage("com.android.gallery3d")
                            if (intent != null) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            }
                        }
                    },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = "Gallery",
                        tint = rightBarPrimary.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // ── Bottom Slider & Explore Bar ───────────────────────────────
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp, start = 20.dp, end = 20.dp)
                    .height(48.dp)
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isDarkMode) Color(0x991C1C1E) else Color(0xB3FFFFFF))
                    .then(
                        if (isDarkMode) Modifier.border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                        else Modifier.border(0.5.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Explore text + Drawer Button
                Row(
                    modifier = Modifier.clickable { viewModel.openDrawer() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Menu,
                        contentDescription = "Explore",
                        tint = primary.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "EXPLORE",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = primary.copy(alpha = 0.8f)
                    )
                }

                // Minimalist M3 Slider in the center
                androidx.compose.material3.Slider(
                    value = iconSizePercent,
                    onValueChange = { newValue ->
                        val oldVal = iconSizePercent.toInt()
                        viewModel.updateIconSizePercent(newValue)
                        if (newValue.toInt() != oldVal) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                    valueRange = 0f..100f,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    colors = androidx.compose.material3.SliderDefaults.colors(
                        thumbColor = primary,
                        activeTrackColor = primary,
                        inactiveTrackColor = primary.copy(alpha = 0.2f),
                    )
                )

                // Value and Wallpaper Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${iconSizePercent.toInt()} %",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = primary.copy(alpha = 0.8f)
                    )
                    IconButton(
                        onClick = { showWallpaperDialog = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Wallpaper,
                            contentDescription = "Change Wallpaper",
                            tint = primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            if (showNameDialog) {
                var draftName by remember(userName) { mutableStateOf(userName) }
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showNameDialog = false },
                    title = { Text("Your Name", fontSize = 16.sp, fontFamily = FontFamily.Monospace) },
                    text = {
                        TextField(
                            value = draftName,
                            onValueChange = { draftName = it.take(18) },
                            singleLine = true,
                            placeholder = { Text("Nikola", fontFamily = FontFamily.Monospace) }
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val cleaned = draftName.trim().ifBlank { "USER" }
                                viewModel.updateUserName(cleaned)
                                showNameDialog = false
                            }
                        ) { Text("Save", fontFamily = FontFamily.Monospace) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNameDialog = false }) {
                            Text("Cancel", fontFamily = FontFamily.Monospace)
                        }
                    }
                )
            }

            if (showWallpaperDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showWallpaperDialog = false },
                    title = { Text("Wallpaper", fontSize = 16.sp, fontFamily = FontFamily.Monospace) },
                    text = {
                        Text(
                            text = "Choose a custom image or restore the default launcher background.",
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showWallpaperDialog = false
                                wallpaperPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) { Text("Choose", fontFamily = FontFamily.Monospace) }
                    },
                    dismissButton = {
                        Row {
                            TextButton(
                                onClick = {
                                    viewModel.updateWallpaper(null)
                                    showWallpaperDialog = false
                                }
                            ) { Text("Default", fontFamily = FontFamily.Monospace) }
                            TextButton(onClick = { showWallpaperDialog = false }) {
                                Text("Cancel", fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                )
            }

            if (showWeatherDialog) {
                var draftCity by remember(weather.city) { mutableStateOf(weather.city) }
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showWeatherDialog = false },
                    title = { Text("Weather City", fontSize = 16.sp, fontFamily = FontFamily.Monospace) },
                    text = {
                        TextField(
                            value = draftCity,
                            onValueChange = { draftCity = it.take(40) },
                            singleLine = true,
                            placeholder = { Text("Ljubljana", fontFamily = FontFamily.Monospace) }
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.updateWeatherCity(draftCity)
                                showWeatherDialog = false
                            }
                        ) { Text("Save", fontFamily = FontFamily.Monospace) }
                    },
                    dismissButton = {
                        Row {
                            TextButton(
                                onClick = {
                                    showWeatherDialog = false
                                    showWeatherAppPicker = true
                                }
                            ) { Text("App", fontFamily = FontFamily.Monospace) }
                            TextButton(onClick = { showWeatherDialog = false }) {
                                Text("Cancel", fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                )
            }

            if (showClockAppPicker) {
                AppTargetPickerDialog(
                    title = "Clock App",
                    apps = allApps,
                    onDismiss = { showClockAppPicker = false },
                    onPick = {
                        viewModel.setClockApp(it.packageName)
                        viewModel.launchApp(it.packageName)
                        showClockAppPicker = false
                    },
                )
            }

            if (showWeatherAppPicker) {
                AppTargetPickerDialog(
                    title = "Weather App",
                    apps = allApps,
                    onDismiss = { showWeatherAppPicker = false },
                    onPick = {
                        viewModel.setWeatherApp(it.packageName)
                        viewModel.launchApp(it.packageName)
                        showWeatherAppPicker = false
                    },
                )
            }

            // ── Long-press context menu ───────────────────────────────────
            contextApp?.let { app ->
                AppContextMenu(
                    app = app,
                    onChangeImage = {
                        imagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onSetBackgroundMode = { mode ->
                        viewModel.updateBackgroundMode(app.app.packageName, mode)
                        contextApp = null
                    },
                    onRemove = {
                        viewModel.removeFromHome(app.app.packageName)
                        contextApp = null
                    },
                    onDismiss = { contextApp = null },
                )
            }

            // ── App drawer — slides up ────────────────────────────────────
            AnimatedVisibility(
                visible  = showDrawer,
                modifier = Modifier.fillMaxSize(),
                enter    = slideInVertically { it },
                exit     = slideOutVertically { it },
            ) {
                AppDrawer(
                    apps           = allApps,
                    onAppLaunch    = { viewModel.launchApp(it) },
                    onAppAddToHome = { viewModel.addAppToHome(it) },
                    onDismiss      = { viewModel.closeDrawer() },
                )
            }
        }

        if (showDrawer) BackHandler { viewModel.closeDrawer() }
    }
}

@Composable
private fun DateHeader(isDarkMode: Boolean, modifier: Modifier = Modifier) {
    val now       = remember { Date() }
    val dayName   = SimpleDateFormat("EEEE",  Locale.getDefault()).format(now).uppercase()
    val dayNum    = SimpleDateFormat("dd",    Locale.getDefault()).format(now)
    val monthName = SimpleDateFormat("MMM",   Locale.getDefault()).format(now).uppercase()

    val primary   = if (isDarkMode) Color.White else Color.Black
    val secondary = primary.copy(alpha = 0.4f)

    Column(modifier = modifier) {
        Text(dayName,  fontSize = 9.sp,  fontWeight = FontWeight.SemiBold,
             fontFamily = FontFamily.Monospace, letterSpacing = 1.2.sp, color = secondary)
        Text(dayNum,   fontSize = 9.sp,  fontWeight = FontWeight.SemiBold,
             fontFamily = FontFamily.Monospace, letterSpacing = 1.2.sp, color = secondary)
        Text(monthName, fontSize = 9.sp, fontWeight = FontWeight.SemiBold,
             fontFamily = FontFamily.Monospace, letterSpacing = 1.2.sp, color = secondary)
    }
}

@Composable
private fun AppTargetPickerDialog(
    title: String,
    apps: List<AppItem>,
    onDismiss: () -> Unit,
    onPick: (AppItem) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, apps) {
        if (query.isBlank()) apps else apps.filter { it.label.contains(query, ignoreCase = true) }
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontSize = 16.sp, fontFamily = FontFamily.Monospace) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("Search apps", fontFamily = FontFamily.Monospace) }
                )
                LazyColumn(modifier = Modifier.height(320.dp)) {
                    items(filtered, key = { it.packageName }) { app ->
                        Text(
                            text = app.label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(app) }
                                .padding(vertical = 12.dp),
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontFamily = FontFamily.Monospace)
            }
        }
    )
}

@Composable
private fun PixelWeatherWidget(
    isDarkMode: Boolean,
    city: String,
    temperatureC: Int?,
    conditionCode: Int?,
    modifier: Modifier = Modifier,
) {
    val color = if (isDarkMode) Color.White else Color.Black
    val sunPattern = listOf(
        "  █  █  ",
        " █ ██ █ ",
        "████████",
        " █ ██ █ ",
        "  █  █  "
    )
    val moonPattern = listOf(
        "  ████  ",
        " ██   █ ",
        " ██     ",
        " ██   █ ",
        "  ████  "
    )
    val pattern = if ((conditionCode ?: 0) in 0..3) sunPattern else moonPattern
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Canvas(modifier = Modifier.size(16.dp)) {
                val rows = pattern.size
                val cols = pattern[0].length
                val cellW = size.width / cols
                val cellH = size.height / rows
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        if (pattern[r][c] == '█') {
                            drawRect(
                                color = color.copy(alpha = 0.8f),
                                topLeft = Offset(c * cellW, r * cellH),
                                size = Size(cellW + 0.3f, cellH + 0.3f)
                            )
                        }
                    }
                }
            }
            Text(
                text = temperatureC?.let { "$it°C" } ?: "--°C",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = color.copy(alpha = 0.72f)
            )
        }
        Text(
            text = city.uppercase(),
            fontSize = 7.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = color.copy(alpha = 0.42f),
            maxLines = 1,
        )
    }
}
