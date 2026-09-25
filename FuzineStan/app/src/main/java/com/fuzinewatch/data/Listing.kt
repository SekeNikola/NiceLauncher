package com.fuzinewatch.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.security.MessageDigest

@Entity(tableName = "listings")
data class Listing(
    @PrimaryKey val id: String,
    val source: String,
    val title: String,
    val price: String,
    val size: String,
    val link: String,
    val firstSeen: Long,
)

fun listingId(source: String, link: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest("$source|$link".toByteArray())
        .joinToString("") { "%02x".format(it) }
