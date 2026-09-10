package com.freedu.personalgallary.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// --- Entities: only metadata/flags, never media bytes ---

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val mediaStoreId: Long,
    val uriString: String,
    val dateTaken: Long,
    val likedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "captions")
data class CaptionEntity(
    @PrimaryKey val mediaStoreId: Long,
    val caption: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_albums")
data class CustomAlbumEntity(    @PrimaryKey(autoGenerate = true) val albumId: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isLocked: Boolean = false
)

@Entity(
    tableName = "album_items",
    primaryKeys = ["albumId", "mediaStoreId"]
)
data class AlbumItemCrossRef(
    val albumId: Long,
    val mediaStoreId: Long,
    val uriString: String,
    val addedAt: Long = System.currentTimeMillis()
)

// --- DAOs ---

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY likedAt DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT mediaStoreId FROM favorites")
    fun observeIds(): Flow<List<Long>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mediaStoreId = :id)")
    fun observeIsFavorite(id: Long): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(fav: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE mediaStoreId = :id")
    suspend fun remove(id: Long)

    @Query("SELECT COUNT(*) FROM favorites")
    suspend fun count(): Int
}

@Dao
interface CaptionDao {
    @Query("SELECT * FROM captions WHERE mediaStoreId = :id")
    suspend fun get(id: Long): CaptionEntity?

    @Query("SELECT * FROM captions")
    fun observeAll(): Flow<List<CaptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(caption: CaptionEntity)
}

@Dao
interface AlbumDao {
    @Query("SELECT * FROM custom_albums ORDER BY isPinned DESC, createdAt DESC")
    fun observeAlbums(): Flow<List<CustomAlbumEntity>>

    @Query("SELECT * FROM custom_albums WHERE albumId = :id")
    suspend fun getAlbum(id: Long): CustomAlbumEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlbum(album: CustomAlbumEntity): Long

    @Query("DELETE FROM custom_albums WHERE albumId = :id")
    suspend fun deleteAlbum(id: Long)

    @Query("DELETE FROM album_items WHERE albumId = :albumId")
    suspend fun clearAlbum(albumId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addItem(ref: AlbumItemCrossRef)

    @Query("DELETE FROM album_items WHERE albumId = :albumId AND mediaStoreId = :mediaId")
    suspend fun removeItem(albumId: Long, mediaId: Long)

    @Query("SELECT * FROM album_items WHERE albumId = :albumId ORDER BY addedAt DESC")
    fun observeItems(albumId: Long): Flow<List<AlbumItemCrossRef>>

    @Query("SELECT albumId FROM album_items WHERE mediaStoreId = :mediaId")
    suspend fun albumIdsForMedia(mediaId: Long): List<Long>

    @Query("UPDATE custom_albums SET isPinned = :pinned WHERE albumId = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("UPDATE custom_albums SET isLocked = :locked WHERE albumId = :id")
    suspend fun setLocked(id: Long, locked: Boolean)
}

// --- Secure recycle bin: file stays on disk, hidden from lists until purged ---

@Entity(tableName = "trash")
data class TrashEntity(
    @PrimaryKey val mediaStoreId: Long,
    val uriString: String,
    val dateTaken: Long,
    val trashedAt: Long = System.currentTimeMillis()
)

@Dao
interface TrashDao {
    @Query("SELECT * FROM trash ORDER BY trashedAt DESC")
    fun observeAll(): Flow<List<TrashEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(entry: TrashEntity)

    @Query("DELETE FROM trash WHERE mediaStoreId = :id")
    suspend fun remove(id: Long)

    @Query("DELETE FROM trash")
    suspend fun clear()

    @Query("SELECT * FROM trash WHERE trashedAt < :olderThan")
    suspend fun expired(olderThan: Long): List<TrashEntity>
}

// --- Vault: per-item lock (hidden everywhere until vault is unlocked) ---

@Entity(tableName = "locked_media")
data class LockedEntity(
    @PrimaryKey val mediaStoreId: Long,
    val uriString: String,
    val lockedAt: Long = System.currentTimeMillis()
)

@Dao
interface LockedDao {
    @Query("SELECT * FROM locked_media ORDER BY lockedAt DESC")
    fun observeAll(): Flow<List<LockedEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(entry: LockedEntity)

    @Query("DELETE FROM locked_media WHERE mediaStoreId = :id")
    suspend fun remove(id: Long)
}

// --- Break-in log: failed unlock attempts (timestamps only, local) ---

@Entity(tableName = "attempts")
data class AttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Long = System.currentTimeMillis()
)

@Dao
interface AttemptDao {
    @Query("SELECT * FROM attempts ORDER BY ts DESC")
    fun observeAll(): Flow<List<AttemptEntity>>

    @Insert
    suspend fun log(entry: AttemptEntity)

    @Query("DELETE FROM attempts")
    suspend fun clear()
}

// --- Time capsule: items sealed until openAt ---

@Entity(tableName = "capsules")
data class CapsuleEntity(
    @PrimaryKey(autoGenerate = true) val capsuleId: Long = 0,
    val name: String,
    val openAt: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "capsule_items", primaryKeys = ["capsuleId", "mediaStoreId"])
data class CapsuleItemCrossRef(
    val capsuleId: Long,
    val mediaStoreId: Long,
    val uriString: String
)

@Dao
interface CapsuleDao {
    @Query("SELECT * FROM capsules ORDER BY openAt ASC")
    fun observeCapsules(): Flow<List<CapsuleEntity>>

    @Query("SELECT * FROM capsule_items")
    fun observeItems(): Flow<List<CapsuleItemCrossRef>>

    @Insert
    suspend fun create(capsule: CapsuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addItem(ref: CapsuleItemCrossRef)

    @Query("DELETE FROM capsules WHERE capsuleId = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM capsule_items WHERE capsuleId = :id")
    suspend fun clearItems(id: Long)
}

// --- Journal: one note per day ---

@Entity(tableName = "journal")
data class JournalEntity(
    @PrimaryKey val dayKey: String,
    val text: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal")
    fun observeAll(): Flow<List<JournalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: JournalEntity)
}

// --- Voice notes attached to photos ---

@Entity(tableName = "voice_notes")
data class VoiceNoteEntity(
    @PrimaryKey val mediaStoreId: Long,
    val filePath: String,
    val durationMs: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface VoiceNoteDao {
    @Query("SELECT * FROM voice_notes")
    fun observeAll(): Flow<List<VoiceNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: VoiceNoteEntity)

    @Query("DELETE FROM voice_notes WHERE mediaStoreId = :id")
    suspend fun remove(id: Long)
}

// --- Edit links: edited copy -> original (for before/after compare) ---

@Entity(tableName = "edit_links")
data class EditLinkEntity(
    @PrimaryKey val editedId: Long,
    val origId: Long
)

@Dao
interface EditLinkDao {
    @Query("SELECT * FROM edit_links")
    fun observeAll(): Flow<List<EditLinkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun link(entry: EditLinkEntity)
}

// --- OCR index: recognized text per photo ---

@Entity(tableName = "ocr_text")
data class OcrEntity(
    @PrimaryKey val mediaStoreId: Long,
    val text: String,
    val scannedAt: Long = System.currentTimeMillis()
)

@Dao
interface OcrDao {
    @Query("SELECT * FROM ocr_text")
    fun observeAll(): Flow<List<OcrEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: OcrEntity)

    @Query("DELETE FROM ocr_text WHERE mediaStoreId = :id")
    suspend fun remove(id: Long)
}

// --- Face index: detected face stats per photo ---

@Entity(tableName = "face_index")
data class FaceIndexEntity(
    @PrimaryKey val mediaStoreId: Long,
    val faceCount: Int,
    val smiling: Boolean,
    val scannedAt: Long = System.currentTimeMillis()
)

@Dao
interface FaceDao {
    @Query("SELECT * FROM face_index")
    fun observeAll(): Flow<List<FaceIndexEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: FaceIndexEntity)

    @Query("DELETE FROM face_index WHERE mediaStoreId = :id")
    suspend fun remove(id: Long)
}

// --- Date overrides: user-corrected timestamps (EXIF stays untouched) ---

@Entity(tableName = "date_overrides")
data class DateOverrideEntity(
    @PrimaryKey val mediaStoreId: Long,
    val dateTaken: Long
)

@Dao
interface DateOverrideDao {
    @Query("SELECT * FROM date_overrides")
    fun observeAll(): Flow<List<DateOverrideEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: DateOverrideEntity)

    @Query("DELETE FROM date_overrides WHERE mediaStoreId = :id")
    suspend fun remove(id: Long)
}

// --- Auto-rules: set-and-forget tidiness ---

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey val type: String,
    val enabled: Boolean,
    val daysParam: Int = 90
)

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules")
    fun observeAll(): Flow<List<RuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: RuleEntity)
}
