package com.example.data.repository

import com.example.data.db.ProjectDao
import com.example.data.entity.ProjectEntity
import com.example.engine.storage.WorkspaceManager
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.InputStream

class ProjectRepository(
    private val projectDao: ProjectDao,
    private val workspaceManager: WorkspaceManager
) {
    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    suspend fun getProjectById(id: Long): ProjectEntity? = projectDao.getProjectById(id)

    fun observeProjectById(id: Long): Flow<ProjectEntity?> = projectDao.observeProjectById(id)

    suspend fun createProject(
        name: String,
        appName: String,
        packageName: String,
        framework: String,
        templateId: String
    ): ProjectEntity {
        // Reserve an ID or generate timestamp
        val tempId = System.currentTimeMillis()
        val dir = workspaceManager.createProjectWorkspace(tempId, templateId)

        val project = ProjectEntity(
            name = name,
            directoryPath = dir.absolutePath,
            framework = framework,
            appName = appName,
            packageName = packageName,
            versionName = "1.0.0",
            versionCode = 1,
            orientation = "PORTRAIT",
            permissionsJson = "[\"android.permission.INTERNET\"]",
            backgroundMode = if (templateId == "telegram_bot") "FOREGROUND_SERVICE" else "NONE",
            creatorName = "",
            creatorUsername = "",
            showBranding = true,
            showCreator = true,
            createdAt = System.currentTimeMillis(),
            lastModified = System.currentTimeMillis()
        )

        val id = projectDao.insertProject(project)
        return project.copy(id = id)
    }

    suspend fun importSingleFile(
        name: String,
        appName: String,
        packageName: String,
        framework: String,
        fileName: String,
        content: String
    ): ProjectEntity {
        val tempId = System.currentTimeMillis()
        val dir = workspaceManager.importSinglePythonFile(tempId, fileName, content)

        val project = ProjectEntity(
            name = name,
            directoryPath = dir.absolutePath,
            framework = framework,
            appName = appName,
            packageName = packageName,
            versionName = "1.0.0",
            versionCode = 1,
            orientation = "PORTRAIT",
            permissionsJson = "[\"android.permission.INTERNET\"]",
            createdAt = System.currentTimeMillis(),
            lastModified = System.currentTimeMillis()
        )

        val id = projectDao.insertProject(project)
        return project.copy(id = id)
    }

    suspend fun importZipProject(
        name: String,
        appName: String,
        packageName: String,
        framework: String,
        zipStream: InputStream
    ): Result<ProjectEntity> {
        val tempId = System.currentTimeMillis()
        val dir = File(workspaceManager.projectsDir, "proj_$tempId")
        val extractResult = workspaceManager.extractZipSafely(zipStream, dir)
        if (extractResult.isFailure) {
            return Result.failure(extractResult.exceptionOrNull() ?: Exception("Failed to extract ZIP"))
        }

        // Ensure main.py exists or find first python file
        val mainPy = File(dir, "main.py")
        if (!mainPy.exists()) {
            val firstPy = dir.walkTopDown().firstOrNull { it.isFile && it.extension.equals("py", true) }
            if (firstPy != null) {
                firstPy.copyTo(mainPy)
            } else {
                mainPy.writeText("# ATP Python project\nprint('Imported project')\n")
            }
        }

        val project = ProjectEntity(
            name = name,
            directoryPath = dir.absolutePath,
            framework = framework,
            appName = appName,
            packageName = packageName,
            versionName = "1.0.0",
            versionCode = 1,
            orientation = "PORTRAIT",
            permissionsJson = "[\"android.permission.INTERNET\"]",
            createdAt = System.currentTimeMillis(),
            lastModified = System.currentTimeMillis()
        )

        val id = projectDao.insertProject(project)
        return Result.success(project.copy(id = id))
    }

    suspend fun updateProject(project: ProjectEntity) {
        projectDao.updateProject(project.copy(lastModified = System.currentTimeMillis()))
    }

    suspend fun deleteProject(project: ProjectEntity) {
        File(project.directoryPath).deleteRecursively()
        projectDao.deleteProject(project)
    }
}
