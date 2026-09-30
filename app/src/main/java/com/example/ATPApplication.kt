package com.example

import android.app.Application
import com.example.data.db.AppDatabase
import com.example.data.repository.BuildRepository
import com.example.data.repository.PackageRepository
import com.example.data.repository.ProjectRepository
import com.example.engine.build.EnvironmentDetector
import com.example.engine.build.OnlineBuildClient
import com.example.engine.pypi.PyPiClient
import com.example.engine.storage.WorkspaceManager

class ATPApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var workspaceManager: WorkspaceManager
        private set

    lateinit var projectRepository: ProjectRepository
        private set

    lateinit var buildRepository: BuildRepository
        private set

    lateinit var packageRepository: PackageRepository
        private set

    lateinit var environmentDetector: EnvironmentDetector
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        workspaceManager = WorkspaceManager(this)

        val pyPiClient = PyPiClient()
        val onlineBuildClient = OnlineBuildClient()

        projectRepository = ProjectRepository(database.projectDao(), workspaceManager)
        buildRepository = BuildRepository(this, database.buildHistoryDao(), workspaceManager, onlineBuildClient)
        packageRepository = PackageRepository(database.packageCacheDao(), database.importMappingDao(), workspaceManager, pyPiClient)
        environmentDetector = EnvironmentDetector(this)
    }

    companion object {
        lateinit var instance: ATPApplication
            private set
    }
}
