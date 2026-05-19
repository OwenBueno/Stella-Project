package com.stella.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stella.core.database.entity.SyncMetaEntity

@Dao
interface SyncMetaDao {
    @Query("SELECT * FROM sync_meta WHERE id = 1 LIMIT 1")
    suspend fun get(): SyncMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(meta: SyncMetaEntity)
}
