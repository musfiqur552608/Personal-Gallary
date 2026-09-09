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
data class CustomAlbumEntity(
    @PrimaryKey(autoGenerate = true) val albumId: Long = 0,
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
