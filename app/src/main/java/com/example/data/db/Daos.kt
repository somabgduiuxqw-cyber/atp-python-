package com.example.data.db

import androidx.room.*
import com.example.data.entity.BuildHistoryEntity
import com.example.data.entity.ImportMappingEntity
import com.example.data.entity.PackageCacheEntity
import com.example.data.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY lastModified DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    fun observeProjectById(id: Long): Flow<ProjectEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)
}

@Dao
interface BuildHistoryDao {
    @Query("SELECT * FROM build_history ORDER BY timestamp DESC")
    fun getAllBuilds(): Flow<List<BuildHistoryEntity>>

    @Query("SELECT * FROM build_history WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getBuildsForProject(projectId: Long): Flow<List<BuildHistoryEntity>>

    @Query("SELECT * FROM build_history WHERE id = :id LIMIT 1")
    suspend fun getBuildById(id: Long): BuildHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuild(build: BuildHistoryEntity): Long

    @Update
    suspend fun updateBuild(build: BuildHistoryEntity)

    @Query("DELETE FROM build_history WHERE id = :id")
    suspend fun deleteBuild(id: Long)
}

@Dao
interface PackageCacheDao {
    @Query("SELECT * FROM package_cache ORDER BY cachedAt DESC")
    fun getAllCachedPackages(): Flow<List<PackageCacheEntity>>

    @Query("SELECT * FROM package_cache WHERE packageName = :name LIMIT 1")
    suspend fun getCachedPackage(name: String): PackageCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackage(pkg: PackageCacheEntity)

    @Delete
    suspend fun deletePackage(pkg: PackageCacheEntity)

    @Query("DELETE FROM package_cache")
    suspend fun clearCache()

    @Query("SELECT SUM(sizeBytes) FROM package_cache")
    fun getTotalCacheSizeBytes(): Flow<Long?>
}

@Dao
interface ImportMappingDao {
    @Query("SELECT * FROM import_mappings")
    fun getAllMappings(): Flow<List<ImportMappingEntity>>

    @Query("SELECT * FROM import_mappings WHERE importName = :name LIMIT 1")
    suspend fun getMapping(name: String): ImportMappingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapping(mapping: ImportMappingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMappings(mappings: List<ImportMappingEntity>)

    @Update
    suspend fun updateMapping(mapping: ImportMappingEntity)
}
