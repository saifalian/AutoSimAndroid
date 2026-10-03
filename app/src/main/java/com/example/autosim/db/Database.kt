
package com.example.autosim.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "click_spots")
data class ClickSpot(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val x: Int,
    val y: Int,
    val delay: Long = 0,
    val repeat: Int = 1
)

@Entity(tableName = "ocr_regions")
data class OcrRegion(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int
)

@Entity(tableName = "ocr_groups")
data class OcrGroup(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val regionId: Int
)

@Entity(tableName = "ocr_phrases")
data class OcrPhrase(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val groupId: Int,
    val text: String,
    val actionId: Int? = null // ClickSpot id for action
)

@Entity(tableName = "text_detection_phrases")
data class TextDetectionPhrase(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val text: String,
    val actionId: Int? = null // ClickSpot id for action
)

@Entity(tableName = "sequences")
data class Sequence(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val repeatCount: Int = 1 // 1 for one time, -1 for infinite, N for N times
)

enum class StepType {
    CLICK, OCR_REGION_SCAN, TEXT_DETECTION, WAIT, REPEAT, CONDITIONAL
}

class StepTypeConverter {
    @TypeConverter
    fun fromStepType(value: StepType): String = value.name
    
    @TypeConverter
    fun toStepType(value: String): StepType = StepType.valueOf(value)
}

@Entity(tableName = "sequence_steps")
data class SequenceStep(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val sequenceId: Int,
    val stepNumber: Int,
    val type: StepType,
    val targetId: Int? = null, // Can be ClickSpot id, OcrRegion id, etc.
    val delay: Long? = null, // For WAIT step
    val delayAfter: Long = 100, // Delay after this step
    val repeatCount: Int? = null, // For REPEAT step
    val jumpToStep: Int? = null, // For CONDITIONAL step
    val conditionalPhrase: String? = null // For CONDITIONAL step
)

@Entity(tableName = "ocr_region_text_history")
data class OcrRegionTextHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val regionId: Int,
    val recognizedText: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "logs_history")
data class LogHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long,
    val message: String,
    val type: String = "INFO" // INFO, ERROR, SUCCESS
)

@Dao
interface ClickSpotDao {
    @Query("SELECT * FROM click_spots ORDER BY id DESC")
    fun getAll(): Flow<List<ClickSpot>>
    
    @Query("SELECT * FROM click_spots WHERE id = :id")
    suspend fun getById(id: Int): ClickSpot?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(spot: ClickSpot): Long

    @Update
    suspend fun update(spot: ClickSpot)

    @Delete
    suspend fun delete(spot: ClickSpot)
}

@Dao
interface OcrRegionDao {
    @Query("SELECT * FROM ocr_regions ORDER BY id DESC")
    fun getAll(): Flow<List<OcrRegion>>
    
    @Query("SELECT * FROM ocr_regions WHERE id = :id")
    suspend fun getById(id: Int): OcrRegion?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(region: OcrRegion): Long

    @Update
    suspend fun update(region: OcrRegion)

    @Delete
    suspend fun delete(region: OcrRegion)
}

@Dao
interface OcrGroupDao {
    @Query("SELECT * FROM ocr_groups ORDER BY id DESC")
    fun getAll(): Flow<List<OcrGroup>>
    
    @Query("SELECT * FROM ocr_groups WHERE regionId = :regionId ORDER BY id DESC")
    fun getGroupsForRegion(regionId: Int): Flow<List<OcrGroup>>
    
    @Query("SELECT * FROM ocr_groups WHERE id = :id")
    suspend fun getById(id: Int): OcrGroup?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: OcrGroup): Long

    @Update
    suspend fun update(group: OcrGroup)

    @Delete
    suspend fun delete(group: OcrGroup)
}

@Dao
interface OcrPhraseDao {
    @Query("SELECT * FROM ocr_phrases ORDER BY id DESC")
    fun getAll(): Flow<List<OcrPhrase>>
    
    @Query("SELECT * FROM ocr_phrases WHERE groupId = :groupId ORDER BY id DESC")
    fun getPhrasesForGroup(groupId: Int): Flow<List<OcrPhrase>>
    
    @Query("SELECT * FROM ocr_phrases WHERE id = :id")
    suspend fun getById(id: Int): OcrPhrase?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(phrase: OcrPhrase): Long

    @Update
    suspend fun update(phrase: OcrPhrase)

    @Delete
    suspend fun delete(phrase: OcrPhrase)
}

@Dao
interface TextDetectionPhraseDao {
    @Query("SELECT * FROM text_detection_phrases ORDER BY id DESC")
    fun getAll(): Flow<List<TextDetectionPhrase>>
    
    @Query("SELECT * FROM text_detection_phrases WHERE id = :id")
    suspend fun getById(id: Int): TextDetectionPhrase?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(phrase: TextDetectionPhrase): Long

    @Update
    suspend fun update(phrase: TextDetectionPhrase)

    @Delete
    suspend fun delete(phrase: TextDetectionPhrase)
}

@Dao
interface SequenceDao {
    @Query("SELECT * FROM sequences ORDER BY id DESC")
    fun getAll(): Flow<List<Sequence>>
    
    @Query("SELECT * FROM sequences WHERE id = :id")
    suspend fun getById(id: Int): Sequence?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sequence: Sequence): Long

    @Update
    suspend fun update(sequence: Sequence)

    @Delete
    suspend fun delete(sequence: Sequence)
}

@Dao
interface SequenceStepDao {
    @Query("SELECT * FROM sequence_steps ORDER BY sequenceId, stepNumber ASC")
    fun getAll(): Flow<List<SequenceStep>>
    
    @Query("SELECT * FROM sequence_steps WHERE sequenceId = :sequenceId ORDER BY stepNumber ASC")
    fun getStepsForSequence(sequenceId: Int): Flow<List<SequenceStep>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(step: SequenceStep)

    @Update
    suspend fun update(step: SequenceStep)

    @Delete
    suspend fun delete(step: SequenceStep)
}

@Dao
interface OcrRegionTextHistoryDao {
    @Query("SELECT * FROM ocr_region_text_history WHERE regionId = :regionId ORDER BY timestamp DESC LIMIT 50")
    fun getHistoryForRegion(regionId: Int): Flow<List<OcrRegionTextHistory>>

    @Insert
    suspend fun insert(history: OcrRegionTextHistory)
    
    @Query("DELETE FROM ocr_region_text_history WHERE regionId = :regionId")
    suspend fun deleteForRegion(regionId: Int)
}

@Dao
interface LogHistoryDao {
    @Query("SELECT * FROM logs_history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<LogHistory>>

    @Insert
    suspend fun insert(log: LogHistory)
    
    @Query("DELETE FROM logs_history")
    suspend fun clearLogs()
}

@Database(
    entities = [
        ClickSpot::class, 
        OcrRegion::class, 
        OcrGroup::class, 
        OcrPhrase::class,
        TextDetectionPhrase::class,
        Sequence::class, 
        SequenceStep::class, 
        OcrRegionTextHistory::class,
        LogHistory::class
    ], 
    version = 4,
    exportSchema = false
)
@TypeConverters(StepTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clickSpotDao(): ClickSpotDao
    abstract fun ocrRegionDao(): OcrRegionDao
    abstract fun ocrGroupDao(): OcrGroupDao
    abstract fun ocrPhraseDao(): OcrPhraseDao
    abstract fun textDetectionPhraseDao(): TextDetectionPhraseDao
    abstract fun sequenceDao(): SequenceDao
    abstract fun sequenceStepDao(): SequenceStepDao
    abstract fun ocrRegionTextHistoryDao(): OcrRegionTextHistoryDao
    abstract fun logHistoryDao(): LogHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "autosim_database"
                )
                .fallbackToDestructiveMigration(true) // For development - remove in production
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
