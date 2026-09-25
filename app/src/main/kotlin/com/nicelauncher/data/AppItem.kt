package com.nicelauncher.data

import android.graphics.drawable.Drawable
import android.net.Uri

data class AppItem(
    val packageName: String,
    val label: String,
    val icon: Drawable,
)

enum class BackgroundMode {
    BLURRED,  // blurred version of app icon fills the circle
    BLACK,    // solid black background
    WHITE,    // solid white background
    PHOTO,    // custom photo from gallery (uses customImageUri)
}

data class HomeApp(
    val packageName: String,
    val customImageUri: String? = null,
    val backgroundMode: BackgroundMode = BackgroundMode.BLURRED,
)

data class AppWithIcon(
    val app: AppItem,
    val customImageUri: Uri?,
    val backgroundMode: BackgroundMode = BackgroundMode.BLURRED,
)
