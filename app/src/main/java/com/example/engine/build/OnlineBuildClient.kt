package com.example.engine.build

import com.example.data.entity.ProjectEntity
import com.example.data.model.OnlineBuildJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class OnlineBuildClient(
    private var baseUrl: String = "https://build.atppython.org"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun setServerUrl(url: String) {
        baseUrl = url.trim().removeSuffix("/")
    }

    fun getServerUrl(): String = baseUrl

    /**
     * Submits a project ZIP to the remote build server.
     */
    suspend fun submitBuild(
        project: ProjectEntity,
        projectZip: File,
        targetAbi: String = "arm64-v8a",
        buildType: String = "debug"
    ): Result<OnlineBuildJob> = withContext(Dispatchers.IO) {
        val endpoint = "$baseUrl/api/build"

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("project_name", project.name)
            .addFormDataPart("app_name", project.appName)
            .addFormDataPart("package_name", project.packageName)
            .addFormDataPart("version_name", project.versionName)
            .addFormDataPart("version_code", project.versionCode.toString())
            .addFormDataPart("framework", project.framework)
            .addFormDataPart("target_abi", targetAbi)
            .addFormDataPart("build_type", buildType)
            .addFormDataPart(
                "project_archive",
                projectZip.name,
                projectZip.asRequestBody("application/zip".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .header("User-Agent", "ATPPython-Client/1.0")
            .build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Build server returned HTTP ${response.code}: ${extractError(body)}")
                )
            }

            val json = JSONObject(body)
            val buildId = json.optString("build_id", json.optString("id", "job_${System.currentTimeMillis()}"))
            val status = json.optString("status", "QUEUED")
            val queuePos = if (json.has("queue_position")) json.getInt("queue_position") else 1

            Result.success(
                OnlineBuildJob(
                    buildId = buildId,
                    serverUrl = baseUrl,
                    status = status,
                    queuePosition = queuePos,
                    progressPercent = 10,
                    logs = listOf("[${System.currentTimeMillis()}] Project uploaded. Job queued at position #$queuePos")
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Polls status of remote build.
     */
    suspend fun getBuildStatus(buildId: String): Result<OnlineBuildJob> = withContext(Dispatchers.IO) {
        val endpoint = "$baseUrl/api/build/$buildId"
        val request = Request.Builder().url(endpoint).build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: $body"))
            }

            val json = JSONObject(body)
            val status = json.optString("status", "BUILDING")
            val progress = json.optInt("progress", 50)
            val queuePos = if (json.has("queue_position")) json.optInt("queue_position") else null
            val downloadUrl = json.optString("download_url", null)
            val error = json.optString("error", null)

            val logsList = mutableListOf<String>()
            val logArr = json.optJSONArray("logs")
            if (logArr != null) {
                for (i in 0 until logArr.length()) {
                    logsList.add(logArr.getString(i))
                }
            }

            Result.success(
                OnlineBuildJob(
                    buildId = buildId,
                    serverUrl = baseUrl,
                    status = status,
                    queuePosition = queuePos,
                    progressPercent = progress,
                    logs = logsList,
                    downloadUrl = downloadUrl,
                    error = error
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cancels remote build job.
     */
    suspend fun cancelBuild(buildId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val endpoint = "$baseUrl/api/build/$buildId/cancel"
        val request = Request.Builder()
            .url(endpoint)
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), "{}"))
            .build()

        try {
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads finished APK from build server.
     */
    suspend fun downloadApk(downloadUrl: String, destinationFile: File): Result<File> = withContext(Dispatchers.IO) {
        val fullUrl = if (downloadUrl.startsWith("http")) downloadUrl else "$baseUrl$downloadUrl"
        val request = Request.Builder().url(fullUrl).build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to download APK (HTTP ${response.code})"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty download body"))
            destinationFile.parentFile?.mkdirs()
            val fos = FileOutputStream(destinationFile)
            body.byteStream().use { input ->
                fos.use { output ->
                    input.copyTo(output)
                }
            }
            Result.success(destinationFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractError(body: String): String {
        return try {
            val j = JSONObject(body)
            j.optString("error", j.optString("message", body))
        } catch (e: Exception) {
            body.take(150)
        }
    }
}
