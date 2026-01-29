package org.haokee.recorder.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.haokee.recorder.data.model.Thought
import org.haokee.recorder.data.model.ThoughtColor
import java.time.LocalDateTime

@Dao
interface ThoughtDao {
    @Query("SELECT * FROM thoughts ORDER BY createdAt DESC")
    fun getAllThoughts(): Flow<List<Thought>>

    @Query("SELECT * FROM thoughts WHERE isTranscribed = 1 AND (alarmTime IS NULL OR alarmTime > :currentTime) ORDER BY createdAt DESC")
    fun getTranscribedThoughts(currentTime: LocalDateTime = LocalDateTime.now()): Flow<List<Thought>>

    @Query("SELECT * FROM thoughts WHERE isTranscribed = 0 AND (alarmTime IS NULL OR alarmTime > :currentTime) ORDER BY createdAt DESC")
    fun getOriginalThoughts(currentTime: LocalDateTime = LocalDateTime.now()): Flow<List<Thought>>

    @Query("SELECT * FROM thoughts WHERE alarmTime IS NOT NULL AND alarmTime <= :currentTime ORDER BY createdAt DESC")
    fun getExpiredAlarmThoughts(currentTime: LocalDateTime = LocalDateTime.now()): Flow<List<Thought>>

    @Query("SELECT * FROM thoughts WHERE color IN (:colors) ORDER BY createdAt DESC")
    fun getThoughtsByColors(colors: List<ThoughtColor>): Flow<List<Thought>>

    @Query("SELECT * FROM thoughts WHERE id = :id")
    suspend fun getThoughtById(id: String): Thought?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThought(thought: Thought)

    @Update
    suspend fun updateThought(thought: Thought)

    @Delete
    suspend fun deleteThought(thought: Thought)

    @Query("DELETE FROM thoughts WHERE id IN (:ids)")
    suspend fun deleteThoughtsByIds(ids: List<String>)
}
