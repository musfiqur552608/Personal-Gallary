package com.freedu.personalgallary.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteEntity::class, CaptionEntity::class, CustomAlbumEntity::class,
        AlbumItemCrossRef::class, TrashEntity::class, LockedEntity::class, AttemptEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favorites(): FavoriteDao
    abstract fun captions(): CaptionDao
    abstract fun albums(): AlbumDao
    abstract fun trash(): TrashDao
    abstract fun locked(): LockedDao
    abstract fun attempts(): AttemptDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "personal_gallery.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
