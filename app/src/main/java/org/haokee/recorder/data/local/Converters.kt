package org.haokee.recorder.data.local

import androidx.room.TypeConverter
import org.haokee.recorder.data.model.ThoughtColor
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Converters {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? {
        return value?.format(formatter)
    }

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it, formatter) }
    }

    @TypeConverter
    fun fromThoughtColor(value: ThoughtColor?): String? {
        return value?.name
    }

    @TypeConverter
    fun toThoughtColor(value: String?): ThoughtColor? {
        return value?.let { ThoughtColor.valueOf(it) }
    }
}
