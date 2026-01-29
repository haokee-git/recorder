package org.haokee.recorder.util.extension

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

fun LocalDateTime.toDisplayString(): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())
    return this.format(formatter)
}

fun LocalDateTime.toDateString(): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
    return this.format(formatter)
}

fun LocalDateTime.toTimeString(): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    return this.format(formatter)
}

fun Long.toTimeString(): String {
    val seconds = this / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
}

fun Int.toTimeString(): String {
    val seconds = this / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
}
