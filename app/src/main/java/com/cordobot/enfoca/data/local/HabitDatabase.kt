package com.cordobot.enfoca.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val color: Long,
    // Minutes after midnight, null when the habit has no reminder
    val reminderMinuteOfDay: Int?,
    val createdOnEpochDay: Long
)

@Entity(
    tableName = "completions",
    primaryKeys = ["habitId", "epochDay"],
    foreignKeys = [ForeignKey(HabitEntity::class, ["id"], ["habitId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("habitId")]
)
data class CompletionEntity(val habitId: Long, val epochDay: Long)

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY createdOnEpochDay, id")
    fun observeHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM completions")
    fun observeCompletions(): Flow<List<CompletionEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabit(id: Long): HabitEntity?

    @Upsert
    suspend fun upsert(habit: HabitEntity): Long

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM completions WHERE habitId = :habitId AND epochDay = :epochDay)")
    suspend fun isCompleted(habitId: Long, epochDay: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletion(completion: CompletionEntity)

    @Query("DELETE FROM completions WHERE habitId = :habitId AND epochDay = :epochDay")
    suspend fun deleteCompletion(habitId: Long, epochDay: Long)
}

@Database(entities = [HabitEntity::class, CompletionEntity::class], version = 1, exportSchema = true)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
}
