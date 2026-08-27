package com.example.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import com.example.data.model.ChannelEntity
import com.example.data.model.ConfigEntity
import com.example.data.model.ProtocolType
import com.example.data.model.ProxyEntity
import com.example.data.model.ProxyType
import kotlinx.coroutines.flow.Flow

class Converters {
    @TypeConverter
    fun fromProtocolType(value: ProtocolType): String = value.name

    @TypeConverter
    fun toProtocolType(value: String): ProtocolType = runCatching {
        ProtocolType.valueOf(value)
    }.getOrDefault(ProtocolType.OTHER)

    @TypeConverter
    fun fromProxyType(value: ProxyType): String = value.name

    @TypeConverter
    fun toProxyType(value: String): ProxyType = runCatching {
        ProxyType.valueOf(value)
    }.getOrDefault(ProxyType.MTPROTO)
}

@Dao
interface ConfigDao {
    @Query("SELECT * FROM configs ORDER BY isFavorite DESC, CASE WHEN isAlive = 1 THEN 0 WHEN isAlive IS NULL THEN 1 ELSE 2 END, pingMs ASC, id DESC")
    fun getAllConfigs(): Flow<List<ConfigEntity>>

    @Query("SELECT * FROM configs WHERE isAlive = 1 ORDER BY isFavorite DESC, pingMs ASC")
    fun getAliveConfigs(): Flow<List<ConfigEntity>>

    @Query("SELECT * FROM configs WHERE id = :id")
    suspend fun getConfigById(id: Long): ConfigEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertConfigs(configs: List<ConfigEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConfig(config: ConfigEntity): Long

    @Update
    suspend fun updateConfig(config: ConfigEntity)

    @Query("UPDATE configs SET pingMs = :pingMs, isAlive = :isAlive, lastTestedAt = :timestamp WHERE id = :id")
    suspend fun updatePing(id: Long, pingMs: Long, isAlive: Boolean, timestamp: Long)

    @Query("DELETE FROM configs WHERE isAlive = 0 OR pingMs = -2")
    suspend fun deleteDeadConfigs(): Int

    @Query("DELETE FROM configs WHERE id = :id")
    suspend fun deleteConfigById(id: Long)

    @Query("DELETE FROM configs")
    suspend fun deleteAllConfigs()

    @Query("UPDATE configs SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long)

    @Query("SELECT COUNT(*) FROM configs")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM configs WHERE isAlive = 1")
    fun getAliveCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM configs WHERE isAlive = 0 OR pingMs = -2")
    fun getDeadCount(): Flow<Int>
}

@Dao
interface ProxyDao {
    @Query("SELECT * FROM proxies ORDER BY isFavorite DESC, CASE WHEN isAlive = 1 THEN 0 WHEN isAlive IS NULL THEN 1 ELSE 2 END, pingMs ASC, id DESC")
    fun getAllProxies(): Flow<List<ProxyEntity>>

    @Query("SELECT * FROM proxies WHERE isAlive = 1 ORDER BY isFavorite DESC, pingMs ASC")
    fun getAliveProxies(): Flow<List<ProxyEntity>>

    @Query("SELECT * FROM proxies WHERE id = :id")
    suspend fun getProxyById(id: Long): ProxyEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProxies(proxies: List<ProxyEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProxy(proxy: ProxyEntity): Long

    @Update
    suspend fun updateProxy(proxy: ProxyEntity)

    @Query("UPDATE proxies SET pingMs = :pingMs, isAlive = :isAlive, lastTestedAt = :timestamp WHERE id = :id")
    suspend fun updatePing(id: Long, pingMs: Long, isAlive: Boolean, timestamp: Long)

    @Query("DELETE FROM proxies WHERE isAlive = 0 OR pingMs = -2")
    suspend fun deleteDeadProxies(): Int

    @Query("DELETE FROM proxies WHERE id = :id")
    suspend fun deleteProxyById(id: Long)

    @Query("DELETE FROM proxies")
    suspend fun deleteAllProxies()

    @Query("UPDATE proxies SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long)

    @Query("SELECT COUNT(*) FROM proxies")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM proxies WHERE isAlive = 1")
    fun getAliveCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM proxies WHERE isAlive = 0 OR pingMs = -2")
    fun getDeadCount(): Flow<Int>
}

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels ORDER BY id ASC")
    fun getAllChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE isEnabled = 1")
    suspend fun getEnabledChannels(): List<ChannelEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelEntity): Long

    @Update
    suspend fun updateChannel(channel: ChannelEntity)

    @Query("UPDATE channels SET lastFetchTime = :time, fetchedConfigCount = :configs, fetchedProxyCount = :proxies WHERE id = :id")
    suspend fun updateFetchStats(id: Long, time: Long, configs: Int, proxies: Int)

    @Query("DELETE FROM channels WHERE id = :id")
    suspend fun deleteChannelById(id: Long)

    @Query("UPDATE channels SET isEnabled = NOT isEnabled WHERE id = :id")
    suspend fun toggleChannel(id: Long)
}

@Database(
    entities = [ConfigEntity::class, ProxyEntity::class, ChannelEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun configDao(): ConfigDao
    abstract fun proxyDao(): ProxyDao
    abstract fun channelDao(): ChannelDao
}
