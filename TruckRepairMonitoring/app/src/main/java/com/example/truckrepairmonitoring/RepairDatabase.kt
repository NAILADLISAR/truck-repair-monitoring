package com.example.truckrepairmonitoring

import android.content.Context
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "repair_records")
data class RepairRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plateNumber: String,
    val repairPart: String,
    val repairDate: String, // yyyy-MM-dd
    val amount: Double
)

@Dao
interface RepairDao {
    @Query("SELECT * FROM repair_records ORDER BY repairDate DESC, id DESC")
    fun observeAll(): Flow<List<RepairRecord>>

    @Insert
    suspend fun insert(record: RepairRecord)

    @Update
    suspend fun update(record: RepairRecord)

    @Delete
    suspend fun delete(record: RepairRecord)
}

@Database(entities = [RepairRecord::class], version = 1, exportSchema = false)
abstract class RepairDatabase : RoomDatabase() {
    abstract fun repairDao(): RepairDao

    companion object {
        @Volatile private var INSTANCE: RepairDatabase? = null

        fun get(context: Context): RepairDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RepairDatabase::class.java,
                    "truck_repair_records.db"
                ).build().also { INSTANCE = it }
            }
    }
}
