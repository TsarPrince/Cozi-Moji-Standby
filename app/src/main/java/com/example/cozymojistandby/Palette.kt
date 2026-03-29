package com.example.cozymojistandby

import android.graphics.Color

data class Palette(
    val c0: IntArray,
    val c1: IntArray,
    val c2: IntArray,
    val c3: IntArray
)

val pallets = mapOf<String, Palette>(
    "blue-green" to Palette(
        color("0261da", "0063db"),
        color("61d392", "65da98"),
        color("38b0fc", "37b6fb"),
        color("2a8564", "288b65"),
    ),
    "pink-purple" to Palette(
        color("9b3aa5", "9b3aa5"),
        color("a690f7", "a690f6"),
        color("d271b3", "cd72b5"),
        color("6b51c9", "5e47b1"),
    ),
)

fun color(hex1: String, hex2: String) : IntArray {
    val alpha = "#e5"
    return intArrayOf(Color.parseColor(alpha + hex1), Color.parseColor(alpha + hex2))
}