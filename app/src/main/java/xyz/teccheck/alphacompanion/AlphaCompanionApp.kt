package xyz.teccheck.alphacompanion

import android.app.Application
import android.companion.CompanionDeviceManager
import androidx.core.content.getSystemService
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import xyz.teccheck.alphacompanion.db.AppDatabase
import xyz.teccheck.alphacompanion.manager.CameraManager

class AlphaCompanionApp : Application() {

    val appDatabase by lazy {
        Room.databaseBuilder<AppDatabase>(applicationContext, "app-database")
            .setDriver(AndroidSQLiteDriver()).build()
    }
    val companionDeviceManager by lazy { getSystemService<CompanionDeviceManager>()!! }

    val cameraManager by lazy { CameraManager(appDatabase, companionDeviceManager) }
}

fun Application.alphaCompanion() = (this as AlphaCompanionApp)
