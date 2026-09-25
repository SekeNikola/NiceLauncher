package com.fuzinewatch.notif

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.fuzinewatch.R
import com.fuzinewatch.data.Listing

object Notifier {

    private const val CHANNEL_ID = "new_listings"

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "New listings",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "New apartment sale listings in Fužine"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun notifyNewListing(context: Context, listing: Listing) {
        if (!canNotify(context)) return

        val tapIntent = PendingIntent.getActivity(
            context,
            listing.id.hashCode(),
            Intent(Intent.ACTION_VIEW, Uri.parse(listing.link)),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val details = listOf(listing.price, listing.size)
            .filter { it.isNotBlank() }
            .joinToString(" · ")

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(listing.title)
            .setContentText(details.ifEmpty { "New listing" })
            .setSubText(listing.source)
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(listing.id.hashCode(), notification)
    }

    private fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
}
