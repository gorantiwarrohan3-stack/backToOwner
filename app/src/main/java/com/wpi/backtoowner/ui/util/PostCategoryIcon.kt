package com.wpi.backtoowner.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.TabletMac
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Watch
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Locale

/**
 * Normalizes labels like `"Lost: Wallet"` to `"wallet"` for keyword matching.
 */
fun normalizedCategoryKey(raw: String): String {
    val trimmed = raw.trim()
    val afterColon = trimmed.substringAfterLast(":").trim()
    return afterColon.lowercase(Locale.US).ifBlank { trimmed.lowercase(Locale.US) }
}

/**
 * Picks a Material icon from free-text category / title (e.g. MLKit chip or post title).
 */
fun categoryIconForItemTitle(rawTitle: String): ImageVector {
    val t = normalizedCategoryKey(rawTitle)
    return when {
        t.contains("wallet") || t.contains("purse") || t.contains("billfold") ->
            Icons.Filled.AccountBalanceWallet
        t.contains("keyboard") -> Icons.Filled.Keyboard
        (t.contains("key") || t.contains("keys")) && !t.contains("keyboard") -> Icons.Filled.VpnKey
        t.contains("phone") || t.contains("iphone") || t.contains("mobile") || t.contains("android") ||
            t.contains("airpod") || t.contains("earbud") ->
            Icons.Filled.Smartphone
        t.contains("tablet") || t.contains("ipad") -> Icons.Filled.TabletMac
        t.contains("watch") || t.contains("fitbit") -> Icons.Filled.Watch
        t.contains("bottle") || t.contains("water") -> Icons.Filled.LocalDrink
        (t.contains("id") && (t.contains("card") || t.contains("badge"))) ||
            t.contains("license") || t.contains("passport") ->
            Icons.Filled.Badge
        t.contains("glass") || t.contains("eyewear") || t.contains("sunglass") || t.contains("goggle") ->
            Icons.Filled.Visibility
        t.contains("backpack") || t.contains("back pack") || t.contains("rucksack") ||
            t.contains("tote") || t.contains("bag") || t.contains("luggage") ->
            Icons.Filled.ShoppingBag
        t.contains("laptop") || t.contains("charger") || t.contains("cable") || t.contains("usb") ||
            t.contains("hub") || t.contains("computer") || t.contains("monitor") || t.contains("mouse") ||
            t.contains("headphone") || t.contains("earphone") || t.contains("electronic") ->
            Icons.Filled.Devices
        else -> Icons.Filled.Inventory2
    }
}
