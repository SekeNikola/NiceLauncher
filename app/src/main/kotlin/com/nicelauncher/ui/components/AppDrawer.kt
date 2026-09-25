package com.nicelauncher.ui.components

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nicelauncher.data.AppItem

@Composable
fun AppDrawer(
    apps: List<AppItem>,
    onAppLaunch: (String) -> Unit,
    onAppAddToHome: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, apps) {
        val q = query.trim()
        if (q.isBlank()) apps
        else apps.filter { it.label.contains(q, ignoreCase = true) }
    }

    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    // Focus the search box (and raise the keyboard) as soon as the drawer opens,
    // so the user can start typing immediately. The short delay lets the slide-in
    // animation attach the node before we request focus.
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(250)
        runCatching { focusRequester.requestFocus() }
        keyboard?.show()
    }

    fun launchTopResult() {
        filtered.firstOrNull()?.let {
            keyboard?.hide()
            onAppLaunch(it.packageName)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim layer sits *behind* the content. Tapping empty space dismisses the
        // drawer. Keeping this as a separate sibling (instead of wrapping the content
        // in a clickable) means it no longer competes with the search field for the
        // tap/focus — which was preventing the keyboard from opening.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF2000000))
                .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = "All Apps",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                    Text(
                        text = "Tap to open · Hold to pin home",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.4f),
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = {
                    Text("Search", color = Color.White.copy(alpha = 0.35f), fontSize = 15.sp)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .focusRequester(focusRequester),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor   = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor   = Color.White.copy(alpha = 0.3f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    cursorColor = Color.White,
                ),
                shape = RoundedCornerShape(28.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { launchTopResult() }),
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.35f),
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = Color.White.copy(alpha = 0.5f),
                            )
                        }
                    }
                },
            )

            Spacer(Modifier.height(16.dp))

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .imePadding(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Text(
                        text = if (query.isBlank()) "No apps" else "No results for \"${query.trim()}\"",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 48.dp),
                    )
                }
            } else if (query.isBlank()) {
                // Browsing all apps: normal top-to-bottom, left-to-right grid.
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .imePadding(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    items(filtered, key = { it.packageName }) { app ->
                        DrawerAppItem(
                            app = app,
                            onClick = { onAppLaunch(app.packageName) },
                            onLongClick = { onAppAddToHome(app.packageName) },
                        )
                    }
                }
            } else {
                // Active search results: pushed to the BOTTOM (spacedBy with
                // Alignment.Bottom) and starting from the RIGHT (RTL layout direction)
                // so matches sit just above the keyboard, within easy thumb reach.
                // The Box takes the remaining column space via weight() and carries the
                // imePadding so the grid's bottom is the visible bottom above the keyboard.
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .imePadding(),
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.Bottom),
                        ) {
                            items(filtered, key = { it.packageName }) { app ->
                                // Render each cell back in LTR so the label reads normally.
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    DrawerAppItem(
                                        app = app,
                                        onClick = { onAppLaunch(app.packageName) },
                                        onLongClick = { onAppAddToHome(app.packageName) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerAppItem(
    app: AppItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        modifier = Modifier.combinedClickable(
            onClick    = onClick,
            onLongClick = onLongClick,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val bitmap = remember(app.icon) { app.icon.toBitmapSafe() }
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape),
        ) {
            Image(
                painter            = BitmapPainter(bitmap.asImageBitmap()),
                contentDescription = app.label,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text      = app.label,
            color     = Color.White,
            fontSize  = 11.sp,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

private fun Drawable.toBitmapSafe(fallback: Int = 96): Bitmap {
    if (this is BitmapDrawable && bitmap != null) return bitmap
    val w   = if (intrinsicWidth > 0) intrinsicWidth else fallback
    val h   = if (intrinsicHeight > 0) intrinsicHeight else fallback
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    android.graphics.Canvas(bmp).also { canvas ->
        setBounds(0, 0, w, h)
        draw(canvas)
    }
    return bmp
}
