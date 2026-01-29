package org.haokee.recorder.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import org.haokee.recorder.data.local.ThoughtDao
import org.haokee.recorder.data.model.Thought
import org.haokee.recorder.data.model.ThoughtColor
import java.io.File
import java.time.LocalDateTime

class ThoughtRepository(
    private val thoughtDao: ThoughtDao,
    private val context: Context
) {
    private val recordingsDir = File(context.filesDir, "recordings").apply {
        if (!exists()) mkdirs()
    }

    fun getAllThoughts(): Flow<List<Thought>> = thoughtDao.getAllThoughts()

    fun getTranscribedThoughts(): Flow<List<Thought>> = thoughtDao.getTranscribedThoughts()

    fun getOriginalThoughts(): Flow<List<Thought>> = thoughtDao.getOriginalThoughts()

    fun getExpiredAlarmThoughts(): Flow<List<Thought>> = thoughtDao.getExpiredAlarmThoughts()

    fun getThoughtsByColors(colors: List<ThoughtColor>): Flow<List<Thought>> =
        thoughtDao.getThoughtsByColors(colors)

    suspend fun getThoughtById(id: String): Thought? = thoughtDao.getThoughtById(id)

    suspend fun insertThought(thought: Thought) {
        thoughtDao.insertThought(thought)
    }

    suspend fun updateThought(thought: Thought) {
        thoughtDao.updateThought(thought)
    }

    suspend fun deleteThought(thought: Thought) {
        // Delete audio file
        deleteAudioFile(thought.audioPath)
        // Delete from database
        thoughtDao.deleteThought(thought)
    }

    suspend fun deleteThoughts(thoughts: List<Thought>) {
        // Delete audio files
        thoughts.forEach { deleteAudioFile(it.audioPath) }
        // Delete from database
        thoughtDao.deleteThoughtsByIds(thoughts.map { it.id })
    }

    fun getAudioFile(fileName: String): File {
        return File(recordingsDir, fileName)
    }

    fun createAudioFile(fileName: String): File {
        return File(recordingsDir, fileName)
    }

    private fun deleteAudioFile(fileName: String) {
        val file = File(recordingsDir, fileName)
        if (file.exists()) {
            file.delete()
        }
    }

    fun getRecordingsDirectory(): File = recordingsDir
}
