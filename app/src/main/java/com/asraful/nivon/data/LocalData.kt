package com.asraful.nivon.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "emergency_contacts")
data class EmergencyContact(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val phone: String, val relationship: String = "", val notes: String = "", val primaryContact: Boolean = false)

@Entity(tableName = "safety_profile")
data class SafetyProfile(@PrimaryKey val id: Int = 1, val fullName: String = "", val bloodGroup: String = "", val allergies: String = "", val conditions: String = "", val medications: String = "", val notes: String = "")

@Dao interface NivonDao {
    @Query("SELECT * FROM emergency_contacts ORDER BY primaryContact DESC, name COLLATE NOCASE") fun contacts(): Flow<List<EmergencyContact>>
    @Query("SELECT * FROM safety_profile WHERE id = 1") fun profile(): Flow<SafetyProfile?>
    @Query("SELECT * FROM emergency_contacts WHERE phone = :phone LIMIT 1") suspend fun byPhone(phone: String): EmergencyContact?
    @androidx.room.Insert suspend fun insert(contact: EmergencyContact)
    @androidx.room.Update suspend fun update(contact: EmergencyContact)
    @androidx.room.Delete suspend fun delete(contact: EmergencyContact)
    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE) suspend fun saveProfile(profile: SafetyProfile)
    @Query("UPDATE emergency_contacts SET primaryContact = 0") suspend fun clearPrimary()
}

@Database(entities = [EmergencyContact::class, SafetyProfile::class], version = 1, exportSchema = false)
abstract class NivonDatabase : RoomDatabase() { abstract fun dao(): NivonDao
    companion object { @Volatile private var instance: NivonDatabase? = null
        fun get(context: Context) = instance ?: synchronized(this) { instance ?: Room.databaseBuilder(context, NivonDatabase::class.java, "nivon.db").build().also { instance = it } }
    }
}

fun normalizeIndianPhone(value: String): String = value.filter { it.isDigit() || it == '+' }.let {
    when { it.startsWith("+91") && it.length in 13..14 -> it; it.length == 10 && it.all(Char::isDigit) -> "+91$it"; else -> it }
}
fun isValidIndianPhone(value: String): Boolean = normalizeIndianPhone(value).matches(Regex("^\\+91[6-9]\\d{9}$"))
