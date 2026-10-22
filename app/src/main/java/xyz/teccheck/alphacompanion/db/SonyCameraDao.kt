package xyz.teccheck.alphacompanion.db

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SonyCameraDao {
    @Query("SELECT * FROM cameras")
    fun getAll(): Flow<List<SonyCamera>>

    @Insert
    suspend fun insertAll(vararg cameras: SonyCamera)

    @Delete
    suspend fun delete(camera: SonyCamera)

    @Query("DELETE FROM cameras WHERE mac_address NOT IN (:addresses)")
    suspend fun retainByAddress(addresses: List<String>)

    @Query("DELETE FROM cameras WHERE association_id NOT IN (:ids)")
    suspend fun retainById(ids: List<Int>)
}
