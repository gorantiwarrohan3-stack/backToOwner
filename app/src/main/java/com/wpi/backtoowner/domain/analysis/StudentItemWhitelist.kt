package com.wpi.backtoowner.domain.analysis

import java.util.Locale

/**
 * Maps ML Kit label text to WPI student-item categories (see project spec).
 */
object StudentItemWhitelist {

    val CANONICAL_CATEGORIES: Set<String> = setOf(
        "Electronics",
        "Keys",
        "ID Card",
        "Wallet",
        "Water Bottle",
        "Backpack",
        "Eyewear",
    )

    fun matchCategory(mlLabelText: String): String? {
        val t = mlLabelText.lowercase(Locale.US).trim()
        if (t.isEmpty()) return null

        return when {
            t.contains("credit card") || t.contains("debit card") -> null
            t.contains("key") && !t.contains("keyboard") -> "Keys"
            t.contains("wallet") || t.contains("purse") || t.contains("billfold") -> "Wallet"
            t.contains("backpack") || t.contains("back pack") || t.contains("rucksack") -> "Backpack"
            t.contains("water bottle") || (t.contains("bottle") && t.contains("water")) -> "Water Bottle"
            t.contains("bottle") && !t.contains("baby") -> "Water Bottle"
            t.contains("eyewear") || t.contains("eyeglass") || t.contains("sunglass") ||
                t.contains("spectacle") || t.contains("goggle") ||
                (t.contains("glass") && (t.contains("eye") || t.contains("wear"))) -> "Eyewear"
            t.contains("id card") || t.contains("identification card") || t.contains("student id") ||
                (t.contains("driver") && t.contains("license")) ||
                (t.contains("badge") && t.contains("id")) -> "ID Card"
            t.contains("identification") && (t.contains("card") || t.contains("document")) -> "ID Card"
            t.contains("mouse") || t.contains("trackpad") || t.contains("touchpad") ||
                t.contains("magic mouse") -> "Electronics"
            // Map keyboard-instrument labels to Electronics before the plain "keyboard" catch,
            // so MLKit's "Musical keyboard" / "Keyboard instrument" never leaks into raw fallback.
            t.contains("keyboard instrument") || t.contains("musical keyboard") -> "Electronics"
            t.contains("keyboard") -> "Electronics"
            t.contains("monitor") || t.contains("display") || t.contains("webcam") ||
                t.contains("dslr") -> "Electronics"
            t.contains("headset") || t.contains("speaker") -> "Electronics"
            t.contains("electronic") || t.contains("laptop") || t.contains("computer") ||
                t.contains("phone") || t.contains("mobile phone") || t.contains("tablet") ||
                t.contains("ipad") || t.contains("airpods") || t.contains("earbuds") ||
                t.contains("charger") || t.contains("headphone") || t.contains("earphone") ||
                t.contains("usb") || t.contains("adapter") || t.contains("hub") ||
                t.contains("cable") -> "Electronics"
            else -> null
        }
    }
}
