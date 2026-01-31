package org.haokee.recorder.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "thoughts")
data class Thought(
    @PrimaryKey
    val id: String,
    val title: String? = null,
    val content: String? = null,
    val audioPath: String,
    val color: ThoughtColor? = null,
    val alarmTime: LocalDateTime? = null,
    val createdAt: LocalDateTime,
    val transcribedAt: LocalDateTime? = null,
    val isTranscribed: Boolean = false,
    val waveformData: String? = null // JSON 数组存储波形数据
)
