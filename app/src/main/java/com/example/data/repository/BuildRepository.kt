package com.example.data.repository

import android.content.Context
import com.example.data.db.BuildHistoryDao
import com.example.data.entity.BuildHistoryEntity
import com.example.data.entity.ProjectEntity
import com.example.data.model.ApkVerificationResult
import com.example.data.model.BuildMode
import com.example.data.model.OnlineBuildJob
import com.example.data.model.StepState
import com.example.engine.build.LocalApkBuilder
import com.example.engine.build.OnlineBuildClient
import com.example.engine.storage.WorkspaceManager
import kotlinx.coroutines.flow.Flow
import java.io.File

class BuildRepository(
    private val context: Context,
    private val buildHistoryDao: BuildHistoryDao,
    private val workspaceManager: WorkspaceManager,
    private val onlineBuildClient: OnlineBuildClient
) {
    val allBuilds: Flow<List<BuildHistoryEntity>> = buildHistoryDao.getAllBuilds()

    fun getBuildsForProject(projectId: Long): Flow<List<BuildHistoryEntity>> =
        buildHistoryDao.getBuildsForProject(projectId)

    suspend fun getBuildById(id: Long): BuildHistoryEntity? = buildHistoryDao.getBuildById(id)

    suspend fun runLocalBuild(
        project: ProjectEntity,
        confirmedPackages: List<String>,
        buildType: String = "DEBUG",
        onStepUpdate: (stepId: String, title: String, log: String, state: StepState) -> Unit
    ): ApkVerificationResult {
        val startTime = System.currentTimeMillis()
        val logBuffer = StringBuilder()

        val builder = LocalApkBuilder(context, project, confirmedPackages) { id, title, log, state ->
            logBuffer.append("[$title] $log\n")
            onStepUpdate(id, title, log, state)
        }

        val result = builder.build()
        val duration = System.currentTimeMillis() - startTime

        val historyEntity = BuildHistoryEntity(
            projectId = project.id,
            projectName = project.name,
            backend = project.framework,
            buildType = buildType,
            status = if (result.isValid) "SUCCESS" else "FAILED",
            apkPath = result.apkFile?.absolutePath,
            apkSize = result.apkSize,
            durationMs = duration,
            errorSummary = result.failureReason,
            fullLogs = logBuffer.toString(),
            timestamp = System.currentTimeMillis()
        )
        buildHistoryDao.insertBuild(historyEntity)

        return result
    }

    suspend fun submitOnlineBuild(
        project: ProjectEntity,
        targetAbi: String = "arm64-v8a",
        buildType: String = "debug"
    ): Result<OnlineBuildJob> {
        val projectDir = File(project.directoryPath)
        val tempZip = File(workspaceManager.buildTmpDir, "online_upload_${project.id}.zip")

        val zipRes = workspaceManager.exportProjectZip(projectDir, tempZip)
        if (zipRes.isFailure) {
            return Result.failure(zipRes.exceptionOrNull() ?: Exception("Failed to package project ZIP"))
        }

        return onlineBuildClient.submitBuild(project, tempZip, targetAbi, buildType)
    }

    suspend fun getOnlineBuildStatus(buildId: String): Result<OnlineBuildJob> =
        onlineBuildClient.getBuildStatus(buildId)

    suspend fun cancelOnlineBuild(buildId: String): Result<Boolean> =
        onlineBuildClient.cancelBuild(buildId)

    suspend fun downloadBuiltApk(downloadUrl: String, projectName: String): Result<File> {
        val target = File(workspaceManager.outputApkDir, "${projectName.replace(" ", "")}_online.apk")
        return onlineBuildClient.downloadApk(downloadUrl, target)
    }

    fun setOnlineServerUrl(url: String) {
        onlineBuildClient.setServerUrl(url)
    }

    fun getOnlineServerUrl(): String = onlineBuildClient.getServerUrl()

    suspend fun deleteBuild(id: Long) = buildHistoryDao.deleteBuild(id)
}
